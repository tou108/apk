package com.mh4g.simulator.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.mh4g.simulator.data.*
import com.mh4g.simulator.search.SearchResult

/**
 * スキルポイント詳細テーブルダイアログ（PC版 スキルポイント表示テーブル相当）
 */
class SkillPointTableDialog(private val result: SearchResult) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val table = TableLayout(requireContext()).apply {
            setPadding(8, 8, 8, 8)
            isStretchAllColumns = true
        }

        // ヘッダー
        val headerRow = TableRow(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#3F3F3F"))
        }
        for (label in listOf("系統", "頭", "胴", "腕", "腰", "脚", "護石", "装飾", "合計", "発動")) {
            headerRow.addView(TextView(requireContext()).apply {
                text = label
                textSize = 10f
                setPadding(3, 4, 3, 4)
                setTextColor(Color.WHITE)
            })
        }
        table.addView(headerRow)

        // 全スキル系統を収集
        val allSystems = mutableSetOf<String>()
        listOf(result.head, result.body, result.arm, result.wst, result.leg)
            .filterNotNull()
            .forEach { e -> e.skillPoints.forEach { (s, _) -> allSystems.add(s) } }
        result.charm?.skillPoints?.forEach { (s, _) -> allSystems.add(s) }
        result.decos.filterNotNull().forEach { d ->
            allSystems.add(d.skill1)
            d.skill2?.let { allSystems.add(it) }
        }

        // 合計ポイントマップ
        val totalPts = mutableMapOf<String, Int>()
        fun addPts(equip: Equipment?) = equip?.skillPoints?.forEach { (s, p) ->
            totalPts[s] = (totalPts[s] ?: 0) + p
        }
        addPts(result.head); addPts(result.body); addPts(result.arm)
        addPts(result.wst);   addPts(result.leg)
        result.charm?.skillPoints?.forEach { (s, p) -> totalPts[s] = (totalPts[s] ?: 0) + p }
        result.decos.filterNotNull().forEach { d ->
            totalPts[d.skill1] = (totalPts[d.skill1] ?: 0) + d.skill1Point
            d.skill2?.let { s2 -> totalPts[s2] = (totalPts[s2] ?: 0) + d.skill2Point }
        }

        // 発動スキルマップ
        val activatedSkillNames = result.activatedSkills.map { it.skillName }.toSet()

        var rowIdx = 0
        for (sysName in allSystems.sortedBy { it }) {
            val row = TableRow(requireContext()).apply {
                setBackgroundColor(if (rowIdx % 2 == 0) Color.WHITE else Color.parseColor("#F0F0F0"))
            }
            rowIdx++

            fun pt(equip: Equipment?) = equip?.getSkillPoint(sysName) ?: 0
            fun charmPt() = result.charm?.getSkillPoint(sysName) ?: 0
            fun decoPt(): Int {
                var sum = 0
                result.decos.filterNotNull().forEach { d ->
                    sum += when (sysName) {
                        d.skill1 -> d.skill1Point
                        d.skill2 -> d.skill2Point
                        else -> 0
                    }
                }
                return sum
            }

            val total = totalPts[sysName] ?: 0

            // 発動スキルを調べる
            val sysSkills = AppData.skillsBySystem[sysName] ?: emptyList()
            val activated = sysSkills.filter { sk ->
                if (sk.point > 0) total >= sk.point else total <= sk.point
            }
            val activatedText = activated.joinToString(",") { it.name }
            val isTarget = activated.any { it.name in activatedSkillNames }
            val isMinus = activated.any { it.point < 0 }

            fun ptCell(value: Int) = TextView(requireContext()).apply {
                text = if (value == 0) "-" else if (value > 0) "+$value" else "$value"
                textSize = 10f
                setPadding(3, 4, 3, 4)
                setTextColor(when {
                    value > 0 -> Color.parseColor("#1565C0")
                    value < 0 -> Color.parseColor("#B71C1C")
                    else      -> Color.GRAY
                })
                gravity = android.view.Gravity.CENTER
            }

            // 系統名
            row.addView(TextView(requireContext()).apply {
                text = sysName
                textSize = 9f
                setPadding(3, 4, 3, 4)
                setTextColor(Color.BLACK)
            })
            // 各部位
            row.addView(ptCell(pt(result.head)))
            row.addView(ptCell(pt(result.body)))
            row.addView(ptCell(pt(result.arm)))
            row.addView(ptCell(pt(result.wst)))
            row.addView(ptCell(pt(result.leg)))
            row.addView(ptCell(charmPt()))
            row.addView(ptCell(decoPt()))
            // 合計
            row.addView(TextView(requireContext()).apply {
                text = if (total >= 0) "+$total" else "$total"
                textSize = 10f
                setPadding(3, 4, 3, 4)
                setTextColor(when {
                    isTarget -> Color.parseColor("#1565C0")
                    isMinus  -> Color.parseColor("#B71C1C")
                    else     -> Color.BLACK
                })
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
            })
            // 発動スキル
            row.addView(TextView(requireContext()).apply {
                text = activatedText
                textSize = 9f
                setPadding(3, 4, 3, 4)
                setTextColor(when {
                    isTarget -> Color.parseColor("#0D47A1")
                    isMinus  -> Color.parseColor("#B71C1C")
                    else     -> Color.DKGRAY
                })
            })

            table.addView(row)
        }

        val scroll = ScrollView(requireContext()).apply {
            val hScroll = HorizontalScrollView(requireContext())
            hScroll.addView(table)
            addView(hScroll)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("スキルポイント詳細")
            .setView(scroll)
            .setPositiveButton("閉じる", null)
            .create()
    }

    companion object {
        fun show(fragment: androidx.fragment.app.Fragment, result: SearchResult) {
            SkillPointTableDialog(result).show(fragment.parentFragmentManager, "SkillPtTable")
        }
    }
}
