package com.mh4g.simulator.search

import com.mh4g.simulator.data.*

/**
 * 検索条件（PC版 c.n クラス相当）
 */
data class SearchCondition(
    var gender: Int = 0,            // 0=両, 1=男, 2=女
    var armorType: Int = 0,         // 0=両, 1=剣士, 2=ガンナー
    var questRank: Int = 0,         // 集☆下限 (0=指定なし)
    var villageRank: Int = 0,       // 村☆下限 (0=指定なし)
    var targetSkills: List<String> = emptyList(), // 発動させたいスキル名リスト
    var excludeSkills: List<String> = emptyList(), // 除外スキル名リスト（マイナス）
    var weaponSlot: Int = -1,        // 武器スロット数 (-1=自動)
    var weaponSlotAuto: Boolean = true,
    var decoSpecs: List<DecoSpec> = emptyList(),  // 装飾品直接指定
    var useExclude: Boolean = true,   // 除外・固定を使う
    var searchCharm: Boolean = true,  // お守り検索
    var useCharms: Boolean = true,    // お守りを使う
    var maxResults: Int = 5000,       // 最大結果数
    var rankType: Int = 0             // 入手時期タイプ (0=集, 1=村)
)

/**
 * 装飾品指定
 */
data class DecoSpec(
    val deco: Decoration?,          // null=空き
    val slot: Int                   // スロット番号
)

/**
 * 検索結果1件（PC版 c.g クラス相当）
 */
data class SearchResult(
    val head: Equipment?,
    val body: Equipment?,
    val arm: Equipment?,
    val wst: Equipment?,
    val leg: Equipment?,
    val charm: Charm?,
    val decos: List<Decoration?>,   // 装飾品リスト（武器スロ+防具スロ分）
    val weaponSlotUsed: Int,        // 使用武器スロット数
    val activatedSkills: List<ActivatedSkill>,  // 発動スキル
    val totalDefMax: Int,           // 最終防御力合計
    val totalResFire: Int,
    val totalResWater: Int,
    val totalResThunder: Int,
    val totalResIce: Int,
    val totalResDragon: Int,
    val freeSlots: Int,             // 余り装飾スロット数
    val emptySlotPatterns: List<EmptySlotPattern> = emptyList()  // 空きスロット活用パターン
) {
    fun equipCount(): Int = listOf(head, body, arm, wst, leg, charm)
        .count { it != null }

    fun noEquipCount(): Int = 6 - equipCount()

    /** 表示用装備名（PC版のtoStringに相当） */
    fun toDisplayRows(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf("頭", head?.name ?: "---", head?.slotStr() ?: "",
                head?.defMax ?: "", head?.resFire ?: 0, head?.resWater ?: 0,
                head?.resThunder ?: 0, head?.resIce ?: 0, head?.resDragon ?: 0),
            arrayOf("胴", body?.name ?: "---", body?.slotStr() ?: "",
                body?.defMax ?: "", body?.resFire ?: 0, body?.resWater ?: 0,
                body?.resThunder ?: 0, body?.resIce ?: 0, body?.resDragon ?: 0),
            arrayOf("腕", arm?.name ?: "---", arm?.slotStr() ?: "",
                arm?.defMax ?: "", arm?.resFire ?: 0, arm?.resWater ?: 0,
                arm?.resThunder ?: 0, arm?.resIce ?: 0, arm?.resDragon ?: 0),
            arrayOf("腰", wst?.name ?: "---", wst?.slotStr() ?: "",
                wst?.defMax ?: "", wst?.resFire ?: 0, wst?.resWater ?: 0,
                wst?.resThunder ?: 0, wst?.resIce ?: 0, wst?.resDragon ?: 0),
            arrayOf("脚", leg?.name ?: "---", leg?.slotStr() ?: "",
                leg?.defMax ?: "", leg?.resFire ?: 0, leg?.resWater ?: 0,
                leg?.resThunder ?: 0, leg?.resIce ?: 0, leg?.resDragon ?: 0)
        )
    }
}

/**
 * 発動スキル1件
 */
data class ActivatedSkill(
    val skillName: String,
    val systemName: String,
    val totalPoint: Int,
    val isTarget: Boolean       // 目標スキルか
)

/**
 * 空きスロット活用パターン
 */
data class EmptySlotPattern(
    val decos: List<Decoration?>,
    val additionalSkills: List<ActivatedSkill>,
    val minusSkills: List<ActivatedSkill>,
    val score: Int              // スコア（マイナス少・プラス多が上位）
)
