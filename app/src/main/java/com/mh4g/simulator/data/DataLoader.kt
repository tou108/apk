package com.mh4g.simulator.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * CSV読み込みユーティリティ（Shift-JIS対応）
 */
object DataLoader {

    private val SJIS = Charset.forName("Windows-31J")

    fun loadSkills(context: Context): Pair<List<Skill>, List<SkillSystem>> {
        val skills = mutableListOf<Skill>()
        val systemMap = linkedMapOf<String, SkillSystem>()
        parseCsv(context, "MH4G_SKILL.csv") { cols ->
            if (cols.size >= 4) {
                val name = cols[0].trim()
                val sysName = cols[1].trim()
                val point = cols[2].trim().toIntOrNull() ?: 0
                val type = cols[3].trim().toIntOrNull() ?: 0
                skills.add(Skill(name, sysName, point, type))
                if (!systemMap.containsKey(sysName)) {
                    systemMap[sysName] = SkillSystem(sysName, type)
                }
            }
        }
        return Pair(skills, systemMap.values.toList())
    }

    fun loadEquipment(context: Context, fileName: String, slotId: Int): List<Equipment> {
        val list = mutableListOf<Equipment>()
        parseCsv(context, fileName) { cols ->
            if (cols.size >= 14) {
                val name = cols[0].trim()
                val gender = cols[1].trim().toIntOrNull() ?: 0
                val type = cols[2].trim().toIntOrNull() ?: 0
                val rare = cols[3].trim().toIntOrNull() ?: 1
                val slotCount = cols[4].trim().toIntOrNull() ?: 0
                val questRank = cols[5].trim().toIntOrNull() ?: 99
                val villageRank = cols[6].trim().toIntOrNull() ?: 99
                val defBase = cols[7].trim().toIntOrNull() ?: 0
                val defMax = cols[8].trim().toIntOrNull() ?: 0
                val resFire = cols[9].trim().toIntOrNull() ?: 0
                val resWater = cols[10].trim().toIntOrNull() ?: 0
                val resThunder = cols[11].trim().toIntOrNull() ?: 0
                val resIce = cols[12].trim().toIntOrNull() ?: 0
                val resDragon = cols[13].trim().toIntOrNull() ?: 0

                val skillPoints = mutableListOf<Pair<String, Int>>()
                for (i in 0..4) {
                    val base = 14 + i * 2
                    if (base + 1 < cols.size) {
                        val sysName = cols[base].trim()
                        val pt = cols[base + 1].trim().toDoubleOrNull()?.toInt()
                        if (sysName.isNotEmpty() && pt != null) {
                            skillPoints.add(Pair(sysName, pt))
                        }
                    }
                }

                val materials = mutableListOf<Pair<String, Int>>()
                for (i in 0..3) {
                    val base = 24 + i * 2
                    if (base + 1 < cols.size) {
                        val matName = cols[base].trim()
                        val cnt = cols[base + 1].trim().toDoubleOrNull()?.toInt()
                        if (matName.isNotEmpty() && cnt != null) {
                            materials.add(Pair(matName, cnt))
                        }
                    }
                }

                list.add(Equipment(name, slotId, gender, type, rare, slotCount,
                    questRank, villageRank, defBase, defMax,
                    resFire, resWater, resThunder, resIce, resDragon,
                    skillPoints, materials))
            }
        }
        return list
    }

    fun loadDecorations(context: Context): List<Decoration> {
        val list = mutableListOf<Decoration>()
        parseCsv(context, "MH4G_DECO.csv") { cols ->
            if (cols.size >= 9) {
                val name = cols[0].trim()
                val rare = cols[1].trim().toIntOrNull() ?: 1
                val slotCost = cols[2].trim().toIntOrNull() ?: 1
                val hrRank = cols[3].trim().toIntOrNull() ?: 1
                val villageRank = cols[4].trim().toIntOrNull() ?: 1
                val skill1 = cols[5].trim()
                val skill1Pt = cols[6].trim().toDoubleOrNull()?.toInt() ?: 0
                val skill2 = if (cols[7].trim().isNotEmpty()) cols[7].trim() else null
                val skill2Pt = cols[8].trim().toDoubleOrNull()?.toInt() ?: 0

                val materials = mutableListOf<Triple<String, Int, Boolean>>()
                // A素材
                for (i in 0..3) {
                    val base = 9 + i * 2
                    if (base + 1 < cols.size) {
                        val matName = cols[base].trim()
                        val cnt = cols[base + 1].trim().toDoubleOrNull()?.toInt()
                        if (matName.isNotEmpty() && cnt != null)
                            materials.add(Triple(matName, cnt, false))
                    }
                }
                // B素材
                for (i in 0..3) {
                    val base = 17 + i * 2
                    if (base + 1 < cols.size) {
                        val matName = cols[base].trim()
                        val cnt = cols[base + 1].trim().toDoubleOrNull()?.toInt()
                        if (matName.isNotEmpty() && cnt != null)
                            materials.add(Triple(matName, cnt, true))
                    }
                }

                list.add(Decoration(name, rare, slotCost, hrRank, villageRank,
                    skill1, skill1Pt, skill2, skill2Pt, materials))
            }
        }
        return list
    }

    fun loadCharmTypes(context: Context): List<Triple<String, Int, Int>> {
        // (typeName, rare, questRank)
        val list = mutableListOf<Triple<String, Int, Int>>()
        parseCsv(context, "MH4G_CHARM.csv") { cols ->
            if (cols.size >= 3) {
                val name = cols[0].trim()
                val rare = cols[1].trim().toIntOrNull() ?: 1
                val qr = cols[2].trim().toIntOrNull() ?: 1
                list.add(Triple(name, rare, qr))
            }
        }
        return list
    }

    private fun parseCsv(context: Context, fileName: String, onRow: (List<String>) -> Unit) {
        try {
            context.assets.open(fileName).use { stream ->
                BufferedReader(InputStreamReader(stream, SJIS)).use { reader ->
                    var firstLine = true
                    reader.forEachLine { line ->
                        if (firstLine) {
                            firstLine = false
                            return@forEachLine
                        }
                        if (line.isBlank()) return@forEachLine
                        onRow(splitCsvLine(line))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        for (c in line) {
            when {
                c == '"' -> inQuote = !inQuote
                c == ',' && !inQuote -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString())
        return result
    }
}
