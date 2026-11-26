package com.aistudio.audit

data class ReleaseArtifactBaseline(
  val schemaVersion: Int = 1,
  val expectedPackage: String = "",
  val allowedPermissions: List<String> = emptyList(),
  val allowedActivities: List<String> = emptyList(),
  val allowedServices: List<String> = emptyList(),
  val allowedReceivers: List<String> = emptyList(),
  val allowedProviders: List<String> = emptyList(),
  val allowedExportedWithoutPermission: List<String> = emptyList(),
  val allowedNativeLibsByAbi: Map<String, List<String>> = emptyMap(),
  val allowedAssetPrefixes: List<String> = emptyList(),
  val allowedMetaInfServiceCountMax: Int = 32,
  val forbiddenDexMethodRefs: List<String> = listOf(
    "Landroid/util/Log;->v",
    "Landroid/util/Log;->d",
    "Landroid/util/Log;->i",
    "Ljava/lang/Throwable;->printStackTrace()V"
  ),
  val forbiddenDexTypeSubstrings: List<String> = listOf(
    "DebugAppCheckProviderFactory",
    "com/google/firebase/appcheck/debug/",
    "FirebaseInitProvider",
    "MlKitInitProvider",
    "TransportBackendDiscovery",
    "JobInfoSchedulerService",
    "AlarmManagerSchedulerBroadcastReceiver"
  ),
  val forbiddenStringTokens: List<String> = listOf(
    "ais-dev-",
    "DebugAppCheckProviderFactory",
    "com/google/firebase/appcheck/debug"
  ),
  val forbiddenDexOnlyStringTokens: List<String> = listOf(
    "321866087498-2ep2f75056ttc1lc33aede3mq2haf3vt.apps.googleusercontent.com"
  ),
  val allowedCleartextUrlPrefixes: List<String> = listOf(
    "http://schemas.android.com/",
    "http://www.w3.org/",
    "http://xml.org/",
    "http://xmlpull.org/",
    "http://apache.org/",
    "http://ns.adobe.com/",
    "http://ns.google.com/",
    "http://localhost",
    "http://127.0.0.1",
    "http://a.b"
  ),
  val forbiddenBroadR8Directives: List<String> = listOf(
    "-dontobfuscate",
    "-dontshrink",
    "-dontoptimize"
  ),
  val maxUnobfuscatedAppClasses: Int = 60,
  val require16KbElfAlignmentOn64Bit: Boolean = true
)

data class AuditViolation(
  val category: String,
  val severity: String,
  val message: String,
  val evidence: String = ""
)

data class ManifestComponent(
  val type: String,
  val name: String,
  val exportedAttr: Boolean?,
  val effectiveExported: Boolean,
  val permission: String?,
  val hasIntentFilter: Boolean,
  val isMainLauncher: Boolean,
  val authorities: String? = null
)

data class PackagedXmlAuditResult(
  val providerPathsEntries: List<String>,
  val backupExcludeDomains: List<String>,
  val cloudBackupExcludeDomains: List<String>,
  val deviceTransferExcludeDomains: List<String>,
  val cleartextTrafficPermitted: Boolean?,
  val trustsUserCertificates: Boolean,
  val xmlIntegrityViolations: List<AuditViolation>
)

data class ManifestAuditResult(
  val packageName: String,
  val versionCode: String,
  val versionName: String,
  val debuggable: Boolean,
  val allowBackup: Boolean?,
  val usesCleartextTraffic: Boolean?,
  val firebaseDataCollectionDefaultEnabled: String?,
  val permissions: List<String>,
  val activities: List<ManifestComponent>,
  val services: List<ManifestComponent>,
  val receivers: List<ManifestComponent>,
  val providers: List<ManifestComponent>,
  val packagedXmlAudit: PackagedXmlAuditResult,
  val decodedXml: String
)

data class DexMethodRef(
  val classDescriptor: String,
  val methodName: String,
  val signature: String
) {
  val fullDescriptor: String get() = "$classDescriptor->$methodName$signature"
  val shortDescriptor: String get() = "$classDescriptor->$methodName"
}

data class DexInvokeHit(
  val callerClass: String,
  val targetMethod: String
)

data class DexFileSummary(
  val entryName: String,
  val sizeBytes: Long,
  val dexVersion: String,
  val stringCount: Int,
  val typeCount: Int,
  val methodIdCount: Int,
  val classDefCount: Int,
  val definedMethodCount: Int,
  val scannedInvokeCount: Int
)

data class DexAuditResult(
  val dexFiles: List<DexFileSummary>,
  val forbiddenMethodIdHits: List<String>,
  val forbiddenInvokeHits: List<DexInvokeHit>,
  val forbiddenTypeHits: List<String>,
  val forbiddenStringHits: List<String>,
  val cleartextUrlHits: List<String>,
  val survivingClassesByPackage: Map<String, Int>,
  val survivingMethodsByPackage: Map<String, Int>
)

data class ResourceTableAuditResult(
  val totalStringEntries: Int,
  val totalXmlFileEntries: Int,
  val forbiddenResourceStringHits: List<String>,
  val cleartextResourceUrlHits: List<String>
)

data class R8RuleGroup(
  val source: String,
  val ruleCount: Int,
  val rules: List<String>
)

data class R8AuditResult(
  val totalDirectives: Int,
  val sourceCount: Int,
  val ruleGroups: List<R8RuleGroup>,
  val broadRuleFindings: List<String>,
  val seedsTotalClasses: Int,
  val seedsTotalMembers: Int,
  val seedsClassesByPackage: Map<String, Int>,
  val seedsAppClasses: List<String>,
  val mappingTotalClasses: Int,
  val mappingObfuscatedClasses: Int,
  val mappingUnobfuscatedClasses: Int,
  val mappingObfuscationPercent: Double,
  val unobfuscatedAppClasses: List<String>,
  val usageStrippedClasses: Int,
  val usageStrippedMembers: Int
)

data class ElfBinaryAudit(
  val entryPath: String,
  val abi: String,
  val libraryName: String,
  val bitness: Int,
  val ptLoadAlignments: List<Long>,
  val is16KbAligned: Boolean
)

data class NativeAndArchiveAuditResult(
  val elfBinaries: List<ElfBinaryAudit>,
  val nativeLibsByAbi: Map<String, List<String>>,
  val assetEntries: List<String>,
  val metaInfServiceEntries: List<String>
)

data class ReleaseArtifactAuditReport(
  val status: String,
  val artifactPath: String,
  val artifactType: String,
  val artifactSizeBytes: Long,
  val violationCount: Int,
  val violations: List<AuditViolation>,
  val manifest: ManifestAuditResult,
  val dex: DexAuditResult,
  val resources: ResourceTableAuditResult,
  val r8: R8AuditResult,
  val nativeAndArchive: NativeAndArchiveAuditResult
)
