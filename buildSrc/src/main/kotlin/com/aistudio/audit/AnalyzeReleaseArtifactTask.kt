package com.aistudio.audit

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class AnalyzeReleaseArtifactTask : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val archiveFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val r8ConfigurationFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val r8SeedsFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val r8UsageFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val r8MappingFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val baselineFile: RegularFileProperty

  @get:OutputFile
  abstract val jsonReportFile: RegularFileProperty

  @get:OutputFile
  abstract val textReportFile: RegularFileProperty

  @TaskAction
  fun verifyReleaseArtifact() {
    val archive = archiveFile.get().asFile
    val configFile = r8ConfigurationFile.get().asFile
    val seedsFile = r8SeedsFile.get().asFile
    val usageFile = r8UsageFile.get().asFile
    val mappingFile = r8MappingFile.get().asFile
    val baselineJson = baselineFile.get().asFile

    listOf(
      "Packaged release archive" to archive,
      "R8 configuration.txt" to configFile,
      "R8 seeds.txt" to seedsFile,
      "R8 usage.txt" to usageFile,
      "R8 mapping.txt" to mappingFile,
      "Baseline allowlist JSON" to baselineJson
    ).forEach { (label, file) ->
      if (!file.exists() || file.length() == 0L) {
        throw GradleException("$label is missing or empty: ${file.absolutePath}")
      }
    }

    val baseline = ReleaseArtifactVerifier.loadBaseline(baselineJson)
    val report = ReleaseArtifactVerifier.verify(
      archiveFile = archive,
      r8ConfigurationFile = configFile,
      r8SeedsFile = seedsFile,
      r8UsageFile = usageFile,
      r8MappingFile = mappingFile,
      baseline = baseline
    )

    val jsonOut = jsonReportFile.get().asFile
    val textOut = textReportFile.get().asFile
    jsonOut.parentFile.mkdirs()
    textOut.parentFile.mkdirs()

    jsonOut.writeText(ReleaseArtifactVerifier.toJson(report), Charsets.UTF_8)
    val textReport = ReleaseArtifactVerifier.formatTextReport(report)
    textOut.writeText(textReport, Charsets.UTF_8)

    // Print concise summary to stdout; full dump + JSON live in app/build/reports/
    println("=========================================================================")
    println("RELEASE ARTIFACT VERIFIER: ${report.status} (${report.violationCount} violations)")
    println("  Archive    : ${report.artifactPath} (${report.artifactType}, ${report.artifactSizeBytes} bytes)")
    println("  Manifest   : pkg=${report.manifest.packageName}, perms=${report.manifest.permissions.size}, activities=${report.manifest.activities.size}, services=${report.manifest.services.size}, receivers=${report.manifest.receivers.size}, providers=${report.manifest.providers.size}")
    println("  DEX        : files=${report.dex.dexFiles.size}, classes=${report.dex.dexFiles.sumOf { it.classDefCount }}, method_ids=${report.dex.dexFiles.sumOf { it.methodIdCount }}, scanned_invokes=${report.dex.dexFiles.sumOf { it.scannedInvokeCount }}")
    println("  Resources  : strings=${report.resources.totalStringEntries}, xml_files=${report.resources.totalXmlFileEntries}")
    println("  R8         : directives=${report.r8.totalDirectives} (${report.r8.sourceCount} sources), seeds=${report.r8.seedsTotalClasses} cls/${report.r8.seedsTotalMembers} mbr, obfuscated=${report.r8.mappingObfuscationPercent}%, unobf_app=${report.r8.unobfuscatedAppClasses.size}, stripped=${report.r8.usageStrippedClasses} cls")
    println("  Native ELF : ${report.nativeAndArchive.elfBinaries.size} .so binaries (64-bit 16KB aligned=${report.nativeAndArchive.elfBinaries.filter { it.bitness == 64 }.all { it.is16KbAligned }})")
    println("  JSON Report: ${jsonOut.absolutePath}")
    println("  Text Report: ${textOut.absolutePath}")
    println("=========================================================================")

    if (report.violations.isNotEmpty()) {
      val details = report.violations.joinToString("\n") { v ->
        "  - [${v.category}] ${v.message}${if (v.evidence.isNotEmpty()) " :: ${v.evidence}" else ""}"
      }
      throw GradleException("analyzeReleaseArtifact FAILED with ${report.violationCount} violation(s):\n$details")
    }
  }
}
