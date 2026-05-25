package com.mh4g.simulator.ui.adapters

import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.*
import com.mh4g.simulator.data.*
import com.mh4g.simulator.search.*

// ============================================================
// 検索結果リストアダプター
// ============================================================
class ResultListAdapter(
    private val context: Context,
    private var data: List<SearchResult>
) : BaseAdapter() {

    override fun getCount() = data.size
    override fun getItem(pos: Int) = data[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val result = data[pos]
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        val equips = listOf(result.head, result.body, result.arm, result.wst, result.leg)
        val equipStr = equips.mapIndexed { i, e ->
            val slot = arrayOf("頭", "胴", "腕", "腰", "脚")[i]
            if (e != null) "$slot:${e.name}" else "$slot:---"
        }.joinToString("  ")

        val skills = result.activatedSkills
            .filter { it.isTarget }
            .joinToString(" ") { it.skillName }

        text1.text = equipStr
        text1.textSize = 10f
        text2.text = "防${result.totalDefMax} | $skills"
        text2.textSize = 11f

        view.setBackgroundColor(if (pos % 2 == 0) Color.WHITE else Color.parseColor("#F0F0F0"))
        return view
    }

    fun updateData(newData: List<SearchResult>) {
        data = newData
        notifyDataSetChanged()
    }
}

// ============================================================
// 空きスロットパターンアダプター
// ============================================================
class EmptySlotAdapter(
    private val context: Context,
    private var data: List<EmptySlotPattern>
) : BaseAdapter() {

    override fun getCount() = data.size
    override fun getItem(pos: Int) = data[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val pattern = data[pos]
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        val decoStr = pattern.decos.filterNotNull().joinToString(",") { it.name }
        val skillStr = pattern.additionalSkills.joinToString(" ") { it.skillName }
        text1.text = "装飾品: ${decoStr.ifEmpty { "なし" }}"
        text1.textSize = 11f
        text2.text = if (skillStr.isNotEmpty()) "追加: $skillStr" else "追加スキルなし"
        text2.textSize = 10f
        return view
    }

    fun updateData(newData: List<EmptySlotPattern>) {
        data = newData
        notifyDataSetChanged()
    }
}

// ============================================================
// 防具リストアダプター（除外・固定タブ用）
// ============================================================
class EquipmentAdapter(
    private val context: Context,
    private val allData: List<Equipment>
) : BaseAdapter() {

    private var filteredData = allData.toMutableList()

    override fun getCount() = filteredData.size
    override fun getItem(pos: Int) = filteredData[pos]
    override fun getItemId(pos: Int) = pos.toLong()
    fun getFilteredItem(pos: Int) = filteredData[pos]

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val equip = filteredData[pos]
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        val excluded = AppData.isExcluded(equip)
        text1.text = (if (excluded) "[除外] " else "") + equip.name + "  " + equip.slotStr()
        text1.setTextColor(if (excluded) Color.GRAY else Color.BLACK)
        text1.textSize = 13f

        val skills = equip.skillPoints.joinToString("  ") { "${it.first}${if (it.second >= 0) "+${it.second}" else "${it.second}"}" }
        val pinned = AppData.pinnedEquip[equip.slot]?.name == equip.name
        text2.text = "${if (pinned) "[固定] " else ""}防${equip.defMax} $skills"
        text2.textSize = 11f

        view.setBackgroundColor(when {
            pinned -> Color.parseColor("#FFF3E0")
            excluded -> Color.parseColor("#FAFAFA")
            pos % 2 == 0 -> Color.WHITE
            else -> Color.parseColor("#F0F0F0")
        })
        return view
    }

    fun filter(query: String, typeFilter: Int = 0) {
        filteredData = allData.filter { equip ->
            val matchName = query.isEmpty() || equip.name.contains(query)
            val matchType = typeFilter == 0 || equip.type == 0 || equip.type == typeFilter
            matchName && matchType
        }.toMutableList()
        notifyDataSetChanged()
    }
}

// ============================================================
// 装飾品アダプター
// ============================================================
class DecoAdapter(
    private val context: Context,
    private val allData: List<Decoration>
) : BaseAdapter() {

    private var filteredData = allData.toMutableList()

    override fun getCount() = filteredData.size
    override fun getItem(pos: Int) = filteredData[pos]
    override fun getItemId(pos: Int) = pos.toLong()
    fun getFilteredItem(pos: Int) = filteredData[pos]

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val deco = filteredData[pos]
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)
        val excluded = AppData.excludedDecos.contains(deco.name)
        text1.text = (if (excluded) "[除外] " else "") + deco.name + "  スロ${deco.slotCost}"
        text1.setTextColor(if (excluded) Color.GRAY else Color.BLACK)
        text2.text = "${deco.skill1}+${deco.skill1Point}" +
                (deco.skill2?.let { " / $it${if (deco.skill2Point >= 0) "+${deco.skill2Point}" else deco.skill2Point}" } ?: "")
        view.setBackgroundColor(if (pos % 2 == 0) Color.WHITE else Color.parseColor("#F0F0F0"))
        return view
    }

    fun filter(query: String) {
        filteredData = if (query.isEmpty()) allData.toMutableList()
        else allData.filter { it.name.contains(query) || it.skill1.contains(query) }.toMutableList()
        notifyDataSetChanged()
    }
}

// ============================================================
// お守りアダプター
// ============================================================
class CharmAdapter(
    private val context: Context,
    data: List<Charm>
) : ArrayAdapter<Charm>(context, android.R.layout.two_line_list_item, data.toMutableList()) {

    fun updateData(newData: List<Charm>) {
        clear()
        addAll(newData)
        notifyDataSetChanged()
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val charm = getItem(pos) ?: return view
        view.findViewById<TextView>(android.R.id.text1).text = charm.displayName()
        view.findViewById<TextView>(android.R.id.text2).text =
            charm.skillPoints.joinToString("  ") {
                "${it.first}${if (it.second >= 0) "+${it.second}" else "${it.second}"}"
            }
        return view
    }
}

// ============================================================
// マイセットアダプター
// ============================================================
class MySetAdapter(
    context: Context,
    data: MutableList<com.mh4g.simulator.data.MySet>
) : ArrayAdapter<com.mh4g.simulator.data.MySet>(context, android.R.layout.two_line_list_item, data) {

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.two_line_list_item, parent, false)
        val mySet = getItem(pos) ?: return view
        view.findViewById<TextView>(android.R.id.text1).text = mySet.label
        view.findViewById<TextView>(android.R.id.text2).text =
            mySet.activatedSkills.joinToString(" ")
        view.setBackgroundColor(if (pos % 2 == 0) Color.WHITE else Color.parseColor("#F0F0F0"))
        return view
    }
}
