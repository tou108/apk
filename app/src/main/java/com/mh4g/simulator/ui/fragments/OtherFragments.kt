package com.mh4g.simulator.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.mh4g.simulator.R
import com.mh4g.simulator.data.*
import com.mh4g.simulator.ui.adapters.DecoAdapter
import com.mh4g.simulator.ui.adapters.CharmAdapter
import com.mh4g.simulator.ui.adapters.MySetAdapter

// ============================================================
// 装飾品除外タブ
// ============================================================
class DecoExcludeFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var searchBox: EditText
    private lateinit var adapter: DecoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        inflater.inflate(R.layout.fragment_deco_exclude, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.decoList)
        searchBox = view.findViewById(R.id.searchBox)

        adapter = DecoAdapter(requireContext(), AppData.decoList)
        listView.adapter = adapter

        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { adapter.filter(s.toString()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        listView.setOnItemClickListener { _, _, pos, _ ->
            val deco = adapter.getFilteredItem(pos)
            if (AppData.excludedDecos.contains(deco.name)) AppData.excludedDecos.remove(deco.name)
            else AppData.excludedDecos.add(deco.name)
            adapter.notifyDataSetChanged()
        }

        registerForContextMenu(listView)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, info: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, info)
        val i = info as? AdapterView.AdapterContextMenuInfo ?: return
        val deco = adapter.getFilteredItem(i.position)
        menu.setHeaderTitle(deco.name)
        menu.add(0, 1, 0, if (AppData.excludedDecos.contains(deco.name)) "除外解除" else "除外する")
        menu.add(0, 2, 1, "装飾品情報を見る")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val deco = adapter.getFilteredItem(info.position)
        return when (item.itemId) {
            1 -> {
                if (AppData.excludedDecos.contains(deco.name)) AppData.excludedDecos.remove(deco.name)
                else AppData.excludedDecos.add(deco.name)
                adapter.notifyDataSetChanged(); true
            }
            2 -> { showDecoInfo(deco); true }
            else -> false
        }
    }

    private fun showDecoInfo(deco: Decoration) {
        val sb = StringBuilder()
        sb.appendLine("【${deco.name}】")
        sb.appendLine("スロット: ${deco.slotCost}")
        sb.appendLine("${deco.skill1} +${deco.skill1Point}")
        deco.skill2?.let { sb.appendLine("$it ${if (deco.skill2Point >= 0) "+${deco.skill2Point}" else deco.skill2Point}") }
        AlertDialog.Builder(requireContext()).setTitle(deco.name).setMessage(sb)
            .setPositiveButton("閉じる", null).show()
    }

    override fun onResume() { super.onResume(); adapter.notifyDataSetChanged() }
}

// ============================================================
// お守り設定タブ
// ============================================================
class CharmFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAdd: Button
    private lateinit var adapter: CharmAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        inflater.inflate(R.layout.fragment_charm, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.charmList)
        btnAdd = view.findViewById(R.id.btnAddCharm)

        adapter = CharmAdapter(requireContext(), AppData.userCharms.getAll())
        listView.adapter = adapter

        btnAdd.setOnClickListener { showAddCharmDialog() }
        registerForContextMenu(listView)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, info: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, info)
        val i = info as? AdapterView.AdapterContextMenuInfo ?: return
        menu.setHeaderTitle(adapter.getItem(i.position)?.displayName() ?: "")
        menu.add(0, 1, 0, "削除する")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val charm = adapter.getItem(info.position) ?: return false
        if (item.itemId == 1) {
            AppData.userCharms.remove(charm)
            adapter.updateData(AppData.userCharms.getAll())
        }
        return true
    }

    private fun showAddCharmDialog() {
        com.mh4g.simulator.ui.dialogs.AddCharmDialog { charm ->
            AppData.userCharms.add(charm)
            adapter.updateData(AppData.userCharms.getAll())
        }.show(parentFragmentManager, "AddCharm")
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(AppData.userCharms.getAll())
    }
}

// ============================================================
// マイセットタブ
// ============================================================
class MySetFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var detailView: TextView
    private lateinit var adapter: MySetAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?) =
        inflater.inflate(R.layout.fragment_myset, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = view.findViewById(R.id.mySetList)
        detailView = view.findViewById(R.id.mySetDetail)

        adapter = MySetAdapter(requireContext(), AppData.mySetList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, pos, _ ->
            val mySet = AppData.mySetList[pos]
            detailView.text = buildMySetDetail(mySet)
        }

        registerForContextMenu(listView)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, info: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, info)
        val i = info as? AdapterView.AdapterContextMenuInfo ?: return
        menu.setHeaderTitle(AppData.mySetList[i.position].label)
        menu.add(0, 1, 0, "削除する")
        menu.add(0, 2, 1, "メモを編集する")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val pos = info.position
        return when (item.itemId) {
            1 -> { AppData.mySetList.removeAt(pos); adapter.notifyDataSetChanged(); true }
            2 -> { showEditNoteDialog(pos); true }
            else -> false
        }
    }

    private fun showEditNoteDialog(pos: Int) {
        val mySet = AppData.mySetList[pos]
        val editText = EditText(requireContext()).apply { setText(mySet.note) }
        AlertDialog.Builder(requireContext())
            .setTitle("メモ編集")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                AppData.mySetList[pos] = mySet.copy(note = editText.text.toString())
                adapter.notifyDataSetChanged()
                if (listView.checkedItemPosition == pos) {
                    detailView.text = buildMySetDetail(AppData.mySetList[pos])
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun buildMySetDetail(mySet: com.mh4g.simulator.data.MySet): String {
        val sb = StringBuilder()
        sb.appendLine("【${mySet.label}】")
        if (mySet.note.isNotEmpty()) sb.appendLine("メモ: ${mySet.note}")
        sb.appendLine()
        sb.appendLine("頭: ${mySet.head?.name ?: "---"}")
        sb.appendLine("胴: ${mySet.body?.name ?: "---"}")
        sb.appendLine("腕: ${mySet.arm?.name ?: "---"}")
        sb.appendLine("腰: ${mySet.wst?.name ?: "---"}")
        sb.appendLine("脚: ${mySet.leg?.name ?: "---"}")
        if (mySet.charm != null) sb.appendLine("護石: ${mySet.charm.displayName()}")
        if (mySet.decos.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("装飾品:")
            for (deco in mySet.decos) sb.appendLine("  ${deco.name}")
        }
        if (mySet.activatedSkills.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("発動スキル:")
            for (s in mySet.activatedSkills) sb.appendLine("  $s")
        }
        return sb.toString()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
