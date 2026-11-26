package com.aistudio.audit

import java.io.File
import java.util.Locale

object R8AuditParser {
  private val BLANKET_GLOBAL_KEEP_REGEX = Regex(
    """^-keep(?:classeswithmembers|classmembers|names)?\b(?:,[^\s]+)*\s+(?:public\s+)?(?:class|interface)\s+(?:\*\*|\*)\s*(?:\{\s*\*\s*;\s*\})?$"""
  )
  private val BLANKET_APP_PACKAGE_KEEP_REGEX = Regex(
    """^-keep\b(?:,[^\s]+)*\s+(?:public\s+)?(?:class|interface)\s+com\.example(?:\.[a-zA-Z0-9_]+)?\.\*\*\s*\{\s*\*\s*;\s*\}$"""
  )

  fun auditR8Outputs(
    configurationFile: File,
    seedsFile: File,
    usageFile: File,
    mappingFile: File,
    baseline: ReleaseArtifactBaseline
  ): R8AuditResult {
    // 1. Parse configuration.txt
    val configLines = configurationFile.readLines(Charsets.UTF_8)
    var currentSource = "Unknown / Inline"
    var inRuleBlock = false
    var braceDepth = 0
    val currentBlock = StringBuilder()
    val rulesBySource = linkedMapOf<String, MutableList<String>>()
    val broadFindings = mutableListOf<String>()
    var totalDirectives = 0

    fun inspectRuleForBroadness(source: String, ruleText: String) {
      val normalized = ruleText.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .trim()

      for (forbidden in baseline.forbiddenBroadR8Directives) {
        if (normalized == forbidden || normalized.startsWith("$forbidden ")) {
          broadFindings.add("[$source] Forbidden R8 directive: $normalized")
        }
      }
      if (BLANKET_GLOBAL_KEEP_REGEX.matches(normalized)) {
        broadFindings.add("[$source] Over-broad global wildcard KEEP rule: $normalized")
      }
      if (BLANKET_APP_PACKAGE_KEEP_REGEX.matches(normalized)) {
        broadFindings.add("[$source] Over-broad app package wildcard KEEP rule: $normalized")
      }
    }

    fun flushBlock() {
      val text = currentBlock.toString().trim()
      if (text.isNotEmpty()) {
        rulesBySource.getOrPut(currentSource) { mutableListOf() }.add(text)
        totalDirectives++
        inspectRuleForBroadness(currentSource, text)
      }
      currentBlock.clear()
      inRuleBlock = false
      braceDepth = 0
    }

    for (rawLine in configLines) {
      val trimmed = rawLine.trim()
      if (trimmed.startsWith("# The proguard configuration file for the following section is ")) {
        if (inRuleBlock) flushBlock()
        val path = trimmed.removePrefix("# The proguard configuration file for the following section is ").trim()
        currentSource = path.substringAfterLast("/transformed/").substringAfterLast("/app/")
        continue
      }
      if (inRuleBlock && braceDepth == 0 && (trimmed.startsWith("-") || trimmed.startsWith("# End of content from "))) {
        flushBlock()
      }
      if (!inRuleBlock) {
        val isTracked = trimmed.startsWith("-keep") ||
          trimmed.startsWith("-assumenosideeffects") ||
          trimmed.startsWith("-if ") ||
          trimmed.startsWith("-dontobfuscate") ||
          trimmed.startsWith("-dontshrink") ||
          trimmed.startsWith("-dontoptimize")
        if (isTracked) {
          inRuleBlock = true
          currentBlock.appendLine(rawLine)
          braceDepth += trimmed.count { it == '{' } - trimmed.count { it == '}' }
          if (braceDepth == 0 && (trimmed.startsWith("-dont") || trimmed.startsWith("-keepattributes"))) {
            flushBlock()
          }
        }
      } else {
        if (trimmed.isEmpty() && braceDepth == 0) {
          flushBlock()
        } else {
          currentBlock.appendLine(rawLine)
          braceDepth += trimmed.count { it == '{' } - trimmed.count { it == '}' }
          if (braceDepth == 0 && trimmed.contains("}")) {
            flushBlock()
          }
        }
      }
    }
    if (inRuleBlock) flushBlock()

    val ruleGroups = rulesBySource.map { (src, list) ->
      R8RuleGroup(source = src, ruleCount = list.size, rules = list)
    }

    // 2. Parse seeds.txt (actual blast radius of KEEP rules)
    val seedClasses = linkedSetOf<String>()
    var seedMembers = 0
    val seedClassesByPkg = linkedMapOf<String, MutableSet<String>>()
    val seedAppClasses = linkedSetOf<String>()

    seedsFile.useLines(Charsets.UTF_8) { seq ->
      seq.forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val colonIdx = line.indexOf(':')
        val className = if (colonIdx >= 0) {
          seedMembers++
          line.substring(0, colonIdx).trim()
        } else {
          line
        }
        if (className.isNotEmpty()) {
          seedClasses.add(className)
          val bucket = packageBucket(className)
          seedClassesByPkg.getOrPut(bucket) { linkedSetOf() }.add(className)
          if (className.startsWith("com.example.")) {
            seedAppClasses.add(className)
          }
        }
      }
    }

    // 3. Parse mapping.txt (actual obfuscation coverage)
    var totalMappedClasses = 0
    var obfuscatedClasses = 0
    var unobfuscatedClasses = 0
    val unobfuscatedAppClasses = linkedSetOf<String>()

    mappingFile.useLines(Charsets.UTF_8) { seq ->
      seq.forEach { line ->
        if (line.isEmpty() || line.startsWith(" ") || line.startsWith("\t") || line.startsWith("#")) return@forEach
        if (!line.endsWith(":")) return@forEach
        val arrowIdx = line.indexOf(" -> ")
        if (arrowIdx <= 0) return@forEach
        val orig = line.substring(0, arrowIdx).trim()
        val obf = line.substring(arrowIdx + 4, line.length - 1).trim()
        totalMappedClasses++
        if (orig == obf) {
          unobfuscatedClasses++
          if (orig.startsWith("com.example.")) {
            unobfuscatedAppClasses.add(orig)
          }
        } else {
          obfuscatedClasses++
        }
      }
    }

    val obfPct = if (totalMappedClasses > 0) {
      String.format(Locale.US, "%.2f", (obfuscatedClasses * 100.0) / totalMappedClasses).toDouble()
    } else {
      0.0
    }

    // 4. Parse usage.txt (dead code stripped by R8)
    var strippedClasses = 0
    var strippedMembers = 0
    usageFile.useLines(Charsets.UTF_8) { seq ->
      seq.forEach { line ->
        if (line.isBlank() || line.startsWith("#")) return@forEach
        if (line.startsWith(" ") || line.startsWith("\t")) {
          strippedMembers++
        } else {
          strippedClasses++
        }
      }
    }

    val sortedSeedByPkg = seedClassesByPkg.entries
      .map { it.key to it.value.size }
      .sortedByDescending { it.second }
      .associate { it.first to it.second }

    return R8AuditResult(
      totalDirectives = totalDirectives,
      sourceCount = ruleGroups.size,
      ruleGroups = ruleGroups,
      broadRuleFindings = broadFindings.distinct(),
      seedsTotalClasses = seedClasses.size,
      seedsTotalMembers = seedMembers,
      seedsClassesByPackage = sortedSeedByPkg,
      seedsAppClasses = seedAppClasses.sorted(),
      mappingTotalClasses = totalMappedClasses,
      mappingObfuscatedClasses = obfuscatedClasses,
      mappingUnobfuscatedClasses = unobfuscatedClasses,
      mappingObfuscationPercent = obfPct,
      unobfuscatedAppClasses = unobfuscatedAppClasses.sorted(),
      usageStrippedClasses = strippedClasses,
      usageStrippedMembers = strippedMembers
    )
  }

  private fun packageBucket(className: String): String {
    val parts = className.split(".")
    if (parts.size <= 1) return "<root>"
    return when {
      className.startsWith("com.example.") -> parts.take(3).joinToString(".")
      className.startsWith("com.google.firebase.") -> "com.google.firebase"
      className.startsWith("com.google.mlkit.") -> "com.google.mlkit"
      className.startsWith("com.google.android.") -> parts.take(4).joinToString(".")
      className.startsWith("androidx.") -> parts.take(2).joinToString(".")
      else -> parts.take(minOf(2, parts.size - 1)).joinToString(".")
    }
  }
}
