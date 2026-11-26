package com.aistudio.audit

import com.android.aapt.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReleaseArtifactAuditMutationTest {

  @get:Rule
  val tempFolder = TemporaryFolder()

  private val androidNs = "http://schemas.android.com/apk/res/android"

  private fun baseAllowlist(): ReleaseArtifactBaseline = ReleaseArtifactBaseline(
    schemaVersion = 1,
    expectedPackage = "com.example.testapp",
    allowedPermissions = listOf(
      "android.permission.INTERNET"
    ),
    allowedActivities = listOf(
      "com.example.testapp.MainActivity"
    ),
    allowedServices = emptyList(),
    allowedReceivers = listOf(
      "com.example.testapp.ProtectedReceiver"
    ),
    allowedProviders = listOf(
      "androidx.core.content.FileProvider"
    ),
    allowedExportedWithoutPermission = emptyList(),
    allowedNativeLibsByAbi = mapOf(
      "arm64-v8a" to listOf("libclean.so")
    ),
    allowedAssetPrefixes = listOf("base/assets/models/"),
    allowedMetaInfServiceCountMax = 4,
    maxUnobfuscatedAppClasses = 10,
    require16KbElfAlignmentOn64Bit = true
  )

  @Test
  fun cleanBaselineAab_passesWithZeroViolations() {
    val fixture = buildFixture()
    val report = runVerify(fixture)
    assertEquals("Expected PASS on clean baseline, got violations: ${report.violations}", "PASS", report.status)
    assertEquals(0, report.violationCount)
    assertEquals(1, report.dex.dexFiles.size)
    assertTrue("Expected scannedInvokeCount >= 1", report.dex.dexFiles.first().scannedInvokeCount >= 1)
  }

  @Test
  fun mutation_unallowlistedPermissionInBinaryManifest_fails() {
    val fixture = buildFixture(extraPermissions = listOf("android.permission.READ_CONTACTS"))
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected MANIFEST_ALLOWLIST violation for READ_CONTACTS, got ${report.violations}",
      report.violations.any { it.category == "MANIFEST_ALLOWLIST" && it.evidence.contains("READ_CONTACTS") }
    )
  }

  @Test
  fun mutation_unprotectedExportedReceiver_fails() {
    val fixture = buildFixture(injectUnprotectedExportedReceiver = true)
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected MANIFEST_EXPORTED violation, got ${report.violations}",
      report.violations.any { it.category == "MANIFEST_EXPORTED" && it.evidence.contains("LeakedReceiver") }
    )
  }

  @Test
  fun mutation_debuggableTrueAndAllowBackupTrue_fails() {
    val fixture = buildFixture(debuggable = true, allowBackup = true)
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(report.violations.any { it.category == "MANIFEST" && it.message.contains("debuggable") })
    assertTrue(report.violations.any { it.category == "MANIFEST" && it.message.contains("allowBackup") })
  }

  @Test
  fun mutation_broadFileProviderAndCleartextNetworkSecurityConfig_fails() {
    val fixture = buildFixture(corruptProviderPaths = true, allowCleartextNetSec = true)
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected provider_paths XML violation, got ${report.violations}",
      report.violations.any { it.category == "MANIFEST_XML" && it.message.contains("provider_paths") }
    )
    assertTrue(
      "Expected network_security_config XML violation, got ${report.violations}",
      report.violations.any { it.category == "MANIFEST_XML" && it.message.contains("network_security_config") }
    )
  }

  @Test
  fun mutation_realLogDInvokeInBinaryDex_failsWithMethodAndInvokeAttribution() {
    val fixture = buildFixture(
      dexTargetClass = "Landroid/util/Log;",
      dexTargetMethod = "d",
      dexTargetParams = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
      dexTargetReturn = "I",
      dexInvokeOpcode = 0x71 // invoke-static
    )
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected forbiddenMethodIdHits to detect Log.d, got ${report.dex.forbiddenMethodIdHits}",
      report.dex.forbiddenMethodIdHits.any { it.contains("Landroid/util/Log;->d") }
    )
    assertTrue(
      "Expected forbiddenInvokeHits to attribute caller class, got ${report.dex.forbiddenInvokeHits}",
      report.dex.forbiddenInvokeHits.any {
        it.callerClass == "Lcom/example/testapp/Sample;" && it.targetMethod.contains("Landroid/util/Log;->d")
      }
    )
  }

  @Test
  fun mutation_realPrintStackTraceInvokeInBinaryDex_fails() {
    val fixture = buildFixture(
      dexTargetClass = "Ljava/lang/Throwable;",
      dexTargetMethod = "printStackTrace",
      dexTargetParams = emptyList(),
      dexTargetReturn = "V",
      dexInvokeOpcode = 0x6e // invoke-virtual
    )
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected DEX_BYTECODE violation for Throwable.printStackTrace()V, got ${report.violations}",
      report.violations.any { it.category == "DEX_BYTECODE" && it.evidence.contains("printStackTrace()V") }
    )
  }

  @Test
  fun mutation_leakedDevHostAndCleartextUrlInResourcesPb_fails() {
    val fixture = buildFixture(
      extraResourceStrings = mapOf(
        "dev_url" to "https://ais-dev-secret-preview.run.app/api",
        "insecure_api" to "http://plain.untrusted.example/v1"
      )
    )
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected RESOURCES_PB violation for ais-dev-, got ${report.violations}",
      report.violations.any { it.category == "RESOURCES_PB" && it.evidence.contains("ais-dev-") }
    )
    assertTrue(
      "Expected RESOURCES_PB violation for cleartext http://, got ${report.violations}",
      report.violations.any { it.category == "RESOURCES_PB" && it.evidence.contains("http://plain.untrusted.example") }
    )
  }

  @Test
  fun mutation_overBroadR8KeepRuleAndDontObfuscate_fails() {
    val fixture = buildFixture(
      extraR8Config = """
        # The proguard configuration file for the following section is /app/proguard-rules.pro
        -dontobfuscate
        -keep class ** { *; }
      """.trimIndent()
    )
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected R8_KEEP_RULES violation for -dontobfuscate, got ${report.violations}",
      report.violations.any { it.category == "R8_KEEP_RULES" && it.evidence.contains("-dontobfuscate") }
    )
    assertTrue(
      "Expected R8_KEEP_RULES violation for -keep class ** { *; }, got ${report.violations}",
      report.violations.any { it.category == "R8_KEEP_RULES" && it.evidence.contains("-keep class **") }
    )
  }

  @Test
  fun mutation_misaligned4KbElf64AndUnexpectedNativeLib_fails() {
    val fixture = buildFixture(elf64Alignment = 4096L, extraNativeLib = "libunexpected_tracker.so")
    val report = runVerify(fixture)
    assertEquals("FAIL", report.status)
    assertTrue(
      "Expected ELF_16KB_ALIGNMENT violation, got ${report.violations}",
      report.violations.any { it.category == "ELF_16KB_ALIGNMENT" && it.evidence.contains("4096") }
    )
    assertTrue(
      "Expected NATIVE_LIBS violation for libunexpected_tracker.so, got ${report.violations}",
      report.violations.any { it.category == "NATIVE_LIBS" && it.evidence.contains("libunexpected_tracker.so") }
    )
  }

  // ---- Fixture construction helpers ----

  private data class AuditFixture(
    val aab: File,
    val config: File,
    val seeds: File,
    val usage: File,
    val mapping: File
  )

  private fun runVerify(f: AuditFixture): ReleaseArtifactAuditReport = ReleaseArtifactVerifier.verify(
    archiveFile = f.aab,
    r8ConfigurationFile = f.config,
    r8SeedsFile = f.seeds,
    r8UsageFile = f.usage,
    r8MappingFile = f.mapping,
    baseline = baseAllowlist()
  )

  private fun buildFixture(
    extraPermissions: List<String> = emptyList(),
    injectUnprotectedExportedReceiver: Boolean = false,
    debuggable: Boolean = false,
    allowBackup: Boolean = false,
    corruptProviderPaths: Boolean = false,
    allowCleartextNetSec: Boolean = false,
    dexTargetClass: String = "Ljava/lang/StringBuilder;",
    dexTargetMethod: String = "toString",
    dexTargetParams: List<String> = emptyList(),
    dexTargetReturn: String = "Ljava/lang/String;",
    dexInvokeOpcode: Int = 0x6e,
    extraResourceStrings: Map<String, String> = emptyMap(),
    extraR8Config: String = "",
    elf64Alignment: Long = 16384L,
    extraNativeLib: String? = null
  ): AuditFixture {
    val dir = tempFolder.newFolder()
    val aabFile = File(dir, "synthetic-release.aab")

    val manifestBytes = buildProtoManifest(
      extraPermissions = extraPermissions,
      injectUnprotectedExportedReceiver = injectUnprotectedExportedReceiver,
      debuggable = debuggable,
      allowBackup = allowBackup
    )
    val resourcesBytes = buildProtoResources(extraResourceStrings)
    val dexBytes = buildMinimalDex(
      callerClass = "Lcom/example/testapp/Sample;",
      targetClass = dexTargetClass,
      targetMethod = dexTargetMethod,
      targetParams = dexTargetParams,
      targetReturn = dexTargetReturn,
      invokeOpcode = dexInvokeOpcode
    )
    val elfBytes = buildMinimalElf64(elf64Alignment)

    ZipOutputStream(aabFile.outputStream()).use { zos ->
      fun put(path: String, bytes: ByteArray) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(bytes)
        zos.closeEntry()
      }
      put("base/manifest/AndroidManifest.xml", manifestBytes)
      put("base/resources.pb", resourcesBytes)
      put("base/dex/classes.dex", dexBytes)
      put("base/res/xml/provider_paths.xml", buildProviderPathsXml(corruptProviderPaths))
      put("base/res/xml/backup_rules.xml", buildBackupRulesXml())
      put("base/res/xml/data_extraction_rules.xml", buildDataExtractionRulesXml())
      put("base/res/xml/network_security_config.xml", buildNetSecXml(allowCleartextNetSec))
      put("base/lib/arm64-v8a/libclean.so", elfBytes)
      if (extraNativeLib != null) {
        put("base/lib/arm64-v8a/$extraNativeLib", buildMinimalElf64(16384L))
      }
      put("base/assets/models/model.bin", byteArrayOf(1, 2, 3))
    }

    val configFile = File(dir, "configuration.txt").apply {
      writeText(
        """
        # The proguard configuration file for the following section is /app/proguard-rules.pro
        -keep @androidx.annotation.Keep class * { *; }
        -assumenosideeffects class android.util.Log {
            public static int v(...);
            public static int d(...);
            public static int i(...);
        }
        $extraR8Config
        """.trimIndent()
      )
    }
    val seedsFile = File(dir, "seeds.txt").apply {
      writeText(
        """
        com.example.testapp.MainActivity
        com.example.testapp.MainActivity: void <init>()
        """.trimIndent()
      )
    }
    val usageFile = File(dir, "usage.txt").apply {
      writeText(
        """
        com.example.unused.DeadHelper
            public void neverCalled()
        """.trimIndent()
      )
    }
    val mappingFile = File(dir, "mapping.txt").apply {
      writeText(
        """
        com.example.testapp.MainActivity -> com.example.testapp.MainActivity:
        com.example.testapp.SecretManager -> a.a:
        """.trimIndent()
      )
    }
    return AuditFixture(aabFile, configFile, seedsFile, usageFile, mappingFile)
  }

  private fun attr(name: String, value: String, ns: String = androidNs): Resources.XmlAttribute {
    return Resources.XmlAttribute.newBuilder()
      .setNamespaceUri(ns)
      .setName(name)
      .setValue(value)
      .build()
  }

  private fun boolAttr(name: String, value: Boolean, ns: String = androidNs): Resources.XmlAttribute {
    return Resources.XmlAttribute.newBuilder()
      .setNamespaceUri(ns)
      .setName(name)
      .setCompiledItem(
        Resources.Item.newBuilder()
          .setPrim(Resources.Primitive.newBuilder().setBooleanValue(value))
      )
      .build()
  }

  private fun elemNode(name: String, block: Resources.XmlElement.Builder.() -> Unit = {}): Resources.XmlNode {
    val el = Resources.XmlElement.newBuilder().setName(name).apply(block).build()
    return Resources.XmlNode.newBuilder().setElement(el).build()
  }

  private fun buildProtoManifest(
    extraPermissions: List<String>,
    injectUnprotectedExportedReceiver: Boolean,
    debuggable: Boolean,
    allowBackup: Boolean
  ): ByteArray {
    val appBuilder = Resources.XmlElement.newBuilder().setName("application")
      .addAttribute(boolAttr("allowBackup", allowBackup))
      .addAttribute(boolAttr("debuggable", debuggable))
      .addChild(
        elemNode("meta-data") {
          addAttribute(attr("name", "firebase_data_collection_default_enabled"))
          addAttribute(boolAttr("value", false))
        }
      )
      .addChild(
        elemNode("activity") {
          addAttribute(attr("name", "com.example.testapp.MainActivity"))
          addAttribute(boolAttr("exported", true))
          addChild(
            elemNode("intent-filter") {
              addChild(elemNode("action") { addAttribute(attr("name", "android.intent.action.MAIN")) })
              addChild(elemNode("category") { addAttribute(attr("name", "android.intent.category.LAUNCHER")) })
            }
          )
        }
      )
      .addChild(
        elemNode("receiver") {
          addAttribute(attr("name", "com.example.testapp.ProtectedReceiver"))
          addAttribute(boolAttr("exported", true))
          addAttribute(attr("permission", "android.permission.DUMP"))
        }
      )
      .addChild(
        elemNode("provider") {
          addAttribute(attr("name", "androidx.core.content.FileProvider"))
          addAttribute(attr("authorities", "com.example.testapp.fileprovider"))
          addAttribute(boolAttr("exported", false))
        }
      )

    if (injectUnprotectedExportedReceiver) {
      appBuilder.addChild(
        elemNode("receiver") {
          addAttribute(attr("name", "com.example.testapp.LeakedReceiver"))
          addAttribute(boolAttr("exported", true))
        }
      )
    }

    val manifestEl = Resources.XmlElement.newBuilder()
      .setName("manifest")
      .addAttribute(attr("package", "com.example.testapp", ns = ""))
      .addAttribute(attr("versionCode", "1"))
      .addAttribute(attr("versionName", "1.0.0"))
      .addChild(elemNode("uses-permission") { addAttribute(attr("name", "android.permission.INTERNET")) })
      .apply {
        extraPermissions.forEach { p ->
          addChild(elemNode("uses-permission") { addAttribute(attr("name", p)) })
        }
      }
      .addChild(Resources.XmlNode.newBuilder().setElement(appBuilder.build()))
      .build()

    return Resources.XmlNode.newBuilder().setElement(manifestEl).build().toByteArray()
  }

  private fun buildProviderPathsXml(corrupt: Boolean): ByteArray {
    val root = Resources.XmlElement.newBuilder().setName("paths")
      .addChild(
        elemNode(if (corrupt) "root-path" else "cache-path") {
          addAttribute(attr("name", "shared_docs", ns = ""))
          addAttribute(attr("path", if (corrupt) "." else "docs/", ns = ""))
        }
      )
      .build()
    return Resources.XmlNode.newBuilder().setElement(root).build().toByteArray()
  }

  private fun buildBackupRulesXml(): ByteArray {
    val domains = listOf("root", "file", "database", "sharedpref", "external")
    val root = Resources.XmlElement.newBuilder().setName("full-backup-content").apply {
      domains.forEach { d ->
        addChild(elemNode("exclude") {
          addAttribute(attr("domain", d, ns = ""))
          addAttribute(attr("path", ".", ns = ""))
        })
      }
    }.build()
    return Resources.XmlNode.newBuilder().setElement(root).build().toByteArray()
  }

  private fun buildDataExtractionRulesXml(): ByteArray {
    val domains = listOf("root", "file", "database", "sharedpref", "external")
    fun section(name: String) = elemNode(name) {
      domains.forEach { d ->
        addChild(elemNode("exclude") {
          addAttribute(attr("domain", d, ns = ""))
          addAttribute(attr("path", ".", ns = ""))
        })
      }
    }
    val root = Resources.XmlElement.newBuilder().setName("data-extraction-rules")
      .addChild(section("cloud-backup"))
      .addChild(section("device-transfer"))
      .build()
    return Resources.XmlNode.newBuilder().setElement(root).build().toByteArray()
  }

  private fun buildNetSecXml(allowCleartext: Boolean): ByteArray {
    val root = Resources.XmlElement.newBuilder().setName("network-security-config")
      .addChild(
        elemNode("base-config") {
          addAttribute(attr("cleartextTrafficPermitted", allowCleartext.toString(), ns = ""))
        }
      )
      .build()
    return Resources.XmlNode.newBuilder().setElement(root).build().toByteArray()
  }

  private fun buildProtoResources(extraStrings: Map<String, String>): ByteArray {
    val typeBuilder = Resources.Type.newBuilder().setName("string")
    val allStrings = linkedMapOf("app_name" to "TestApp") + extraStrings
    allStrings.forEach { (k, v) ->
      typeBuilder.addEntry(
        Resources.Entry.newBuilder()
          .setName(k)
          .addConfigValue(
            Resources.ConfigValue.newBuilder()
              .setValue(
                Resources.Value.newBuilder()
                  .setItem(
                    Resources.Item.newBuilder()
                      .setStr(Resources.String.newBuilder().setValue(v))
                  )
              )
          )
      )
    }
    return Resources.ResourceTable.newBuilder()
      .addPackage(
        Resources.Package.newBuilder()
          .setPackageName("com.example.testapp")
          .addType(typeBuilder)
      )
      .build()
      .toByteArray()
  }

  private fun buildMinimalElf64(ptLoadAlign: Long): ByteArray {
    val bytes = ByteArray(128)
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    bytes[0] = 0x7F
    bytes[1] = 'E'.code.toByte()
    bytes[2] = 'L'.code.toByte()
    bytes[3] = 'F'.code.toByte()
    bytes[4] = 2 // ELF64
    bytes[5] = 1 // little endian
    buf.putLong(32, 64L) // e_phoff
    buf.putShort(54, 56) // e_phentsize
    buf.putShort(56, 1)  // e_phnum
    // Program header 0 at offset 64
    buf.putInt(64, 1) // p_type = PT_LOAD
    buf.putLong(64 + 48, ptLoadAlign) // p_align
    return bytes
  }

  // Builds a valid binary DEX with string_ids, type_ids, proto_ids, method_ids, class_defs, class_data_item, and code_item
  private fun buildMinimalDex(
    callerClass: String,
    targetClass: String,
    targetMethod: String,
    targetParams: List<String>,
    targetReturn: String,
    invokeOpcode: Int
  ): ByteArray {
    val shorty = buildString {
      append(if (targetReturn.startsWith("L")) 'L' else targetReturn.first())
      targetParams.forEach { append(if (it.startsWith("L")) 'L' else it.first()) }
    }
    val stringSet = linkedSetOf(
      "V", "I", "Ljava/lang/Object;", "Ljava/lang/String;",
      callerClass, targetClass, targetMethod, shorty, "run", targetReturn
    ).apply { addAll(targetParams) }
    val strings = stringSet.sorted()
    val strIdx = strings.withIndex().associate { it.value to it.index }

    val types = linkedSetOf("V", "I", "Ljava/lang/Object;", "Ljava/lang/String;", callerClass, targetClass, targetReturn)
      .apply { addAll(targetParams) }
      .sorted()
    val typeIdx = types.withIndex().associate { it.value to it.index }

    // Data section layout after fixed tables
    val headerSize = 112
    val stringIdsOff = headerSize
    val typeIdsOff = stringIdsOff + strings.size * 4
    val protoIdsOff = typeIdsOff + types.size * 4
    val protoCount = 2 // 0 = ()V for caller.run, 1 = target proto
    val methodIdsOff = protoIdsOff + protoCount * 12
    val methodCount = 2 // 0 = caller.run()V, 1 = targetMethod
    val classDefsOff = methodIdsOff + methodCount * 8
    val dataStart = classDefsOff + 32 // 1 class_def

    val dataBuf = ByteArrayOutputStream()
    fun align4() {
      while ((dataStart + dataBuf.size()) % 4 != 0) dataBuf.write(0)
    }
    fun writeUleb128(v: Int) {
      var x = v
      do {
        var b = x and 0x7F
        x = x ushr 7
        if (x != 0) b = b or 0x80
        dataBuf.write(b)
      } while (x != 0)
    }
    fun writeIntLE(v: Int) {
      dataBuf.write(v and 0xFF)
      dataBuf.write((v ushr 8) and 0xFF)
      dataBuf.write((v ushr 16) and 0xFF)
      dataBuf.write((v ushr 24) and 0xFF)
    }
    fun writeShortLE(v: Int) {
      dataBuf.write(v and 0xFF)
      dataBuf.write((v ushr 8) and 0xFF)
    }

    // 1. String data items
    val stringOffsets = IntArray(strings.size)
    strings.forEachIndexed { idx, s ->
      stringOffsets[idx] = dataStart + dataBuf.size()
      writeUleb128(s.length)
      dataBuf.write(s.toByteArray(Charsets.UTF_8))
      dataBuf.write(0)
    }

    // 2. TypeList for targetParams (if non-empty)
    var targetParamsOff = 0
    if (targetParams.isNotEmpty()) {
      align4()
      targetParamsOff = dataStart + dataBuf.size()
      writeIntLE(targetParams.size)
      targetParams.forEach { writeShortLE(typeIdx.getValue(it)) }
    }

    // 3. code_item for callerClass->run()V: invokes method_idx=1 then return-void (0x0e)
    align4()
    val codeOff = dataStart + dataBuf.size()
    writeShortLE(2) // registers_size
    writeShortLE(0) // ins_size
    writeShortLE(2) // outs_size
    writeShortLE(0) // tries_size
    writeIntLE(0)   // debug_info_off
    writeIntLE(4)   // insns_size = 4 16-bit units (3 for invoke-* + 1 for return-void)
    writeShortLE((0x00 shl 8) or (invokeOpcode and 0xFF)) // opcode + arg count
    writeShortLE(1) // method_idx = 1 (targetMethod)
    writeShortLE(0) // arg registers
    writeShortLE(0x000e) // return-void

    // 4. class_data_item (0 static, 0 instance, 1 direct method, 0 virtual)
    val classDataOff = dataStart + dataBuf.size()
    writeUleb128(0) // static_fields_size
    writeUleb128(0) // instance_fields_size
    writeUleb128(1) // direct_methods_size
    writeUleb128(0) // virtual_methods_size
    writeUleb128(0) // method_idx_diff = 0 (callerClass->run()V)
    writeUleb128(0x0009) // PUBLIC STATIC
    writeUleb128(codeOff)

    val totalSize = dataStart + dataBuf.size()
    val out = ByteArray(totalSize)
    val buf = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)

    // Header
    "dex\n037\u0000".toByteArray(Charsets.US_ASCII).copyInto(out, 0)
    buf.putInt(32, totalSize)
    buf.putInt(36, headerSize)
    buf.putInt(40, 0x12345678)
    buf.putInt(56, strings.size)
    buf.putInt(60, stringIdsOff)
    buf.putInt(64, types.size)
    buf.putInt(68, typeIdsOff)
    buf.putInt(72, protoCount)
    buf.putInt(76, protoIdsOff)
    buf.putInt(88, methodCount)
    buf.putInt(92, methodIdsOff)
    buf.putInt(96, 1)
    buf.putInt(100, classDefsOff)

    // string_ids
    stringOffsets.forEachIndexed { idx, off -> buf.putInt(stringIdsOff + idx * 4, off) }
    // type_ids
    types.forEachIndexed { idx, t -> buf.putInt(typeIdsOff + idx * 4, strIdx.getValue(t)) }
    // proto_ids[0] = ()V
    buf.putInt(protoIdsOff, strIdx.getValue("V"))
    buf.putInt(protoIdsOff + 4, typeIdx.getValue("V"))
    buf.putInt(protoIdsOff + 8, 0)
    // proto_ids[1] = target
    buf.putInt(protoIdsOff + 12, strIdx.getValue(shorty))
    buf.putInt(protoIdsOff + 16, typeIdx.getValue(targetReturn))
    buf.putInt(protoIdsOff + 20, targetParamsOff)

    // method_ids[0] = callerClass->run()V
    buf.putShort(methodIdsOff, typeIdx.getValue(callerClass).toShort())
    buf.putShort(methodIdsOff + 2, 0)
    buf.putInt(methodIdsOff + 4, strIdx.getValue("run"))
    // method_ids[1] = targetClass->targetMethod
    buf.putShort(methodIdsOff + 8, typeIdx.getValue(targetClass).toShort())
    buf.putShort(methodIdsOff + 10, 1)
    buf.putInt(methodIdsOff + 12, strIdx.getValue(targetMethod))

    // class_defs[0] = callerClass
    buf.putInt(classDefsOff, typeIdx.getValue(callerClass))
    buf.putInt(classDefsOff + 4, 0x0001) // PUBLIC
    buf.putInt(classDefsOff + 8, typeIdx.getValue("Ljava/lang/Object;"))
    buf.putInt(classDefsOff + 24, classDataOff)

    dataBuf.toByteArray().copyInto(out, dataStart)
    return out
  }
}
