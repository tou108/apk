package com.mh4g.simulator.data

import android.content.Context

/**
 * アプリ全体で共有するデータを保持するシングルトン
 */
object AppData {

    // ============================================================
    // スキルデータ
    // ============================================================
    var skills: List<Skill> = emptyList()
    var skillSystems: List<SkillSystem> = emptyList()
    /** スキル系統名 → Skill一覧 */
    val skillsBySystem: MutableMap<String, List<Skill>> = mutableMapOf()
    /** スキル名 → Skill */
    val skillByName: MutableMap<String, Skill> = mutableMapOf()
    /** スキル系統名 → SkillSystem */
    val systemByName: MutableMap<String, SkillSystem> = mutableMapOf()

    // ============================================================
    // 防具データ
    // ============================================================
    var headList: List<Equipment> = emptyList()
    var bodyList: List<Equipment> = emptyList()
    var armList:  List<Equipment> = emptyList()
    var wstList:  List<Equipment> = emptyList()
    var legList:  List<Equipment> = emptyList()

    // ============================================================
    // 装飾品データ
    // ============================================================
    var decoList: List<Decoration> = emptyList()
    /** スキル系統名 → 装飾品リスト */
    val decoBySystem: MutableMap<String, List<Decoration>> = mutableMapOf()

    // ============================================================
    // お守りデータ
    // ============================================================
    var charmTypes: List<Triple<String, Int, Int>> = emptyList() // (name, rare, questRank)
    val userCharms = UserCharmList()

    // ============================================================
    // 除外・固定状態
    // ============================================================
    val excludedEquip = mutableSetOf<String>()   // 除外防具名セット (部位+名前)
    val pinnedEquip = mutableMapOf<Int, Equipment?>() // 固定防具 slot→Equipment
    val excludedDecos = mutableSetOf<String>()   // 除外装飾品名セット

    // ============================================================
    // マイセット
    // ============================================================
    val mySetList = mutableListOf<MySet>()

    // ============================================================
    // ロード済みフラグ
    // ============================================================
    var isLoaded = false

    fun initialize(context: Context) {
        if (isLoaded) return
        isLoaded = true

        // スキル
        val (s, sys) = DataLoader.loadSkills(context)
        skills = s
        skillSystems = sys
        skills.forEach { skillByName[it.name] = it }
        skillSystems.forEach { systemByName[it.name] = it }
        skillsBySystem.clear()
        skillsBySystem.putAll(skills.groupBy { it.system })

        // 防具
        headList = DataLoader.loadEquipment(context, "MH4G_EQUIP_HEAD.csv", SLOT_HEAD)
        bodyList = DataLoader.loadEquipment(context, "MH4G_EQUIP_BODY.csv", SLOT_BODY)
        armList  = DataLoader.loadEquipment(context, "MH4G_EQUIP_ARM.csv",  SLOT_ARM)
        wstList  = DataLoader.loadEquipment(context, "MH4G_EQUIP_WST.csv",  SLOT_WST)
        legList  = DataLoader.loadEquipment(context, "MH4G_EQUIP_LEG.csv",  SLOT_LEG)

        // 装飾品
        decoList = DataLoader.loadDecorations(context)
        decoBySystem.clear()
        val bySystem1 = decoList.groupBy { it.skill1 }
        bySystem1.forEach { (k, v) ->
            decoBySystem[k] = (decoBySystem[k] ?: emptyList()) + v
        }
        decoList.filter { it.skill2 != null }.groupBy { it.skill2!! }.forEach { (k, v) ->
            decoBySystem[k] = (decoBySystem[k] ?: emptyList()) + v
        }

        // お守り種類
        charmTypes = DataLoader.loadCharmTypes(context)

        // 固定初期化
        for (slot in 0..4) pinnedEquip[slot] = null
    }

    /** 全防具を部位別に返す */
    fun getEquipList(slot: Int): List<Equipment> = when(slot) {
        SLOT_HEAD -> headList
        SLOT_BODY -> bodyList
        SLOT_ARM  -> armList
        SLOT_WST  -> wstList
        SLOT_LEG  -> legList
        else -> emptyList()
    }

    /** 除外装備キー（部位+名前） */
    fun equipKey(equip: Equipment) = "${equip.slot}:${equip.name}"

    /** 防具が除外されているか */
    fun isExcluded(equip: Equipment) = excludedEquip.contains(equipKey(equip))

    /** 除外トグル */
    fun toggleExclude(equip: Equipment) {
        val key = equipKey(equip)
        if (excludedEquip.contains(key)) excludedEquip.remove(key)
        else excludedEquip.add(key)
    }

    /** 固定 */
    fun pinEquip(equip: Equipment) {
        pinnedEquip[equip.slot] = equip
    }

    /** 固定解除 */
    fun unpinEquip(slot: Int) {
        pinnedEquip[slot] = null
    }
}

/**
 * マイセット1件
 */
data class MySet(
    val label: String,
    val note: String,
    val head: Equipment?,
    val body: Equipment?,
    val arm: Equipment?,
    val wst: Equipment?,
    val leg: Equipment?,
    val charm: Charm?,
    val decos: List<Decoration>,
    val weaponSlot: Int,
    val activatedSkills: List<String>
)
