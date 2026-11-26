package com.aistudio.audit

import com.android.aapt.Resources

object ResourceTableParser {
  fun auditResourceTable(
    resourcesPbBytes: ByteArray,
    baseline: ReleaseArtifactBaseline
  ): ResourceTableAuditResult {
    val table = Resources.ResourceTable.parseFrom(resourcesPbBytes)
    var totalStrings = 0
    var totalXmlFiles = 0
    val forbiddenHits = linkedSetOf<String>()
    val cleartextUrlHits = linkedSetOf<String>()

    for (pkg in table.packageList) {
      for (type in pkg.typeList) {
        for (entry in type.entryList) {
          for (cv in entry.configValueList) {
            if (!cv.hasValue() || !cv.value.hasItem()) continue
            val item = cv.value.item
            val resId = "${pkg.packageName}:${type.name}/${entry.name}"

            if (item.hasFile() && item.file.path.endsWith(".xml")) {
              totalXmlFiles++
            }

            val strVal: String? = when {
              item.hasStr() -> item.str.value
              item.hasRawStr() -> item.rawStr.value
              item.hasStyledStr() -> item.styledStr.value
              else -> null
            }

            if (strVal != null) {
              totalStrings++
              for (token in baseline.forbiddenStringTokens) {
                if (strVal.contains(token)) {
                  forbiddenHits.add("$resId matched [$token]: \"${strVal.take(120)}\"")
                }
              }
              if (strVal.startsWith("http://", ignoreCase = true) &&
                baseline.allowedCleartextUrlPrefixes.none { strVal.startsWith(it, ignoreCase = true) }
              ) {
                cleartextUrlHits.add("$resId -> ${strVal.take(140)}")
              }
            }
          }
        }
      }
    }

    return ResourceTableAuditResult(
      totalStringEntries = totalStrings,
      totalXmlFileEntries = totalXmlFiles,
      forbiddenResourceStringHits = forbiddenHits.sorted(),
      cleartextResourceUrlHits = cleartextUrlHits.sorted()
    )
  }
}
