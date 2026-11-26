package com.aistudio.audit

import com.google.gson.GsonBuilder
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

object ReleaseArtifactVerifier {
  private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

  fun loadBaseline(file: File): ReleaseArtifactBaseline {
    return gson.fromJson(file.readText(Charsets.UTF_8), ReleaseArtifactBaseline::class.java)
  }

  fun verify(
    archiveFile: File,
    r8ConfigurationFile: File,
    r8SeedsFile: File,
    r8UsageFile: File,
    r8MappingFile: File,
    baseline: ReleaseArtifactBaseline
  ): ReleaseArtifactAuditReport {
    val violations = mutableListOf<AuditViolation>()
    var manifestProtoBytes: ByteArray? = null
    var resourcesPbBytes: ByteArray? = null
    val packagedXmlFiles = linkedMapOf<String, ByteArray>()
    val dexEntries = linkedMapOf<String, ByteArray>()
    val elfAudits = mutableListOf<ElfBinaryAudit>()
    val assetEntries = mutableListOf<String>()
    val metaInfServiceEntries = mutableListOf<String>()

    val artifactType = when {
      archiveFile.name.endsWith(".aab", ignoreCase = true) -> "AAB"
      archiveFile.name.endsWith(".apk", ignoreCase = true) -> "APK"
      else -> "ARCHIVE"
    }

    ZipFile(archiveFile).use { zip ->
      val entries = zip.entries()
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (entry.isDirectory) continue
        val name = entry.name

        when {
          name == "base/manifest/AndroidManifest.xml" -> {
            manifestProtoBytes = zip.getInputStream(entry).readBytes()
          }
          name == "base/resources.pb" -> {
            resourcesPbBytes = zip.getInputStream(entry).readBytes()
          }
          name.startsWith("base/res/xml/") && name.endsWith(".xml") -> {
            val fileName = name.substringAfterLast("/")
            packagedXmlFiles[fileName] = zip.getInputStream(entry).readBytes()
          }
          (name.startsWith("base/dex/") || !name.contains("/")) && name.endsWith(".dex") -> {
            dexEntries[name] = zip.getInputStream(entry).readBytes()
          }
          (name.startsWith("base/lib/") || name.startsWith("lib/")) && name.endsWith(".so") -> {
            val bytes = zip.getInputStream(entry).readBytes()
            elfAudits.add(ElfAlignmentChecker.inspectElf(name, bytes))
          }
          name.startsWith("base/assets/") || name.startsWith("assets/") -> {
            assetEntries.add(name)
          }
          name.contains("META-INF/services/") -> {
            metaInfServiceEntries.add(name)
          }
        }
      }
    }

    val rawManifestBytes = manifestProtoBytes
      ?: throw IllegalStateException("Binary protobuf base/manifest/AndroidManifest.xml not found inside ${archiveFile.name}")
    val rawResourcesBytes = resourcesPbBytes
      ?: throw IllegalStateException("Binary protobuf base/resources.pb not found inside ${archiveFile.name}")
    if (dexEntries.isEmpty()) {
      throw IllegalStateException("No classes*.dex entries found inside ${archiveFile.name}")
    }

    // 1. Binary Manifest + Packaged Security XML Audit (DOM-based, zero regex)
    val manifestAudit = BinaryManifestParser.parseManifest(rawManifestBytes, packagedXmlFiles)
    violations.addAll(manifestAudit.packagedXmlAudit.xmlIntegrityViolations)

    if (baseline.expectedPackage.isNotEmpty() && manifestAudit.packageName != baseline.expectedPackage) {
      violations.add(
        AuditViolation(
          category = "MANIFEST",
          severity = "ERROR",
          message = "Package name mismatch",
          evidence = "Expected ${baseline.expectedPackage}, got ${manifestAudit.packageName}"
        )
      )
    }
    if (manifestAudit.debuggable) {
      violations.add(
        AuditViolation("MANIFEST", "ERROR", "Binary release manifest has android:debuggable=\"true\"")
      )
    }
    if (manifestAudit.allowBackup != false) {
      violations.add(
        AuditViolation(
          "MANIFEST",
          "ERROR",
          "Binary release manifest does not enforce android:allowBackup=\"false\"",
          "allowBackup=${manifestAudit.allowBackup}"
        )
      )
    }
    if (manifestAudit.usesCleartextTraffic == true) {
      violations.add(
        AuditViolation("MANIFEST", "ERROR", "Binary release manifest enables android:usesCleartextTraffic=\"true\"")
      )
    }
    if (manifestAudit.firebaseDataCollectionDefaultEnabled != "false") {
      violations.add(
        AuditViolation(
          "MANIFEST",
          "ERROR",
          "Binary release manifest missing firebase_data_collection_default_enabled=\"false\"",
          "actual=${manifestAudit.firebaseDataCollectionDefaultEnabled}"
        )
      )
    }

    // Allowlist checks on permissions & all component types
    fun checkAllowlist(category: String, label: String, actual: List<String>, allowed: List<String>) {
      val allowedSet = allowed.toSet()
      val unexpected = actual.filter { it !in allowedSet }
      for (item in unexpected) {
        violations.add(
          AuditViolation(
            category = category,
            severity = "ERROR",
            message = "Un-allowlisted $label found in binary release manifest",
            evidence = item
          )
        )
      }
    }

    checkAllowlist("MANIFEST_ALLOWLIST", "uses-permission", manifestAudit.permissions, baseline.allowedPermissions)
    checkAllowlist("MANIFEST_ALLOWLIST", "activity", manifestAudit.activities.map { it.name }, baseline.allowedActivities)
    checkAllowlist("MANIFEST_ALLOWLIST", "service", manifestAudit.services.map { it.name }, baseline.allowedServices)
    checkAllowlist("MANIFEST_ALLOWLIST", "receiver", manifestAudit.receivers.map { it.name }, baseline.allowedReceivers)
    checkAllowlist("MANIFEST_ALLOWLIST", "provider", manifestAudit.providers.map { it.name }, baseline.allowedProviders)

    // Exported component guard: exported=true without android:permission forbidden except MAIN/LAUNCHER or explicit allowlist
    val allowedExportedNoPerm = baseline.allowedExportedWithoutPermission.toSet()
    val allComponents = manifestAudit.activities + manifestAudit.services + manifestAudit.receivers + manifestAudit.providers
    for (comp in allComponents) {
      if (comp.effectiveExported && !comp.isMainLauncher && comp.permission.isNullOrBlank()) {
        if (comp.name !in allowedExportedNoPerm) {
          violations.add(
            AuditViolation(
              category = "MANIFEST_EXPORTED",
              severity = "ERROR",
              message = "Exported ${comp.type} lacks android:permission and is not allowlisted",
              evidence = "${comp.name} (exportedAttr=${comp.exportedAttr}, hasIntentFilter=${comp.hasIntentFilter})"
            )
          )
        }
      }
    }

    // Verify FileProvider exists and is not exported
    val fileProvider = manifestAudit.providers.firstOrNull { it.name == "androidx.core.content.FileProvider" }
    if (fileProvider == null) {
      violations.add(
        AuditViolation("MANIFEST", "ERROR", "Expected androidx.core.content.FileProvider missing from binary manifest")
      )
    } else if (fileProvider.effectiveExported) {
      violations.add(
        AuditViolation("MANIFEST_EXPORTED", "ERROR", "FileProvider must not be exported", fileProvider.name)
      )
    }

    // 2. Structural Binary DEX Audit
    val dexAudit = DexBinaryParser.auditDexFiles(dexEntries, baseline)
    for (hit in dexAudit.forbiddenMethodIdHits) {
      violations.add(
        AuditViolation("DEX_BYTECODE", "ERROR", "Forbidden method referenced in DEX method_ids table", hit)
      )
    }
    for (invoke in dexAudit.forbiddenInvokeHits) {
      violations.add(
        AuditViolation(
          "DEX_BYTECODE",
          "ERROR",
          "Forbidden method invoked in Dalvik bytecode instruction",
          "${invoke.callerClass} calls ${invoke.targetMethod}"
        )
      )
    }
    for (typeHit in dexAudit.forbiddenTypeHits) {
      violations.add(
        AuditViolation("DEX_BYTECODE", "ERROR", "Forbidden SDK/debug class present in DEX type_ids", typeHit)
      )
    }
    for (strHit in dexAudit.forbiddenStringHits) {
      violations.add(
        AuditViolation("DEX_STRINGS", "ERROR", "Forbidden token found in DEX string_ids pool", strHit)
      )
    }
    for (urlHit in dexAudit.cleartextUrlHits) {
      violations.add(
        AuditViolation("DEX_STRINGS", "ERROR", "Un-allowlisted cleartext http:// URL found in DEX string_ids", urlHit)
      )
    }

    // 3. Packaged Protobuf ResourceTable Audit (base/resources.pb)
    val resAudit = ResourceTableParser.auditResourceTable(rawResourcesBytes, baseline)
    for (hit in resAudit.forbiddenResourceStringHits) {
      violations.add(
        AuditViolation("RESOURCES_PB", "ERROR", "Forbidden token found in packaged resources.pb", hit)
      )
    }
    for (urlHit in resAudit.cleartextResourceUrlHits) {
      violations.add(
        AuditViolation("RESOURCES_PB", "ERROR", "Un-allowlisted cleartext http:// URL in packaged resources.pb", urlHit)
      )
    }

    // 4. All 4 R8 Outputs Audit (configuration.txt, seeds.txt, usage.txt, mapping.txt)
    val r8Audit = R8AuditParser.auditR8Outputs(
      configurationFile = r8ConfigurationFile,
      seedsFile = r8SeedsFile,
      usageFile = r8UsageFile,
      mappingFile = r8MappingFile,
      baseline = baseline
    )
    for (broad in r8Audit.broadRuleFindings) {
      violations.add(
        AuditViolation("R8_KEEP_RULES", "ERROR", "Over-broad or unsafe R8 directive detected", broad)
      )
    }
    if (r8Audit.unobfuscatedAppClasses.size > baseline.maxUnobfuscatedAppClasses) {
      violations.add(
        AuditViolation(
          "R8_OBFUSCATION",
          "ERROR",
          "Un-obfuscated app class count exceeds baseline budget",
          "actual=${r8Audit.unobfuscatedAppClasses.size}, max=${baseline.maxUnobfuscatedAppClasses}"
        )
      )
    }
    if (r8Audit.usageStrippedClasses == 0) {
      violations.add(
        AuditViolation("R8_SHRINKING", "ERROR", "R8 usage.txt reports 0 stripped classes (shrinking may be inactive)")
      )
    }

    // 5. Native ELF (.so) + Assets + META-INF/services Audit
    val nativeByAbi = elfAudits
      .groupBy { it.abi }
      .mapValues { (_, list) -> list.map { it.libraryName }.sorted() }
      .toSortedMap()

    for ((abi, libs) in nativeByAbi) {
      val allowedForAbi = baseline.allowedNativeLibsByAbi[abi]?.toSet()
      if (allowedForAbi == null) {
        violations.add(
          AuditViolation(
            "NATIVE_LIBS",
            "ERROR",
            "Unexpected native ABI packaged in release archive",
            "$abi -> $libs"
          )
        )
      } else {
        for (lib in libs) {
          if (lib !in allowedForAbi) {
            violations.add(
              AuditViolation(
                "NATIVE_LIBS",
                "ERROR",
                "Un-allowlisted native library (.so) packaged in release archive",
                "$abi/$lib"
              )
            )
          }
        }
      }
    }

    if (baseline.require16KbElfAlignmentOn64Bit) {
      for (elf in elfAudits.sortedBy { it.entryPath }) {
        val is64BitAbi = elf.bitness == 64 || elf.abi == "arm64-v8a" || elf.abi == "x86_64"
        if (is64BitAbi && !elf.is16KbAligned) {
          violations.add(
            AuditViolation(
              "ELF_16KB_ALIGNMENT",
              "ERROR",
              "64-bit native library fails Google Play 16 KB ELF PT_LOAD alignment requirement",
              "${elf.entryPath}: PT_LOAD p_align=${elf.ptLoadAlignments}"
            )
          )
        }
      }
    }

    if (baseline.allowedAssetPrefixes.isNotEmpty()) {
      for (asset in assetEntries.sorted()) {
        if (baseline.allowedAssetPrefixes.none { asset.startsWith(it) }) {
          violations.add(
            AuditViolation("ARCHIVE_ASSETS", "ERROR", "Un-allowlisted asset packaged in release archive", asset)
          )
        }
      }
    }

    if (metaInfServiceEntries.size > baseline.allowedMetaInfServiceCountMax) {
      violations.add(
        AuditViolation(
          "ARCHIVE_META_INF",
          "ERROR",
          "META-INF/services entry count exceeds baseline threshold",
          "actual=${metaInfServiceEntries.size}, max=${baseline.allowedMetaInfServiceCountMax}"
        )
      )
    }

    val nativeAudit = NativeAndArchiveAuditResult(
      elfBinaries = elfAudits.sortedBy { it.entryPath },
      nativeLibsByAbi = nativeByAbi,
      assetEntries = assetEntries.sorted(),
      metaInfServiceEntries = metaInfServiceEntries.sorted()
    )

    val status = if (violations.isEmpty()) "PASS" else "FAIL"
    return ReleaseArtifactAuditReport(
      status = status,
      artifactPath = archiveFile.name,
      artifactType = artifactType,
      artifactSizeBytes = archiveFile.length(),
      violationCount = violations.size,
      violations = violations,
      manifest = manifestAudit,
      dex = dexAudit,
      resources = resAudit,
      r8 = r8Audit,
      nativeAndArchive = nativeAudit
    )
  }

  fun toJson(report: ReleaseArtifactAuditReport): String = gson.toJson(report)

  fun formatTextReport(report: ReleaseArtifactAuditReport): String = buildString {
    appendLine("=========================================================================")
    appendLine("DETERMINISTIC BINARY RELEASE ARTIFACT AUDIT REPORT")
    appendLine("=========================================================================")
    appendLine("Artifact : ${report.artifactPath} (${report.artifactType}, ${String.format(Locale.US, "%.2f", report.artifactSizeBytes / 1024.0 / 1024.0)} MB)")
    appendLine("Verdict  : ${report.status} (${report.violationCount} violations)")

    if (report.violations.isNotEmpty()) {
      appendLine("\n[!] VIOLATIONS (${report.violations.size})")
      report.violations.forEach { v ->
        appendLine("  - [${v.category}] ${v.message}${if (v.evidence.isNotEmpty()) " :: ${v.evidence}" else ""}")
      }
    }

    val m = report.manifest
    appendLine("\n[1] BINARY MANIFEST (decoded from packaged base/manifest/AndroidManifest.xml via aapt2-proto -> DOM)")
    appendLine("  package=${m.packageName} versionCode=${m.versionCode} versionName=${m.versionName}")
    appendLine("  debuggable=${m.debuggable} allowBackup=${m.allowBackup} usesCleartextTraffic=${m.usesCleartextTraffic} firebase_data_collection=${m.firebaseDataCollectionDefaultEnabled}")
    appendLine("  Permissions (${m.permissions.size}):")
    m.permissions.forEach { appendLine("    - $it") }
    appendLine("  Activities (${m.activities.size}):")
    m.activities.forEach { appendLine("    - ${it.name} [exported=${it.effectiveExported}, perm=${it.permission ?: "none"}, launcher=${it.isMainLauncher}]") }
    appendLine("  Services (${m.services.size}):")
    m.services.forEach { appendLine("    - ${it.name} [exported=${it.effectiveExported}, perm=${it.permission ?: "none"}]") }
    appendLine("  Receivers (${m.receivers.size}):")
    m.receivers.forEach { appendLine("    - ${it.name} [exported=${it.effectiveExported}, perm=${it.permission ?: "none"}]") }
    appendLine("  Providers (${m.providers.size}):")
    m.providers.forEach { appendLine("    - ${it.name} [exported=${it.effectiveExported}, authorities=${it.authorities ?: "none"}]") }
    appendLine("  Packaged Security XMLs:")
    appendLine("    - provider_paths.xml        : ${m.packagedXmlAudit.providerPathsEntries}")
    appendLine("    - backup_rules.xml excludes : ${m.packagedXmlAudit.backupExcludeDomains}")
    appendLine("    - cloud-backup excludes     : ${m.packagedXmlAudit.cloudBackupExcludeDomains}")
    appendLine("    - device-transfer excludes  : ${m.packagedXmlAudit.deviceTransferExcludeDomains}")
    appendLine("    - network_security_config   : cleartextPermitted=${m.packagedXmlAudit.cleartextTrafficPermitted}, trustsUserCerts=${m.packagedXmlAudit.trustsUserCertificates}")

    appendLine("\n[1.1] DECODED BINARY AndroidManifest.xml")
    appendLine("-------------------------------------------------------------------------")
    appendLine(m.decodedXml.trimEnd())
    appendLine("-------------------------------------------------------------------------")

    val d = report.dex
    appendLine("\n[2] BINARY DEX BYTECODE AUDIT (string_ids / type_ids / method_ids / invoke-* walker)")
    d.dexFiles.forEach { df ->
      appendLine("  ${df.entryName}: v${df.dexVersion}, ${String.format(Locale.US, "%.2f", df.sizeBytes / 1024.0 / 1024.0)} MB, strings=${df.stringCount}, types=${df.typeCount}, method_ids=${df.methodIdCount}, classes=${df.classDefCount}, definedMethods=${df.definedMethodCount}, scannedInvokes=${df.scannedInvokeCount}")
    }
    appendLine("  Forbidden method_ids hits  : ${d.forbiddenMethodIdHits.size}")
    appendLine("  Forbidden invoke-* hits    : ${d.forbiddenInvokeHits.size}")
    appendLine("  Forbidden type_ids hits    : ${d.forbiddenTypeHits.size}")
    appendLine("  Forbidden string_ids hits  : ${d.forbiddenStringHits.size}")
    appendLine("  Cleartext http:// DEX hits : ${d.cleartextUrlHits.size}")
    appendLine("  Surviving classes / defined methods by package (top 15):")
    d.survivingClassesByPackage.entries.take(15).forEach { (pkg, cnt) ->
      val mCnt = d.survivingMethodsByPackage[pkg] ?: 0
      appendLine("    - $pkg: $cnt classes, $mCnt methods")
    }

    val res = report.resources
    appendLine("\n[3] PACKAGED RESOURCES TABLE (base/resources.pb)")
    appendLine("  String entries scanned : ${res.totalStringEntries}")
    appendLine("  XML file refs scanned  : ${res.totalXmlFileEntries}")
    appendLine("  Forbidden string hits  : ${res.forbiddenResourceStringHits.size}")
    appendLine("  Cleartext http:// hits : ${res.cleartextResourceUrlHits.size}")

    val r8 = report.r8
    appendLine("\n[4] R8 KEEP RULES & BLAST-RADIUS AUDIT (configuration.txt + seeds.txt + usage.txt + mapping.txt)")
    appendLine("  Directives applied   : ${r8.totalDirectives} across ${r8.sourceCount} sources")
    appendLine("  Broad rule findings  : ${r8.broadRuleFindings.size}")
    appendLine("  seeds.txt pinned     : ${r8.seedsTotalClasses} classes, ${r8.seedsTotalMembers} members (app com.example.*: ${r8.seedsAppClasses.size} classes)")
    appendLine("  mapping.txt coverage : ${r8.mappingObfuscatedClasses}/${r8.mappingTotalClasses} obfuscated (${r8.mappingObfuscationPercent}%), unobfuscated app classes=${r8.unobfuscatedAppClasses.size}")
    appendLine("  usage.txt stripped   : ${r8.usageStrippedClasses} classes, ${r8.usageStrippedMembers} members")
    appendLine("  Unobfuscated com.example.* classes (${r8.unobfuscatedAppClasses.size}):")
    r8.unobfuscatedAppClasses.forEach { appendLine("    - $it") }
    appendLine("  Applied R8 KEEP directives by source:")
    r8.ruleGroups.forEach { g ->
      appendLine("  >>> Source: ${g.source} (${g.ruleCount} rules)")
      g.rules.forEach { rule ->
        rule.lines().forEach { line -> appendLine("      $line") }
      }
    }

    val nat = report.nativeAndArchive
    appendLine("\n[5] NATIVE LIBRARIES (ELF 16 KB ALIGNMENT), ASSETS & META-INF/services")
    nat.elfBinaries.forEach { elf ->
      appendLine("  ${elf.entryPath}: ELF${elf.bitness}, PT_LOAD p_align=${elf.ptLoadAlignments}, 16KB_OK=${elf.is16KbAligned}")
    }
    appendLine("  Assets count           : ${nat.assetEntries.size}")
    appendLine("  META-INF/services count: ${nat.metaInfServiceEntries.size}")
  }
}
