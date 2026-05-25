package com.mh4g.simulator.data

/**
 * スキル系統（スキルポイントをまとめる単位）
 */
data class SkillSystem(
    val name: String,           // スキル系統名
    val type: Int               // 0=両方, 1=剣士, 2=ガンナー
)

/**
 * スキル（発動するスキル効果）
 */
data class Skill(
    val name: String,           // スキル名
    val system: String,         // スキル系統名
    val point: Int,             // 必要ポイント（負=マイナス）
    val type: Int               // 0=両方, 1=剣士, 2=ガンナー
)

/**
 * 防具スロット定義（各部位の入れ替えパターン）
 */
const val SLOT_HEAD = 0
const val SLOT_BODY = 1
const val SLOT_ARM  = 2
const val SLOT_WST  = 3
const val SLOT_LEG  = 4
const val SLOT_CHARM = 5
const val SLOT_NONE = -1

/**
 * 防具1件
 */
data class Equipment(
    val name: String,
    val slot: Int,              // 部位 (SLOT_*)
    val gender: Int,            // 0=両, 1=男, 2=女
    val type: Int,              // 0=両方, 1=剣士, 2=ガンナー
    val rare: Int,
    val slotCount: Int,         // 装飾品スロット数
    val questRank: Int,         // 入手時期集☆ (99=不可)
    val villageRank: Int,       // 入手時期村☆ (99=不可)
    val defBase: Int,
    val defMax: Int,
    val resFire: Int,
    val resWater: Int,
    val resThunder: Int,
    val resIce: Int,
    val resDragon: Int,
    val skillPoints: List<Pair<String, Int>>,  // (系統名, ポイント) 最大5個
    val materials: List<Pair<String, Int>>     // (素材名, 個数)
) {
    /** 装飾品スロット文字列表示 */
    fun slotStr(): String = "○".repeat(slotCount) + "－".repeat(3 - slotCount)
    
    /** 部位名 */
    fun slotName(): String = when(slot) {
        SLOT_HEAD -> "頭"
        SLOT_BODY -> "胴"
        SLOT_ARM  -> "腕"
        SLOT_WST  -> "腰"
        SLOT_LEG  -> "脚"
        else -> ""
    }
    
    /** 指定スキル系統のポイントを取得 */
    fun getSkillPoint(systemName: String): Int {
        return skillPoints.firstOrNull { it.first == systemName }?.second ?: 0
    }
}

/**
 * 装飾品1件
 */
data class Decoration(
    val name: String,
    val rare: Int,
    val slotCost: Int,          // 必要スロット数
    val hrRank: Int,            // 入手時期HR
    val villageRank: Int,       // 入手時期村☆
    val skill1: String,
    val skill1Point: Int,
    val skill2: String?,
    val skill2Point: Int,
    val materials: List<Triple<String, Int, Boolean>>  // (素材名, 個数, isB)
) {
    fun getSkillPoint(systemName: String): Int {
        return when (systemName) {
            skill1 -> skill1Point
            skill2 -> skill2Point
            else -> 0
        }
    }
}

/**
 * お守り1件
 */
data class Charm(
    val id: Int,                // 通し番号（識別用）
    val typeName: String,       // お守り種類名（例：闘士の護石）
    val rare: Int,
    val questRank: Int,         // 入手時期(集☆)
    val slotCount: Int,         // スロット数
    val skillPoints: List<Pair<String, Int>>  // (系統名, ポイント) 最大2個
) {
    fun slotStr(): String = "○".repeat(slotCount) + "－".repeat(3 - slotCount)
    
    fun getSkillPoint(systemName: String): Int {
        return skillPoints.firstOrNull { it.first == systemName }?.second ?: 0
    }
    
    fun displayName(): String {
        val sb = StringBuilder(typeName)
        if (skillPoints.isNotEmpty()) {
            sb.append(" [")
            sb.append(skillPoints.joinToString(",") { "${it.first}${if (it.second >= 0) "+${it.second}" else "${it.second}"}" })
            sb.append("]")
        }
        if (slotCount > 0) sb.append(" ${slotStr()}")
        return sb.toString()
    }
}

/**
 * ユーザーが登録したお守りリスト管理
 */
class UserCharmList {
    private val charms = mutableListOf<Charm>()
    private var nextId = 1
    
    fun add(charm: Charm): Charm {
        val c = charm.copy(id = nextId++)
        charms.add(c)
        return c
    }
    
    fun remove(charm: Charm) { charms.remove(charm) }
    fun getAll(): List<Charm> = charms.toList()
    fun clear() { charms.clear() }
}
