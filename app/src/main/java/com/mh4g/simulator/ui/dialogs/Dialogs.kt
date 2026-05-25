package com.mh4g.simulator.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.mh4g.simulator.R
import com.mh4g.simulator.data.*
import com.mh4g.simulator.search.SearchResult

// ============================================================
// お守り追加ダイアログ
// ============================================================
class AddCharmDialog(private val onAdd: (Charm) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_charm, null)

        val spinnerType = view.findViewById<Spinner>(R.id.spinnerCharmType)
        val spinnerSlot = view.findViewById<Spinner>(R.id.spinnerCharmSlot)
        val skill1System = view.findViewById<Spinner>(R.id.spinnerSkill1System)
        val skill1Pt = view.findViewById<EditText>(R.id.etSkill1Point)
        val skill2System = view.findViewById<Spinner>(R.id.spinnerSkill2System)
        val skill2Pt = view.findViewById<EditText>(R.id.etSkill2Point)

        // お守り種類
        val typeNames = AppData.charmTypes.map { it.first }.toTypedArray()
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeNames).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = a
        }

        // スロット数
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            arrayOf("0", "1", "2", "3")).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSlot.adapter = a
        }

        // スキル系統
        val sysNames = arrayOf("(なし)") + AppData.skillSystems.map { it.name }.toTypedArray()
        for (sp in listOf(skill1System, skill2System)) {
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sysNames).also { a ->
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                sp.adapter = a
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("お守り登録")
            .setView(view)
            .setPositiveButton("登録") { _, _ ->
                val typeName = typeNames.getOrNull(spinnerType.selectedItemPosition) ?: ""
                val slotCount = spinnerSlot.selectedItemPosition
                val charmType = AppData.charmTypes.getOrNull(spinnerType.selectedItemPosition)

                val skillPoints = mutableListOf<Pair<String, Int>>()
                if (skill1System.selectedItemPosition > 0) {
                    val sys = sysNames[skill1System.selectedItemPosition]
                    val pt = skill1Pt.text.toString().toIntOrNull() ?: 0
                    if (pt != 0) skillPoints.add(Pair(sys, pt))
                }
                if (skill2System.selectedItemPosition > 0) {
                    val sys = sysNames[skill2System.selectedItemPosition]
                    val pt = skill2Pt.text.toString().toIntOrNull() ?: 0
                    if (pt != 0) skillPoints.add(Pair(sys, pt))
                }

                onAdd(Charm(
                    id = 0,
                    typeName = typeName,
                    rare = charmType?.second ?: 1,
                    questRank = charmType?.third ?: 1,
                    slotCount = slotCount,
                    skillPoints = skillPoints
                ))
            }
            .setNegativeButton("キャンセル", null)
            .create()
    }
}

// ============================================================
// マイセット保存ダイアログ
// ============================================================
class SaveMySetDialog(
    private val result: SearchResult,
    private val onSave: (label: String, note: String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_myset, null)
        val etLabel = view.findViewById<EditText>(R.id.etLabel)
        val etNote = view.findViewById<EditText>(R.id.etNote)

        // デフォルトラベル：発動スキル
        val defaultLabel = result.activatedSkills
            .filter { it.isTarget }
            .joinToString(",") { it.skillName }
        etLabel.setText(defaultLabel)

        return AlertDialog.Builder(requireContext())
            .setTitle("マイセットに追加")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                onSave(etLabel.text.toString(), etNote.text.toString())
            }
            .setNegativeButton("キャンセル", null)
            .create()
    }
}
