package com.whispercpp.whisper

import android.util.Log
import java.io.BufferedReader
import java.io.FileReader

object WhisperCpuConfig {
    val preferredThreadCount: Int
        get() {
            val highPerfCount = CpuInfo.getHighPerfCpuCount()
            val totalCores = Runtime.getRuntime().availableProcessors()

            // Detect Snapdragon 750G (SM7225 / Kryo 570)
            val cpuInfo = CpuInfo.getCpuInfoString()
            val is750G = cpuInfo?.contains("SM7225") == true ||
                         cpuInfo?.contains("Snapdragon 750G") == true ||
                         cpuInfo?.contains("Kryo 570") == true

            // For big.LITTLE architectures:
            // - Helio G99: 2x A76 + 6x A55
            // - Snapdragon 750G: 2x A77 + 6x A55
            // Use 2 big + 2 LITTLE for best balance of speed and thermal
            val optimalThreads = when {
                // Snapdragon 750G: 2 big (A77) + 2 little (A55)
                is750G -> {
                    Log.d("WhisperCpuConfig", "Detected Snapdragon 750G")
                    4
                }
                // Helio G99 or similar: 2 big + 2 LITTLE
                highPerfCount == 2 && totalCores >= 6 -> 4
                highPerfCount >= 4 -> highPerfCount
                else -> (totalCores / 2).coerceAtLeast(2)
            }

            Log.d("WhisperCpuConfig", "Using $optimalThreads threads (is750G=$is750G, highPerf=$highPerfCount, total=$totalCores)")
            return optimalThreads
        }
}

private class CpuInfo(private val lines: List<String>) {
    private fun getHighPerfCpuCount(): Int = try {
        getHighPerfCpuCountByFrequencies()
    } catch (e: Exception) {
        Log.d(LOG_TAG, "Couldn't read CPU frequencies", e)
        getHighPerfCpuCountByVariant()
    }

    private fun getHighPerfCpuCountByFrequencies(): Int =
        getCpuValues(property = "processor") { getMaxCpuFrequency(it.toInt()) }
            .also { Log.d(LOG_TAG, "Binned cpu frequencies (frequency, count): ${it.binnedValues()}") }
            .countDroppingMin()

    private fun getHighPerfCpuCountByVariant(): Int =
        getCpuValues(property = "CPU variant") { it.substringAfter("0x").toInt(radix = 16) }
            .also { Log.d(LOG_TAG, "Binned cpu variants (variant, count): ${it.binnedValues()}") }
            .countKeepingMin()

    private fun List<Int>.binnedValues() = groupingBy { it }.eachCount()

    private fun getCpuValues(property: String, mapper: (String) -> Int) = lines
        .asSequence()
        .filter { it.startsWith(property) }
        .map { mapper(it.substringAfter(':').trim()) }
        .sorted()
        .toList()


    private fun List<Int>.countDroppingMin(): Int {
        val min = min()
        return count { it > min }
    }

    private fun List<Int>.countKeepingMin(): Int {
        val min = min()
        return count { it == min }
    }

    companion object {
        private const val LOG_TAG = "WhisperCpuConfig"

        fun getHighPerfCpuCount(): Int = try {
            readCpuInfo().getHighPerfCpuCount()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Couldn't read CPU info", e)
            // Our best guess -- just return the # of CPUs minus 4.
            (Runtime.getRuntime().availableProcessors() - 4).coerceAtLeast(0)
        }

        fun getCpuInfoString(): String? = try {
            BufferedReader(FileReader("/proc/cpuinfo"))
                .useLines { it.joinToString("\n") }
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Couldn't read CPU info string", e)
            null
        }

        private fun readCpuInfo() = CpuInfo(
            BufferedReader(FileReader("/proc/cpuinfo"))
                .useLines { it.toList() }
        )

        private fun getMaxCpuFrequency(cpuIndex: Int): Int {
            val path = "/sys/devices/system/cpu/cpu${cpuIndex}/cpufreq/cpuinfo_max_freq"
            val maxFreq = BufferedReader(FileReader(path)).use { it.readLine() }
            return maxFreq.toInt()
        }
    }
}