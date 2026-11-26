package com.aistudio.audit

import com.android.aapt.Resources
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object BinaryManifestParser {
  private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
  private const val XMLNS_NS = "http://www.w3.org/2000/xmlns/"

  fun decodeProtoXmlToDom(protoBytes: ByteArray): Document {
    val rootNode = Resources.XmlNode.parseFrom(protoBytes)
    val factory = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }
    val doc = factory.newDocumentBuilder().newDocument()
    if (rootNode.hasElement()) {
      val rootEl = buildDomElement(doc, rootNode.element, isRoot = true)
      doc.appendChild(rootEl)
    }
    return doc
  }

  private fun buildDomElement(doc: Document, protoEl: Resources.XmlElement, isRoot: Boolean): Element {
    val el = if (protoEl.namespaceUri.isNotEmpty()) {
      doc.createElementNS(protoEl.namespaceUri, protoEl.name)
    } else {
      doc.createElement(protoEl.name)
    }

    if (isRoot) {
      el.setAttributeNS(XMLNS_NS, "xmlns:android", ANDROID_NS)
    }
    for (nsDecl in protoEl.namespaceDeclarationList) {
      if (nsDecl.prefix.isNotEmpty() && nsDecl.uri.isNotEmpty()) {
        el.setAttributeNS(XMLNS_NS, "xmlns:${nsDecl.prefix}", nsDecl.uri)
      }
    }

    for (attr in protoEl.attributeList) {
      val value = resolveAttributeValue(attr)
      if (attr.namespaceUri == ANDROID_NS) {
        el.setAttributeNS(ANDROID_NS, "android:${attr.name}", value)
      } else if (attr.namespaceUri.isNotEmpty()) {
        el.setAttributeNS(attr.namespaceUri, attr.name, value)
      } else {
        el.setAttribute(attr.name, value)
      }
    }

    for (child in protoEl.childList) {
      when {
        child.hasElement() -> el.appendChild(buildDomElement(doc, child.element, isRoot = false))
        child.hasText() && child.text.isNotEmpty() -> el.appendChild(doc.createTextNode(child.text))
      }
    }
    return el
  }

  private fun resolveAttributeValue(attr: Resources.XmlAttribute): String {
    if (attr.value.isNotEmpty()) return attr.value
    if (!attr.hasCompiledItem()) return ""
    val item = attr.compiledItem
    return when {
      item.hasStr() -> item.str.value
      item.hasRawStr() -> item.rawStr.value
      item.hasRef() -> {
        val ref = item.ref
        if (ref.name.isNotEmpty()) "@${ref.name}" else "@0x${Integer.toHexString(ref.id)}"
      }
      item.hasPrim() -> {
        val p = item.prim
        when {
          p.hasBooleanValue() -> p.booleanValue.toString()
          p.hasIntDecimalValue() -> p.intDecimalValue.toString()
          p.hasIntHexadecimalValue() -> "0x${Integer.toHexString(p.intHexadecimalValue)}"
          p.hasFloatValue() -> p.floatValue.toString()
          else -> ""
        }
      }
      else -> ""
    }
  }

  fun renderDocumentXml(doc: Document): String {
    val transformer = TransformerFactory.newInstance().newTransformer().apply {
      setOutputProperty(OutputKeys.INDENT, "yes")
      setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
      setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    }
    val sw = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(sw))
    return sw.toString()
  }

  fun parseManifest(
    manifestProtoBytes: ByteArray,
    packagedXmlFiles: Map<String, ByteArray>
  ): ManifestAuditResult {
    val doc = decodeProtoXmlToDom(manifestProtoBytes)
    val root = doc.documentElement
      ?: throw IllegalArgumentException("Decoded AndroidManifest.xml has no root element")

    val packageName = root.getAttribute("package")
    val versionCode = getAndroidAttr(root, "versionCode").orEmpty()
    val versionName = getAndroidAttr(root, "versionName").orEmpty()

    val permissions = directChildrenByTag(root, "uses-permission")
      .mapNotNull { getAndroidAttr(it, "name") }
      .distinct()
      .sorted()

    val appEl = directChildrenByTag(root, "application").firstOrNull()
      ?: throw IllegalArgumentException("Decoded AndroidManifest.xml missing <application>")

    val debuggable = getAndroidAttr(appEl, "debuggable")?.equals("true", ignoreCase = true) == true
    val allowBackup = getAndroidAttr(appEl, "allowBackup")?.let { it.equals("true", ignoreCase = true) }
    val usesCleartextTraffic = getAndroidAttr(appEl, "usesCleartextTraffic")?.let { it.equals("true", ignoreCase = true) }

    val firebaseDataCollection = directChildrenByTag(appEl, "meta-data")
      .firstOrNull { getAndroidAttr(it, "name") == "firebase_data_collection_default_enabled" }
      ?.let { getAndroidAttr(it, "value") }

    val activities = extractComponents(appEl, "activity", packageName)
    val services = extractComponents(appEl, "service", packageName)
    val receivers = extractComponents(appEl, "receiver", packageName)
    val providers = extractComponents(appEl, "provider", packageName)

    val xmlAudit = auditPackagedSecurityXmls(packagedXmlFiles)

    return ManifestAuditResult(
      packageName = packageName,
      versionCode = versionCode,
      versionName = versionName,
      debuggable = debuggable,
      allowBackup = allowBackup,
      usesCleartextTraffic = usesCleartextTraffic,
      firebaseDataCollectionDefaultEnabled = firebaseDataCollection,
      permissions = permissions,
      activities = activities,
      services = services,
      receivers = receivers,
      providers = providers,
      packagedXmlAudit = xmlAudit,
      decodedXml = renderDocumentXml(doc)
    )
  }

  private fun extractComponents(appEl: Element, tagName: String, packageName: String): List<ManifestComponent> {
    return directChildrenByTag(appEl, tagName).mapNotNull { el ->
      val rawName = getAndroidAttr(el, "name") ?: return@mapNotNull null
      val resolvedName = if (rawName.startsWith(".")) "$packageName$rawName" else rawName
      val exportedAttr = getAndroidAttr(el, "exported")?.let { it.equals("true", ignoreCase = true) }
      val permission = getAndroidAttr(el, "permission")?.takeIf { it.isNotBlank() }
      val authorities = if (tagName == "provider") getAndroidAttr(el, "authorities") else null
      val intentFilters = directChildrenByTag(el, "intent-filter")
      val hasIntentFilter = intentFilters.isNotEmpty()
      val isMainLauncher = intentFilters.any { filter ->
        val hasMain = directChildrenByTag(filter, "action")
          .any { getAndroidAttr(it, "name") == "android.intent.action.MAIN" }
        val hasLauncher = directChildrenByTag(filter, "category")
          .any { getAndroidAttr(it, "name") == "android.intent.category.LAUNCHER" }
        hasMain && hasLauncher
      }
      val effectiveExported = exportedAttr ?: hasIntentFilter
      ManifestComponent(
        type = tagName,
        name = resolvedName,
        exportedAttr = exportedAttr,
        effectiveExported = effectiveExported,
        permission = permission,
        hasIntentFilter = hasIntentFilter,
        isMainLauncher = isMainLauncher,
        authorities = authorities
      )
    }.sortedBy { it.name }
  }

  private fun auditPackagedSecurityXmls(packagedXmlFiles: Map<String, ByteArray>): PackagedXmlAuditResult {
    val violations = mutableListOf<AuditViolation>()
    val providerPathEntries = mutableListOf<String>()
    val backupExcludeDomains = mutableListOf<String>()
    val cloudBackupExcludeDomains = mutableListOf<String>()
    val deviceTransferExcludeDomains = mutableListOf<String>()
    var cleartextPermitted: Boolean? = null
    var trustsUserCerts = false

    val requiredDomains = setOf("root", "file", "database", "sharedpref", "external")

    // 1. provider_paths.xml
    val providerBytes = packagedXmlFiles["provider_paths.xml"]
    if (providerBytes == null) {
      violations.add(
        AuditViolation(
          category = "MANIFEST_XML",
          severity = "ERROR",
          message = "Packaged res/xml/provider_paths.xml is missing from bundle"
        )
      )
    } else {
      val doc = decodeProtoXmlToDom(providerBytes)
      val root = doc.documentElement
      val children = directElementChildren(root)
      if (children.isEmpty()) {
        violations.add(
          AuditViolation(
            category = "MANIFEST_XML",
            severity = "ERROR",
            message = "Packaged provider_paths.xml defines no path elements"
          )
        )
      }
      for (el in children) {
        val tag = el.localName ?: el.tagName
        val path = el.getAttribute("path")
        val name = el.getAttribute("name")
        providerPathEntries.add("<$tag name=\"$name\" path=\"$path\"/>")
        if (tag != "cache-path" || path != "docs/") {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "FileProvider exposes overly broad path in packaged provider_paths.xml",
              evidence = "<$tag name=\"$name\" path=\"$path\"/> (expected only <cache-path path=\"docs/\"/>)"
            )
          )
        }
      }
    }

    // 2. backup_rules.xml
    val backupBytes = packagedXmlFiles["backup_rules.xml"]
    if (backupBytes == null) {
      violations.add(
        AuditViolation(
          category = "MANIFEST_XML",
          severity = "ERROR",
          message = "Packaged res/xml/backup_rules.xml is missing from bundle"
        )
      )
    } else {
      val doc = decodeProtoXmlToDom(backupBytes)
      val root = doc.documentElement
      for (el in directElementChildren(root)) {
        val tag = el.localName ?: el.tagName
        val domain = el.getAttribute("domain")
        val path = el.getAttribute("path")
        if (tag == "include") {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged backup_rules.xml contains forbidden <include>",
              evidence = "domain=$domain path=$path"
            )
          )
        } else if (tag == "exclude" && (path == "." || path.isEmpty())) {
          backupExcludeDomains.add(domain)
        }
      }
      val missing = requiredDomains - backupExcludeDomains.toSet()
      if (missing.isNotEmpty()) {
        violations.add(
          AuditViolation(
            category = "MANIFEST_XML",
            severity = "ERROR",
            message = "Packaged backup_rules.xml does not exclude all sensitive domains",
            evidence = "Missing exclusions: ${missing.sorted()}"
          )
        )
      }
    }

    // 3. data_extraction_rules.xml
    val extractionBytes = packagedXmlFiles["data_extraction_rules.xml"]
    if (extractionBytes == null) {
      violations.add(
        AuditViolation(
          category = "MANIFEST_XML",
          severity = "ERROR",
          message = "Packaged res/xml/data_extraction_rules.xml is missing from bundle"
        )
      )
    } else {
      val doc = decodeProtoXmlToDom(extractionBytes)
      val root = doc.documentElement
      fun inspectSection(sectionName: String, target: MutableList<String>) {
        val section = directChildrenByTag(root, sectionName).firstOrNull()
        if (section == null) {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged data_extraction_rules.xml missing <$sectionName>"
            )
          )
          return
        }
        for (el in directElementChildren(section)) {
          val tag = el.localName ?: el.tagName
          val domain = el.getAttribute("domain")
          val path = el.getAttribute("path")
          if (tag == "include") {
            violations.add(
              AuditViolation(
                category = "MANIFEST_XML",
                severity = "ERROR",
                message = "Packaged data_extraction_rules.xml <$sectionName> contains forbidden <include>",
                evidence = "domain=$domain path=$path"
              )
            )
          } else if (tag == "exclude" && (path == "." || path.isEmpty())) {
            target.add(domain)
          }
        }
        val missing = requiredDomains - target.toSet()
        if (missing.isNotEmpty()) {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged data_extraction_rules.xml <$sectionName> missing domain exclusions",
              evidence = "Missing: ${missing.sorted()}"
            )
          )
        }
      }
      inspectSection("cloud-backup", cloudBackupExcludeDomains)
      inspectSection("device-transfer", deviceTransferExcludeDomains)
    }

    // 4. network_security_config.xml
    val netSecBytes = packagedXmlFiles["network_security_config.xml"]
    if (netSecBytes == null) {
      violations.add(
        AuditViolation(
          category = "MANIFEST_XML",
          severity = "ERROR",
          message = "Packaged res/xml/network_security_config.xml is missing from bundle"
        )
      )
    } else {
      val doc = decodeProtoXmlToDom(netSecBytes)
      val root = doc.documentElement
      val baseConfig = directChildrenByTag(root, "base-config").firstOrNull()
      if (baseConfig == null) {
        violations.add(
          AuditViolation(
            category = "MANIFEST_XML",
            severity = "ERROR",
            message = "Packaged network_security_config.xml missing <base-config>"
          )
        )
      } else {
        cleartextPermitted = baseConfig.getAttribute("cleartextTrafficPermitted")
          .takeIf { it.isNotEmpty() }
          ?.equals("true", ignoreCase = true)
        if (cleartextPermitted != false) {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged network_security_config.xml does not enforce cleartextTrafficPermitted=\"false\"",
              evidence = "cleartextTrafficPermitted=$cleartextPermitted"
            )
          )
        }
      }
      // Check all <certificates> elements recursively for user certs
      val certNodes = doc.getElementsByTagName("certificates")
      for (i in 0 until certNodes.length) {
        val el = certNodes.item(i) as? Element ?: continue
        if (el.getAttribute("src").equals("user", ignoreCase = true)) {
          trustsUserCerts = true
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged network_security_config.xml trusts user-installed CA certificates in release",
              evidence = "<certificates src=\"user\"/>"
            )
          )
        }
      }
      // Check domain-config elements for cleartextTrafficPermitted="true"
      val domainConfigs = doc.getElementsByTagName("domain-config")
      for (i in 0 until domainConfigs.length) {
        val el = domainConfigs.item(i) as? Element ?: continue
        if (el.getAttribute("cleartextTrafficPermitted").equals("true", ignoreCase = true)) {
          violations.add(
            AuditViolation(
              category = "MANIFEST_XML",
              severity = "ERROR",
              message = "Packaged network_security_config.xml permits cleartext traffic in <domain-config>"
            )
          )
        }
      }
    }

    return PackagedXmlAuditResult(
      providerPathsEntries = providerPathEntries.sorted(),
      backupExcludeDomains = backupExcludeDomains.sorted(),
      cloudBackupExcludeDomains = cloudBackupExcludeDomains.sorted(),
      deviceTransferExcludeDomains = deviceTransferExcludeDomains.sorted(),
      cleartextTrafficPermitted = cleartextPermitted,
      trustsUserCertificates = trustsUserCerts,
      xmlIntegrityViolations = violations
    )
  }

  private fun getAndroidAttr(el: Element, localName: String): String? {
    val nsVal = el.getAttributeNS(ANDROID_NS, localName)
    if (nsVal.isNotEmpty()) return nsVal
    val prefixed = el.getAttribute("android:$localName")
    if (prefixed.isNotEmpty()) return prefixed
    val bare = el.getAttribute(localName)
    return bare.takeIf { it.isNotEmpty() }
  }

  private fun directChildrenByTag(parent: Element, tagName: String): List<Element> {
    return directElementChildren(parent).filter { (it.localName ?: it.tagName) == tagName }
  }

  private fun directElementChildren(parent: Element): List<Element> {
    val result = mutableListOf<Element>()
    val children = parent.childNodes
    for (i in 0 until children.length) {
      val node = children.item(i)
      if (node is Element) result.add(node)
    }
    return result
  }
}
