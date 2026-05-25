package com.mh4g.simulator.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.mh4g.simulator.R
import com.mh4g.simulator.data.*
import com.mh4g.simulator.ui.adapters.EquipmentAdapter

class EquipExcludeFragment : Fragment() {

    private var slotId: Int = SLOT_HEAD
    private lateinit var listView: ListView
    private lateinit var searchBox: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var tvPinned: TextView
    private lateinit var btnClearPin: Button
    private lateinit var adapter: EquipmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slotId = arguments?.getInt(ARG_SLOT) ?: SLOT_HEAD
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_equip_exclude, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.equipList)
        searchBox = view.findViewById(R.id.searchBox)
        spinnerType = view.findViewById(R.id.spinnerType)
        tvPinned = view.findViewById(R.id.tvPinned)
        btnClearPin = view.findViewById(R.id.btnClearPin)

        val typeOptions = arrayOf("全種", "剣士", "ガンナー")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeOptions).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = a
        }

        adapter = EquipmentAdapter(requireContext(), AppData.getEquipList(slotId))
        listView.adapter = adapter

        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) { filterList() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updatePinDisplay()

        btnClearPin.setOnClickListener {
            AppData.unpinEquip(slotId)
            updatePinDisplay()
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val equip = adapter.getFilteredItem(pos)
            AppData.toggleExclude(equip)
            adapter.notifyDataSetChanged()
        }

        registerForContextMenu(listView)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.equipList) {
            val info = menuInfo as? AdapterView.AdapterContextMenuInfo ?: return
            val equip = adapter.getFilteredItem(info.position)
            menu.setHeaderTitle(equip.name)
            menu.add(0, CTX_TOGGLE_EXCLUDE, 0, if (AppData.isExcluded(equip)) "除外解除" else "除外する")
            menu.add(0, CTX_PIN, 1, "固定する")
            menu.add(0, CTX_SHOW_INFO, 2, "装備情報を見る")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val equip = adapter.getFilteredItem(info.position)
        return when (item.itemId) {
            CTX_TOGGLE_EXCLUDE -> {
                AppData.toggleExclude(equip)
                adapter.notifyDataSetChanged()
                true
            }
            CTX_PIN -> {
                AppData.pinEquip(equip)
                updatePinDisplay()
                true
            }
            CTX_SHOW_INFO -> {
                showEquipInfo(equip)
                true
            }
            else -> false
        }
    }

    private fun showEquipInfo(equip: Equipment) {
        com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, equip)
    }

    private fun filterList() {
        val query = searchBox.text.toString()
        val typeFilter = spinnerType.selectedItemPosition // 0=全, 1=剣士, 2=ガンナー
        adapter.filter(query, typeFilter)
    }

    private fun updatePinDisplay() {
        val pinned = AppData.pinnedEquip[slotId]
        tvPinned.text = if (pinned != null) "固定: ${pinned.name}" else "固定: なし"
        btnClearPin.isEnabled = pinned != null
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updatePinDisplay()
    }

    companion object {
        private const val ARG_SLOT = "slot"
        private const val CTX_TOGGLE_EXCLUDE = 1
        private const val CTX_PIN = 2
        private const val CTX_SHOW_INFO = 3

        fun newInstance(slot: Int) = EquipExcludeFragment().apply {
            arguments = Bundle().apply { putInt(ARG_SLOT, slot) }
        }
    }
}
