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
            menu.add(0, MENU_ADD_MYSET, 0, "マイセットに追加する")
            menu.add(0, MENU_EXCLUDE_HEAD, 1, "頭装備を除外する")
            menu.add(0, MENU_EXCLUDE_BODY, 2, "胴装備を除外する")
            menu.add(0, MENU_EXCLUDE_ARM,  3, "腕装備を除外する")
            menu.add(0, MENU_EXCLUDE_WST,  4, "腰装備を除外する")
            menu.add(0, MENU_EXCLUDE_LEG,  5, "脚装備を除外する")
            menu.add(0, MENU_PIN_HEAD, 6, "頭装備を固定する")
            menu.add(0, MENU_PIN_BODY, 7, "胴装備を固定する")
            menu.add(0, MENU_PIN_ARM,  8, "腕装備を固定する")
            menu.add(0, MENU_PIN_WST,  9, "腰装備を固定する")
            menu.add(0, MENU_PIN_LEG,  10, "脚装備を固定する")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return false
        val result = if (info.position < searchResults.size) searchResults[info.position] else return false
        when (item.itemId) {
            MENU_ADD_MYSET -> addToMySet(result)
            MENU_EXCLUDE_HEAD -> result.head?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_BODY -> result.body?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_ARM  -> result.arm?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_WST  -> result.wst?.let { AppData.toggleExclude(it) }
            MENU_EXCLUDE_LEG  -> result.leg?.let { AppData.toggleExclude(it) }
            MENU_PIN_HEAD -> result.head?.let { AppData.pinEquip(it) }
            MENU_PIN_BODY -> result.body?.let { AppData.pinEquip(it) }
            MENU_PIN_ARM  -> result.arm?.let { AppData.pinEquip(it) }
            MENU_PIN_WST  -> result.wst?.let { AppData.pinEquip(it) }
            MENU_PIN_LEG  -> result.leg?.let { AppData.pinEquip(it) }
        }
        return true
    }

    private fun startSearch() {
        val cond = SearchCondition(
            gender = spinnerGender.selectedItemPosition,
            armorType = spinnerType.selectedItemPosition,
            weaponSlotAuto = spinnerWeaponSlot.selectedItemPosition == 0,
            weaponSlot = if (spinnerWeaponSlot.selectedItemPosition == 0) -1
                else spinnerWeaponSlot.selectedItemPosition - 1,
            targetSkills = selectedSkills.filter { !it.startsWith("-") },
            excludeSkills = selectedSkills.filter { it.startsWith("-") }.map { it.drop(1) } +
                    excludeSkills,
            useExclude = cbUseExclude.isChecked,
            searchCharm = cbSearchCharm.isChecked,
            useCharms = AppData.userCharms.getAll().isNotEmpty()
        )
        viewModel.updateCondition(cond)
        viewModel.startSearch()
    }

    private fun showSkillSelectionDialog() {
        val skills = AppData.skills.map { it.name }.toTypedArray()
        var selected = -1
        AlertDialog.Builder(requireContext())
            .setTitle("スキル選択")
            .setSingleChoiceItems(skills, -1) { _, which -> selected = which }
            .setPositiveButton("追加（発動）") { _, _ ->
                if (selected >= 0) {
                    addSkillChip(skills[selected], false)
                }
            }
            .setNeutralButton("追加（除外）") { _, _ ->
                if (selected >= 0) {
                    addSkillChip(skills[selected], true)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun addSkillChip(skillName: String, exclude: Boolean) {
        val displayName = if (exclude) "-$skillName" else skillName
        if (selectedSkills.contains(displayName)) return
        selectedSkills.add(displayName)

        val chip = Button(requireContext())
        chip.text = displayName
        chip.textSize = 12f
        chip.setOnClickListener {
            selectedSkills.remove(displayName)
            skillSelectionContainer.removeView(chip)
        }
        skillSelectionContainer.addView(chip)
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
        private const val MENU_ADD_MYSET = 1
        private const val MENU_EXCLUDE_HEAD = 2
        private const val MENU_EXCLUDE_BODY = 3
        private const val MENU_EXCLUDE_ARM = 4
        private const val MENU_EXCLUDE_WST = 5
        private const val MENU_EXCLUDE_LEG = 6
        private const val MENU_PIN_HEAD = 7
        private const val MENU_PIN_BODY = 8
        private const val MENU_PIN_ARM = 9
        private const val MENU_PIN_WST = 10
        private const val MENU_PIN_LEG = 11
    }
}
