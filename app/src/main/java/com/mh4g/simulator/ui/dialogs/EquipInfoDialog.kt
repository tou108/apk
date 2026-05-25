package com.mh4g.simulator.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.mh4g.simulator.data.*

/**
 * 装備情報ダイアログ（PC版 装備情報ポップアップ相当）
 */
class EquipInfoDialog(private val equip: Equipment) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)

        val table = TableLayout(requireContext()).apply {
            setPadding(16, 8, 16, 8)
            isStretchAllColumns = true
        }

        fun addRow(vararg cols: Pair<String, Int>) {
            val row = TableRow(requireContext())
            for ((text, color) in cols) {
                val tv = TextView(requireContext()).apply {
                    this.text = text
                    textSize = 12f
                    setPadding(4, 4, 4, 4)
                    setTextColor(color)
                }
                row.addView(tv)
            }
            table.addView(row)
        }

        fun addHeader(vararg labels: String) {
            val row = TableRow(requireContext())
            row.setBackgroundColor(Color.parseColor("#3F3F3F"))
            for (label in labels) {
                val tv = TextView(requireContext()).apply {
                    text = label
                    textSize = 11f
                    setPadding(4, 4, 4, 4)
                    setTextColor(Color.WHITE)
                }
                row.addView(tv)
            }
            table.addView(row)
        }

        fun addDivider() {
            val row = TableRow(requireContext())
            row.setBackgroundColor(Color.parseColor("#DDDDDD"))
            val v = View(requireContext()).apply { minimumHeight = 1 }
            row.addView(v)
            table.addView(row)
        }

        val black = Color.BLACK
        val gray  = Color.GRAY

        // 名前・スロット
        addHeader(equip.name)
        addRow(
            "スロット" to black,
            equip.slotStr() to black,
            "部位" to black,
            equip.slotName() to black
        )
        addRow(
            "レア" to gray,
            "R${equip.rare}" to black,
            "性別" to gray,
            when(equip.gender){ 1->"男"; 2->"女"; else->"両" } to black
        )
        addRow(
            "タイプ" to gray,
            when(equip.type){ 1->"剣士"; 2->"ガンナー"; else->"両" } to black,
            "集☆" to gray,
            if(equip.questRank==99) "不可" to gray else "${equip.questRank}☆" to black
        )
        addRow(
            "防御" to gray,
            "${equip.defBase}→${equip.defMax}" to black,
            "村☆" to gray,
            if(equip.villageRank==99) "不可" to gray else "${equip.villageRank}☆" to black
        )

        addDivider()

        // 耐性
        addHeader("火", "水", "雷", "氷", "龍")
        addRow(
            "${equip.resFire}"    to if(equip.resFire    >0) Color.RED    else if(equip.resFire    <0) Color.BLUE  else black,
            "${equip.resWater}"   to if(equip.resWater   >0) Color.RED    else if(equip.resWater   <0) Color.BLUE  else black,
            "${equip.resThunder}" to if(equip.resThunder >0) Color.RED    else if(equip.resThunder <0) Color.BLUE  else black,
            "${equip.resIce}"     to if(equip.resIce     >0) Color.RED    else if(equip.resIce     <0) Color.BLUE  else black,
            "${equip.resDragon}"  to if(equip.resDragon  >0) Color.RED    else if(equip.resDragon  <0) Color.BLUE  else black
        )

        addDivider()

        // スキルポイント
        addHeader("スキル系統", "ポイント")
        for ((sys, pt) in equip.skillPoints) {
            val ptColor = if (pt > 0) Color.parseColor("#1565C0") else Color.parseColor("#B71C1C")
            addRow(sys to black, (if (pt >= 0) "+$pt" else "$pt") to ptColor)
        }
        if (equip.skillPoints.isEmpty()) {
            addRow("（スキルなし）" to gray, "" to gray)
        }

        // 素材
        if (equip.materials.isNotEmpty()) {
            addDivider()
            addHeader("素材", "個数")
            for ((mat, cnt) in equip.materials) {
                addRow(mat to black, "×$cnt" to black)
            }
        }

        val scroll = ScrollView(requireContext()).apply {
            addView(table)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("装備情報：${equip.name}")
            .setView(scroll)
            .setPositiveButton("閉じる", null)
            .create()
    }

    companion object {
        fun show(fragment: androidx.fragment.app.Fragment, equip: Equipment) {
            EquipInfoDialog(equip).show(fragment.parentFragmentManager, "EquipInfo")
        }
    }
}
