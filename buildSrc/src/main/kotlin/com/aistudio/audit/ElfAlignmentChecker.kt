package com.aistudio.audit

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ElfAlignmentChecker {
  private const val PT_LOAD = 1
  private const val MIN_16KB_ALIGN = 16384L

  fun inspectElf(entryPath: String, bytes: ByteArray): ElfBinaryAudit {
    if (bytes.size < 64) {
      throw IllegalArgumentException("ELF binary $entryPath is too small (${bytes.size} bytes)")
    }
    if (bytes[0] != 0x7F.toByte() || bytes[1] != 'E'.code.toByte() ||
      bytes[2] != 'L'.code.toByte() || bytes[3] != 'F'.code.toByte()
    ) {
      throw IllegalArgumentException("Invalid ELF magic in $entryPath")
    }

    val eiClass = bytes[4].toInt() and 0xFF // 1 = ELF32, 2 = ELF64
    val eiData = bytes[5].toInt() and 0xFF  // 1 = LE, 2 = BE
    val order = if (eiData == 2) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
    val buf = ByteBuffer.wrap(bytes).order(order)

    val bitness: Int
    val ptLoadAlignments = mutableListOf<Long>()

    when (eiClass) {
      2 -> { // ELF64
        bitness = 64
        val ePhoff = buf.getLong(32).toInt()
        val ePhentsize = buf.getShort(54).toInt() and 0xFFFF
        val ePhnum = buf.getShort(56).toInt() and 0xFFFF
        for (i in 0 until ePhnum) {
          val off = ePhoff + i * ePhentsize
          if (off + 56 > bytes.size) break
          val pType = buf.getInt(off)
          val pAlign = buf.getLong(off + 48)
          if (pType == PT_LOAD) {
            ptLoadAlignments.add(pAlign)
          }
        }
      }
      1 -> { // ELF32
        bitness = 32
        val ePhoff = buf.getInt(28)
        val ePhentsize = buf.getShort(42).toInt() and 0xFFFF
        val ePhnum = buf.getShort(44).toInt() and 0xFFFF
        for (i in 0 until ePhnum) {
          val off = ePhoff + i * ePhentsize
          if (off + 32 > bytes.size) break
          val pType = buf.getInt(off)
          val pAlign = buf.getInt(off + 28).toLong() and 0xFFFFFFFFL
          if (pType == PT_LOAD) {
            ptLoadAlignments.add(pAlign)
          }
        }
      }
      else -> throw IllegalArgumentException("Unsupported ELF class $eiClass in $entryPath")
    }

    val pathParts = entryPath.split("/")
    val libIdx = pathParts.indexOfLast { it == "lib" }
    val abi = if (libIdx >= 0 && libIdx + 1 < pathParts.size) pathParts[libIdx + 1] else "unknown"
    val libraryName = pathParts.last()
    val aligned16Kb = ptLoadAlignments.isNotEmpty() && ptLoadAlignments.all { it >= MIN_16KB_ALIGN }

    return ElfBinaryAudit(
      entryPath = entryPath,
      abi = abi,
      libraryName = libraryName,
      bitness = bitness,
      ptLoadAlignments = ptLoadAlignments,
      is16KbAligned = aligned16Kb
    )
  }
}
