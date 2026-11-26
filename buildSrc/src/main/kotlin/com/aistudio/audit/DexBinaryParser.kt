package com.aistudio.audit

import java.nio.ByteBuffer
import java.nio.ByteOrder

object DexBinaryParser {
  private const val ENDIAN_CONSTANT = 0x12345678

  // Standard Dalvik opcode instruction widths in 16-bit code units (0x00..0xFF)
  private val OPCODE_WIDTH_UNITS = intArrayOf(
    1, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 1, 1, 1, 1, 1, // 0x00..0x0f
    1, 1, 1, 2, 3, 2, 2, 3, 5, 2, 2, 3, 2, 1, 1, 2, // 0x10..0x1f
    2, 1, 2, 2, 3, 3, 3, 1, 1, 2, 3, 3, 3, 2, 2, 2, // 0x20..0x2f
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, // 0x30..0x3f
    1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0x40..0x4f
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0x50..0x5f
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, // 0x60..0x6f
    3, 3, 3, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, // 0x70..0x7f
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0x80..0x8f
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0x90..0x9f
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0xa0..0xaf
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0xb0..0xbf
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0xc0..0xcf
    2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, // 0xd0..0xdf
    2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0xe0..0xef
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 4, 3, 3, 2, 2  // 0xf0..0xff
  )

  fun auditDexFiles(
    dexEntries: Map<String, ByteArray>,
    baseline: ReleaseArtifactBaseline
  ): DexAuditResult {
    val summaries = mutableListOf<DexFileSummary>()
    val forbiddenMethodIdHits = linkedSetOf<String>()
    val forbiddenInvokeHits = mutableListOf<DexInvokeHit>()
    val forbiddenTypeHits = linkedSetOf<String>()
    val forbiddenStringHits = linkedSetOf<String>()
    val cleartextUrlHits = linkedSetOf<String>()
    val classesByPackage = linkedMapOf<String, Int>()
    val methodsByPackage = linkedMapOf<String, Int>()

    val forbiddenStringAll = baseline.forbiddenStringTokens + baseline.forbiddenDexOnlyStringTokens

    for ((entryName, bytes) in dexEntries.toSortedMap()) {
      val parsed = parseSingleDex(
        entryName = entryName,
        bytes = bytes,
        baseline = baseline,
        forbiddenStringAll = forbiddenStringAll,
        forbiddenMethodIdHits = forbiddenMethodIdHits,
        forbiddenInvokeHits = forbiddenInvokeHits,
        forbiddenTypeHits = forbiddenTypeHits,
        forbiddenStringHits = forbiddenStringHits,
        cleartextUrlHits = cleartextUrlHits,
        classesByPackage = classesByPackage,
        methodsByPackage = methodsByPackage
      )
      summaries.add(parsed)
    }

    val sortedClasses = classesByPackage.entries
      .sortedByDescending { it.value }
      .associate { it.key to it.value }
    val sortedMethods = methodsByPackage.entries
      .sortedByDescending { it.value }
      .associate { it.key to it.value }

    return DexAuditResult(
      dexFiles = summaries,
      forbiddenMethodIdHits = forbiddenMethodIdHits.sorted(),
      forbiddenInvokeHits = forbiddenInvokeHits.distinct().sortedBy { "${it.callerClass} -> ${it.targetMethod}" },
      forbiddenTypeHits = forbiddenTypeHits.sorted(),
      forbiddenStringHits = forbiddenStringHits.sorted(),
      cleartextUrlHits = cleartextUrlHits.sorted(),
      survivingClassesByPackage = sortedClasses,
      survivingMethodsByPackage = sortedMethods
    )
  }

  private fun parseSingleDex(
    entryName: String,
    bytes: ByteArray,
    baseline: ReleaseArtifactBaseline,
    forbiddenStringAll: List<String>,
    forbiddenMethodIdHits: MutableSet<String>,
    forbiddenInvokeHits: MutableList<DexInvokeHit>,
    forbiddenTypeHits: MutableSet<String>,
    forbiddenStringHits: MutableSet<String>,
    cleartextUrlHits: MutableSet<String>,
    classesByPackage: MutableMap<String, Int>,
    methodsByPackage: MutableMap<String, Int>
  ): DexFileSummary {
    if (bytes.size < 112) {
      throw IllegalArgumentException("DEX file $entryName is truncated (${bytes.size} bytes)")
    }
    val magic = String(bytes, 0, 8, Charsets.US_ASCII)
    if (!magic.startsWith("dex\n")) {
      throw IllegalArgumentException("Invalid DEX magic in $entryName: $magic")
    }
    val version = magic.substring(4, 7)

    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val endianTag = buf.getInt(40)
    if (endianTag != ENDIAN_CONSTANT) {
      throw IllegalArgumentException("Unsupported DEX endian tag 0x${Integer.toHexString(endianTag)} in $entryName")
    }

    val stringIdsSize = buf.getInt(56)
    val stringIdsOff = buf.getInt(60)
    val typeIdsSize = buf.getInt(64)
    val typeIdsOff = buf.getInt(68)
    val protoIdsSize = buf.getInt(72)
    val protoIdsOff = buf.getInt(76)
    val methodIdsSize = buf.getInt(88)
    val methodIdsOff = buf.getInt(92)
    val classDefsSize = buf.getInt(96)
    val classDefsOff = buf.getInt(100)

    // 1. Parse string_ids table (structured MUTF-8 pool)
    val strings = Array(stringIdsSize) { idx ->
      val dataOff = buf.getInt(stringIdsOff + idx * 4)
      decodeMutf8At(bytes, dataOff)
    }

    for (str in strings) {
      for (token in forbiddenStringAll) {
        if (str.contains(token)) {
          forbiddenStringHits.add("$entryName: matched [$token] in string \"${str.take(120)}\"")
        }
      }
      if (str.startsWith("http://", ignoreCase = true) &&
        baseline.allowedCleartextUrlPrefixes.none { str.startsWith(it, ignoreCase = true) }
      ) {
        cleartextUrlHits.add("$entryName: ${str.take(140)}")
      }
    }

    // 2. Parse type_ids table
    val types = Array(typeIdsSize) { idx ->
      val descIdx = buf.getInt(typeIdsOff + idx * 4)
      strings[descIdx]
    }

    for (typeDesc in types) {
      for (sub in baseline.forbiddenDexTypeSubstrings) {
        if (typeDesc.contains(sub)) {
          forbiddenTypeHits.add("$entryName: $typeDesc")
        }
      }
    }

    // 3. Parse proto_ids table
    val protos = Array(protoIdsSize) { idx ->
      val off = protoIdsOff + idx * 12
      val retTypeIdx = buf.getInt(off + 4)
      val paramsOff = buf.getInt(off + 8)
      val sb = StringBuilder("(")
      if (paramsOff != 0) {
        val paramCount = buf.getInt(paramsOff)
        for (p in 0 until paramCount) {
          val pTypeIdx = buf.getShort(paramsOff + 4 + p * 2).toInt() and 0xFFFF
          sb.append(types[pTypeIdx])
        }
      }
      sb.append(")").append(types[retTypeIdx])
      sb.toString()
    }

    // 4. Parse method_ids table and identify forbidden method indices
    val methods = Array(methodIdsSize) { idx ->
      val off = methodIdsOff + idx * 8
      val classIdx = buf.getShort(off).toInt() and 0xFFFF
      val protoIdx = buf.getShort(off + 2).toInt() and 0xFFFF
      val nameIdx = buf.getInt(off + 4)
      DexMethodRef(types[classIdx], strings[nameIdx], protos[protoIdx])
    }

    val forbiddenMethodIndices = HashSet<Int>()
    for (idx in methods.indices) {
      val ref = methods[idx]
      if (matchesForbiddenMethod(ref, baseline.forbiddenDexMethodRefs)) {
        forbiddenMethodIndices.add(idx)
        forbiddenMethodIdHits.add("$entryName: ${ref.fullDescriptor}")
      }
    }

    // 5. Walk class_defs -> class_data_item -> code_item bytecode instructions
    var totalDefinedMethods = 0
    var scannedInvokes = 0

    for (i in 0 until classDefsSize) {
      val off = classDefsOff + i * 32
      val classIdx = buf.getInt(off)
      val classDataOff = buf.getInt(off + 24)
      val classDesc = types[classIdx]
      val pkgBucket = classifyPackageBucket(classDesc)
      classesByPackage[pkgBucket] = (classesByPackage[pkgBucket] ?: 0) + 1

      if (classDataOff == 0) continue

      val cursor = IntArray(1) { classDataOff }
      val staticFieldsSize = readUleb128(bytes, cursor)
      val instanceFieldsSize = readUleb128(bytes, cursor)
      val directMethodsSize = readUleb128(bytes, cursor)
      val virtualMethodsSize = readUleb128(bytes, cursor)

      // Skip encoded_field entries (2 uleb128s each)
      repeat(staticFieldsSize + instanceFieldsSize) {
        readUleb128(bytes, cursor)
        readUleb128(bytes, cursor)
      }

      val classMethodCount = directMethodsSize + virtualMethodsSize
      totalDefinedMethods += classMethodCount
      methodsByPackage[pkgBucket] = (methodsByPackage[pkgBucket] ?: 0) + classMethodCount

      // Walk direct_methods and virtual_methods
      repeat(2) { pass ->
        val count = if (pass == 0) directMethodsSize else virtualMethodsSize
        repeat(count) {
          readUleb128(bytes, cursor) // method_idx_diff
          readUleb128(bytes, cursor) // access_flags
          val codeOff = readUleb128(bytes, cursor)
          if (codeOff != 0) {
            scannedInvokes += scanCodeItemInvokes(
              buf = buf,
              codeOff = codeOff,
              callerClass = classDesc,
              methods = methods,
              forbiddenMethodIndices = forbiddenMethodIndices,
              forbiddenInvokeHits = forbiddenInvokeHits
            )
          }
        }
      }
    }

    return DexFileSummary(
      entryName = entryName,
      sizeBytes = bytes.size.toLong(),
      dexVersion = version,
      stringCount = stringIdsSize,
      typeCount = typeIdsSize,
      methodIdCount = methodIdsSize,
      classDefCount = classDefsSize,
      definedMethodCount = totalDefinedMethods,
      scannedInvokeCount = scannedInvokes
    )
  }

  private fun scanCodeItemInvokes(
    buf: ByteBuffer,
    codeOff: Int,
    callerClass: String,
    methods: Array<DexMethodRef>,
    forbiddenMethodIndices: Set<Int>,
    forbiddenInvokeHits: MutableList<DexInvokeHit>
  ): Int {
    val insnsSize = buf.getInt(codeOff + 12)
    val insnsStart = codeOff + 16
    var pc = 0
    var invokeCount = 0

    while (pc < insnsSize) {
      val unit0 = buf.getShort(insnsStart + pc * 2).toInt() and 0xFFFF
      val opcode = unit0 and 0xFF

      // Handle pseudo-instruction payloads (NOP with non-zero high byte)
      if (opcode == 0x00 && unit0 != 0x0000) {
        when (unit0) {
          0x0100 -> { // packed-switch-payload
            val size = buf.getShort(insnsStart + (pc + 1) * 2).toInt() and 0xFFFF
            pc += (size * 2) + 4
          }
          0x0200 -> { // sparse-switch-payload
            val size = buf.getShort(insnsStart + (pc + 1) * 2).toInt() and 0xFFFF
            pc += (size * 4) + 2
          }
          0x0300 -> { // fill-array-data-payload
            val elemWidth = buf.getShort(insnsStart + (pc + 1) * 2).toInt() and 0xFFFF
            val size = buf.getInt(insnsStart + (pc + 2) * 2)
            pc += ((size * elemWidth + 1) / 2) + 4
          }
          else -> pc += 1
        }
        continue
      }

      val isInvoke = (opcode in 0x6e..0x72) || (opcode in 0x74..0x78) || opcode == 0xfa || opcode == 0xfb
      if (isInvoke && pc + 1 < insnsSize) {
        invokeCount++
        val methodIdx = buf.getShort(insnsStart + (pc + 1) * 2).toInt() and 0xFFFF
        if (methodIdx in forbiddenMethodIndices && methodIdx < methods.size) {
          forbiddenInvokeHits.add(
            DexInvokeHit(
              callerClass = callerClass,
              targetMethod = methods[methodIdx].fullDescriptor
            )
          )
        }
      }

      val width = OPCODE_WIDTH_UNITS[opcode]
      pc += if (width > 0) width else 1
    }
    return invokeCount
  }

  private fun matchesForbiddenMethod(ref: DexMethodRef, patterns: List<String>): Boolean {
    return patterns.any { pat ->
      if (pat.contains("(")) {
        ref.fullDescriptor == pat
      } else {
        ref.shortDescriptor == pat
      }
    }
  }

  private fun classifyPackageBucket(classDesc: String): String {
    if (!classDesc.startsWith("L") || !classDesc.endsWith(";")) return "<primitive/array>"
    val internal = classDesc.substring(1, classDesc.length - 1)
    val parts = internal.split("/")
    if (parts.size <= 1) return "<obfuscated/root>"
    return when {
      internal.startsWith("com/example/") -> parts.take(3).joinToString(".")
      internal.startsWith("com/google/firebase/") -> "com.google.firebase"
      internal.startsWith("com/google/mlkit/") -> "com.google.mlkit"
      internal.startsWith("com/google/android/datatransport/") -> "com.google.android.datatransport"
      internal.startsWith("com/google/android/") -> "com.google.android"
      internal.startsWith("androidx/") -> parts.take(2).joinToString(".")
      internal.startsWith("okhttp3/") -> "okhttp3"
      internal.startsWith("retrofit2/") -> "retrofit2"
      internal.startsWith("kotlinx/") -> parts.take(2).joinToString(".")
      internal.startsWith("kotlin/") -> "kotlin"
      else -> parts.take(minOf(2, parts.size - 1)).joinToString(".")
    }
  }

  private fun readUleb128(data: ByteArray, cursor: IntArray): Int {
    var result = 0
    var shift = 0
    var pos = cursor[0]
    while (true) {
      val b = data[pos++].toInt() and 0xFF
      result = result or ((b and 0x7F) shl shift)
      if ((b and 0x80) == 0) break
      shift += 7
    }
    cursor[0] = pos
    return result
  }

  private fun decodeMutf8At(data: ByteArray, offset: Int): String {
    val cursor = IntArray(1) { offset }
    val utf16Len = readUleb128(data, cursor)
    var pos = cursor[0]
    val out = CharArray(utf16Len)
    var outIdx = 0
    while (outIdx < utf16Len && pos < data.size) {
      val a = data[pos++].toInt() and 0xFF
      if (a == 0) break
      if (a < 0x80) {
        out[outIdx++] = a.toChar()
      } else if ((a and 0xE0) == 0xC0 && pos < data.size) {
        val b = data[pos++].toInt() and 0xFF
        out[outIdx++] = (((a and 0x1F) shl 6) or (b and 0x3F)).toChar()
      } else if ((a and 0xF0) == 0xE0 && pos + 1 < data.size) {
        val b = data[pos++].toInt() and 0xFF
        val c = data[pos++].toInt() and 0xFF
        out[outIdx++] = (((a and 0x0F) shl 12) or ((b and 0x3F) shl 6) or (c and 0x3F)).toChar()
      } else {
        out[outIdx++] = '?'
      }
    }
    return String(out, 0, outIdx)
  }
}
