package com.mh4g.simulator.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mh4g.simulator.R
import com.mh4g.simulator.data.*
import com.mh4g.simulator.search.*
import com.mh4g.simulator.ui.adapters.*
import com.mh4g.simulator.ui.dialogs.*

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by activityViewModels()

    // 検索条件UI
    private lateinit var skillSelectionContainer: LinearLayout
    private lateinit var btnAddSkill: Button
    private lateinit var spinnerGender: Spinner
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerWeaponSlot: Spinner
    private lateinit var cbUseExclude: CheckBox
    private lateinit var cbSearchCharm: CheckBox
    private lateinit var btnSearch: Button
    private lateinit var btnStop: Button

    // 結果UI
    private lateinit var resultLogView: TextView
    private lateinit var resultTable: ListView
    private lateinit var resultDetailView: TextView
    private lateinit var emptySlotTable: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResultCount: TextView

    // 選択中スキル一覧
    private val selectedSkills = mutableListOf<String>()
    private val excludeSkills = mutableListOf<String>()

    // 結果データ
    private var searchResults = listOf<SearchResult>()
    private var selectedResult: SearchResult? = null
    private lateinit var resultAdapter: ResultListAdapter
    private lateinit var emptySlotAdapter: EmptySlotAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupUI()
        observeViewModel()
    }

    private fun bindViews(v: View) {
        skillSelectionContainer = v.findViewById(R.id.skillSelectionContainer)
        btnAddSkill = v.findViewById(R.id.btnAddSkill)
        spinnerGender = v.findViewById(R.id.spinnerGender)
        spinnerType = v.findViewById(R.id.spinnerType)
        spinnerWeaponSlot = v.findViewById(R.id.spinnerWeaponSlot)
        cbUseExclude = v.findViewById(R.id.cbUseExclude)
        cbSearchCharm = v.findViewById(R.id.cbSearchCharm)
        btnSearch = v.findViewById(R.id.btnSearch)
        btnStop = v.findViewById(R.id.btnStop)
        resultLogView = v.findViewById(R.id.resultLogView)
        resultTable = v.findViewById(R.id.resultTable)
        resultDetailView = v.findViewById(R.id.resultDetailView)
        emptySlotTable = v.findViewById(R.id.emptySlotTable)
        progressBar = v.findViewById(R.id.progressBar)
        tvResultCount = v.findViewById(R.id.tvResultCount)
    }

    private fun setupUI() {
        // スピナー設定
        ArrayAdapter.createFromResource(requireContext(), R.array.gender_options,
            android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGender.adapter = adapter
        }
        ArrayAdapter.createFromResource(requireContext(), R.array.type_options,
            android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = adapter
        }
        val weaponSlotOptions = arrayOf("自動", "0", "1", "2", "3")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weaponSlotOptions).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWeaponSlot.adapter = adapter
        }

        // スキル追加ボタン
        btnAddSkill.setOnClickListener { showSkillSelectionDialog() }

        // 検索ボタン
        btnSearch.setOnClickListener { startSearch() }
        btnStop.setOnClickListener { viewModel.cancelSearch() }
        btnStop.visibility = View.GONE

        // チェックボックス
        cbUseExclude.isChecked = true
        cbSearchCharm.isChecked = true

        // 結果リスト
        resultAdapter = ResultListAdapter(requireContext(), emptyList())
        resultTable.adapter = resultAdapter
        resultTable.setOnItemClickListener { _, _, pos, _ ->
            onResultSelected(searchResults[pos])
        }
        registerForContextMenu(resultTable)

        emptySlotAdapter = EmptySlotAdapter(requireContext(), emptyList())
        emptySlotTable.adapter = emptySlotAdapter
    }

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnSearch.isEnabled = true
                    btnStop.visibility = View.GONE
                }
                is SearchState.Searching -> {
                    progressBar.visibility = View.VISIBLE
                    btnSearch.isEnabled = false
                    btnStop.visibility = View.VISIBLE
                    resultLogView.text = "検索中..."
                }
                is SearchState.Done -> {
                    progressBar.visibility = View.GONE
                    btnSearch.isEnabled = true
                    btnStop.visibility = View.GONE
                    handleSearchDone(state.results)
                }
                is SearchState.Error -> {
                    progressBar.visibility = View.GONE
                    btnSearch.isEnabled = true
                    btnStop.visibility = View.GONE
                    resultLogView.text = "エラー: ${state.message}"
                }
            }
        }
        viewModel.progress.observe(viewLifecycleOwner) { prog ->
            progressBar.progress = prog
        }
    }

    private fun handleSearchDone(results: SearchResults) {
        searchResults = results.results
        resultAdapter.updateData(searchResults)
        tvResultCount.text = "${searchResults.size}件"

        val sb = StringBuilder()
        if (searchResults.isEmpty()) {
            sb.appendLine("結果なし")
            if (results.charmSuggestions.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("【持っていれば発動するお守り】")
                for (charm in results.charmSuggestions) {
                    sb.appendLine("  ${charm.skillPoints.joinToString(", ") { "${it.first} +${it.second}" }}" +
                            if (charm.slotCount > 0) " スロ${charm.slotCount}" else "")
                }
            }
        } else {
            sb.appendLine("${searchResults.size}件ヒット")
            sb.appendLine("装備なし数→最終強化防御力→耐性値の合計 順で表示")
        }
        resultLogView.text = sb.toString()

        if (searchResults.isNotEmpty()) {
            onResultSelected(searchResults[0])
        }
    }

    private fun onResultSelected(result: SearchResult) {
        selectedResult = result
        resultDetailView.text = buildDetailText(result)
        emptySlotAdapter.updateData(result.emptySlotPatterns)
    }

    private fun buildDetailText(result: SearchResult): String {
        val sb = StringBuilder()

        // 装備一覧
        sb.appendLine("【装備】")
        fun equipLine(name: String, equip: Equipment?) {
            if (equip != null) sb.appendLine("$name: ${equip.name} ${equip.slotStr()}")
            else sb.appendLine("$name: ---")
        }
        equipLine("頭", result.head)
        equipLine("胴", result.body)
        equipLine("腕", result.arm)
        equipLine("腰", result.wst)
        equipLine("脚", result.leg)
        if (result.charm != null) sb.appendLine("護石: ${result.charm.displayName()}")

        // 装飾品
        if (result.decos.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("【装飾品】")
            for (deco in result.decos) {
                if (deco != null) sb.appendLine("  ${deco.name}")
            }
        }
        if (result.weaponSlotUsed > 0) sb.appendLine("  武器スロ: ${result.weaponSlotUsed}使用")
        if (result.freeSlots > 0) sb.appendLine("  空きスロ: ${result.freeSlots}")

        // ステータス
        sb.appendLine()
        sb.appendLine("【ステータス】")
        sb.appendLine("最終防御力: ${result.totalDefMax}")
        sb.appendLine("耐性: 火${result.totalResFire} 水${result.totalResWater} " +
                "雷${result.totalResThunder} 氷${result.totalResIce} 龍${result.totalResDragon}")

        // 発動スキル
        sb.appendLine()
        sb.appendLine("【発動スキル】")
        val (targets, others) = result.activatedSkills.partition { it.isTarget }
        for (skill in targets) sb.appendLine("  ◎ ${skill.skillName} (${skill.systemName} ${skill.totalPoint}pt)")
        for (skill in others) {
            val marker = if (skill.totalPoint < 0) "  △" else "  ○"
            sb.appendLine("$marker ${skill.skillName} (${skill.systemName} ${skill.totalPoint}pt)")
        }

        return sb.toString()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (v.id == R.id.resultTable) {
            menu.setHeaderTitle("操作")
            menu.add(0, MENU_ADD_MYSET,    0, "マイセットに追加する")
            menu.add(0, MENU_SHOW_SKILLPT, 1, "スキルポイントを表示する")
            menu.add(0, MENU_EXCLUDE_HEAD, 2, "頭装備を除外する")
            menu.add(0, MENU_EXCLUDE_BODY, 3, "胴装備を除外する")
            menu.add(0, MENU_EXCLUDE_ARM,  4, "腕装備を除外する")
            menu.add(0, MENU_EXCLUDE_WST,  5, "腰装備を除外する")
            menu.add(0, MENU_EXCLUDE_LEG,  6, "脚装備を除外する")
            menu.add(0, MENU_PIN_HEAD,     7, "頭装備を固定する")
            menu.add(0, MENU_PIN_BODY,     8, "胴装備を固定する")
            menu.add(0, MENU_PIN_ARM,      9, "腕装備を固定する")
            menu.add(0, MENU_PIN_WST,     10, "腰装備を固定する")
            menu.add(0, MENU_PIN_LEG,     11, "脚装備を固定する")
            menu.add(0, MENU_INFO_HEAD,   12, "頭装備の情報を見る")
            menu.add(0, MENU_INFO_BODY,   13, "胴装備の情報を見る")
            menu.add(0, MENU_INFO_ARM,    14, "腕装備の情報を見る")
            menu.add(0, MENU_INFO_WST,    15, "腰装備の情報を見る")
            menu.add(0, MENU_INFO_LEG,    16, "脚装備の情報を見る")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val result = if (info.position < searchResults.size) searchResults[info.position] else return false
        when (item.itemId) {
            MENU_ADD_MYSET    -> addToMySet(result)
            MENU_SHOW_SKILLPT -> com.mh4g.simulator.ui.dialogs.SkillPointTableDialog.show(this, result)
            MENU_EXCLUDE_HEAD -> result.head?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_BODY -> result.body?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_ARM  -> result.arm?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_WST  -> result.wst?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_LEG  -> result.leg?.let { AppData.toggleExclude(it) }
            MENU_PIN_HEAD     -> result.head?.let { AppData.pinEquip(it) }
            MENU_PIN_BODY     -> result.body?.let { AppData.pinEquip(it) }
            MENU_PIN_ARM      -> result.arm?.let { AppData.pinEquip(it) }
            MENU_PIN_WST      -> result.wst?.let { AppData.pinEquip(it) }
            MENU_PIN_LEG      -> result.leg?.let { AppData.pinEquip(it) }
            MENU_INFO_HEAD    -> result.head?.let { com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, it) }
            MENU_INFO_BODY    -> result.body?.let { com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, it) }
            MENU_INFO_ARM     -> result.arm?.let { com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, it) }
            MENU_INFO_WST     -> result.wst?.let { com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, it) }
            MENU_INFO_LEG     -> result.leg?.let { com.mh4g.simulator.ui.dialogs.EquipInfoDialog.show(this, it) }
        }
        return true
    }

    private fun startSearch() {
        val targets = selectedSkills.filter { !it.startsWith("-") }
        val excludes = selectedSkills.filter { it.startsWith("-") }.map { it.drop(1) }

        if (targets.isEmpty()) {
            Toast.makeText(context, "発動スキルを1つ以上選択してください", Toast.LENGTH_SHORT).show()
            return
        }

        val cond = SearchCondition(
            gender          = spinnerGender.selectedItemPosition,
            armorType       = spinnerType.selectedItemPosition,
            weaponSlotAuto  = currentWeaponSlot < 0,
            weaponSlot      = currentWeaponSlot,
            targetSkills    = targets,
            excludeSkills   = excludes,
            decoSpecs       = currentDecoSpecs,
            useExclude      = cbUseExclude.isChecked,
            searchCharm     = cbSearchCharm.isChecked,
            useCharms       = AppData.userCharms.getAll().isNotEmpty()
        )
        viewModel.updateCondition(cond)
        viewModel.startSearch()
    }

    private fun showSkillSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_skill_select, null)
        val filterEdit = dialogView.findViewById<android.widget.EditText>(R.id.etSkillFilter)
        val listView   = dialogView.findViewById<android.widget.ListView>(R.id.skillListView)

        // 系統名→スキル名（発動スキルのみ。point>0のもの）
        val allSkillNames = AppData.skills
            .filter { it.point > 0 }
            .map { it.name }
            .distinct()
            .sorted()

        var filteredSkills = allSkillNames.toMutableList()
        var selectedSkillName: String? = null

        val listAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_single_choice,
            filteredSkills
        )
        listView.adapter = listAdapter
        listView.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
        listView.setOnItemClickListener { _, _, pos, _ ->
            selectedSkillName = filteredSkills[pos]
        }

        filterEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val query = s.toString()
                filteredSkills = allSkillNames.filter {
                    query.isEmpty() || it.contains(query) ||
                    (AppData.skillByName[it]?.system?.contains(query) == true)
                }.toMutableList()
                listAdapter.clear()
                listAdapter.addAll(filteredSkills)
                listAdapter.notifyDataSetChanged()
                selectedSkillName = null
            }
        })

        AlertDialog.Builder(requireContext())
            .setTitle("スキル選択")
            .setView(dialogView)
            .setPositiveButton("発動スキルに追加") { _, _ ->
                selectedSkillName?.let { addSkillChip(it, false) }
            }
            .setNeutralButton("除外スキルに追加") { _, _ ->
                selectedSkillName?.let { addSkillChip(it, true) }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun addSkillChip(skillName: String, exclude: Boolean) {
        val displayName = if (exclude) "[-]$skillName" else skillName
        val internalName = if (exclude) "-$skillName" else skillName
        if (selectedSkills.contains(internalName)) return
        selectedSkills.add(internalName)

        val chip = android.widget.Button(requireContext())
        chip.text = displayName
        chip.textSize = 11f
        chip.setPadding(12, 4, 12, 4)
        val bg = if (exclude) 0xFFB71C1C.toInt() else 0xFF1565C0.toInt()
        chip.setBackgroundColor(bg)
        chip.setTextColor(android.graphics.Color.WHITE)
        chip.setOnClickListener {
            selectedSkills.remove(internalName)
            skillSelectionContainer.removeView(chip)
        }

        val lp = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(4, 4, 4, 4) }
        skillSelectionContainer.addView(chip, lp)
    }

    // 武器スロット・装飾品直接指定の状態
    private var currentWeaponSlot: Int = -1
    private var currentDecoSpecs: List<com.mh4g.simulator.search.DecoSpec> = emptyList()

    private fun showWeaponDecoDialog() {
        com.mh4g.simulator.ui.dialogs.WeaponDecoDialog(
            currentWeaponSlot, currentDecoSpecs
        ) { weapSlot, specs ->
            currentWeaponSlot = weapSlot
            currentDecoSpecs = specs
            val slotText = if (weapSlot < 0) "自動" else "武器スロ$weapSlot"
            val decoText = if (specs.isEmpty()) "" else
                "  装飾品${specs.size}個指定"
            spinnerWeaponSlot.let { sp ->
                (sp.adapter as? android.widget.ArrayAdapter<*>)?.let { a ->
                    // 表示更新用
                }
            }
            Toast.makeText(context, "$slotText$decoText を設定しました", Toast.LENGTH_SHORT).show()
        }.show(parentFragmentManager, "WeaponDeco")
    }

    private fun addToMySet(result: SearchResult) {
        val dialog = SaveMySetDialog(result) { label, note ->
            AppData.mySetList.add(
                com.mh4g.simulator.data.MySet(
                    label = label,
                    note = note,
                    head = result.head,
                    body = result.body,
                    arm = result.arm,
                    wst = result.wst,
                    leg = result.leg,
                    charm = result.charm,
                    decos = result.decos.filterNotNull(),
                    weaponSlot = result.weaponSlotUsed,
                    activatedSkills = result.activatedSkills.map { it.skillName }
                )
            )
            Toast.makeText(context, "マイセットに追加しました", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "SaveMySet")
    }

    companion object {
        private const val MENU_ADD_MYSET    = 1
        private const val MENU_SHOW_SKILLPT = 2
        private const val MENU_EXCLUDE_HEAD = 3
        private const val MENU_EXCLUDE_BODY = 4
        private const val MENU_EXCLUDE_ARM  = 5
        private const val MENU_EXCLUDE_WST  = 6
        private const val MENU_EXCLUDE_LEG  = 7
        private const val MENU_PIN_HEAD     = 8
        private const val MENU_PIN_BODY     = 9
        private const val MENU_PIN_ARM      = 10
        private const val MENU_PIN_WST      = 11
        private const val MENU_PIN_LEG      = 12
        private const val MENU_INFO_HEAD    = 13
        private const val MENU_INFO_BODY    = 14
        private const val MENU_INFO_ARM     = 15
        private const val MENU_INFO_WST     = 16
        private const val MENU_INFO_LEG     = 17
    }
}
