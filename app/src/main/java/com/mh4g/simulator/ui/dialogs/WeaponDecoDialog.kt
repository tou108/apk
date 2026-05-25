package com.mh4g.simulator.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.mh4g.simulator.R
import com.mh4g.simulator.data.*
import com.mh4g.simulator.search.DecoSpec

/**
 * 武器スロット数と装飾品直接指定ダイアログ（PC版 ui.h.D相当）
 */
class WeaponDecoDialog(
    private val currentWeaponSlot: Int,
    private val currentSpecs: List<DecoSpec>,
    private val onApply: (weaponSlot: Int, specs: List<DecoSpec>) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_weapon_deco, null)

        val spinnerSlot  = view.findViewById<Spinner>(R.id.spinnerWeaponSlot)
        val decoContainer = view.findViewById<LinearLayout>(R.id.decoContainer)

        // 武器スロット数スピナー
        val slotOptions = arrayOf("自動", "0", "1", "2", "3")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, slotOptions).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSlot.adapter = a
            spinnerSlot.setSelection(if (currentWeaponSlot < 0) 0 else currentWeaponSlot + 1)
        }

        // 装飾品指定行（最大3行）
        val decoSpinners = mutableListOf<Spinner>()
        val decoNames = arrayOf("(指定なし)") +
            AppData.decoList.sortedBy { it.name }.map { it.name }.toTypedArray()

        for (i in 0..2) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val label = TextView(requireContext()).apply {
                text = "装飾品${i + 1}:"
                textSize = 12f
                setPadding(0, 0, 6, 0)
                gravity = Gravity.CENTER_VERTICAL
            }
            val spinner = Spinner(requireContext())
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, decoNames).also { a ->
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = a
            }
            // 既存指定を復元
            val existingDeco = currentSpecs.getOrNull(i)?.deco
            if (existingDeco != null) {
                val idx = decoNames.indexOfFirst { it == existingDeco.name }
                if (idx >= 0) spinner.setSelection(idx)
            }

            row.addView(label)
            row.addView(spinner, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            decoContainer.addView(row)
            decoSpinners.add(spinner)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("武器スロット・装飾品直接指定")
            .setView(view)
            .setPositiveButton("適用") { _, _ ->
                val selSlot = spinnerSlot.selectedItemPosition
                val weapSlot = if (selSlot == 0) -1 else selSlot - 1

                val specs = decoSpinners.mapIndexedNotNull { i, sp ->
                    if (sp.selectedItemPosition == 0) null
                    else {
                        val decoName = decoNames[sp.selectedItemPosition]
                        val deco = AppData.decoList.firstOrNull { it.name == decoName }
                        DecoSpec(deco, i)
                    }
                }
                onApply(weapSlot, specs)
            }
            .setNeutralButton("クリア") { _, _ ->
                onApply(-1, emptyList())
            }
            .setNegativeButton("キャンセル", null)
            .create()
    }
}
