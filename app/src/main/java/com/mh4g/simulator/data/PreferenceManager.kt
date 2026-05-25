package com.mh4g.simulator.data

import android.content.Context
import android.content.SharedPreferences
import com.mh4g.simulator.search.SearchCondition
import org.json.JSONArray
import org.json.JSONObject

/**
 * 検索条件・マイセットをSharedPreferencesで永続化
 */
object PreferenceManager {

    private const val PREF_NAME = "mh4g_simulator"
    private const val KEY_CONDITION = "last_condition"
    private const val KEY_MYSET = "my_set_list"
    private const val KEY_CHARMS = "user_charms"
    private const val KEY_EXCLUDED_EQUIP = "excluded_equip"
    private const val KEY_EXCLUDED_DECO = "excluded_deco"
    private const val KEY_PINNED = "pinned_equip"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ============================================================
    // 検索条件
    // ============================================================
    fun saveCondition(context: Context, cond: SearchCondition) {
        val json = JSONObject().apply {
            put("gender", cond.gender)
            put("armorType", cond.armorType)
            put("weaponSlotAuto", cond.weaponSlotAuto)
            put("weaponSlot", cond.weaponSlot)
            put("useExclude", cond.useExclude)
            put("searchCharm", cond.searchCharm)
            put("targetSkills", JSONArray(cond.targetSkills))
            put("excludeSkills", JSONArray(cond.excludeSkills))
        }
        prefs(context).edit().putString(KEY_CONDITION, json.toString()).apply()
    }

    fun loadCondition(context: Context): SearchCondition {
        val str = prefs(context).getString(KEY_CONDITION, null) ?: return SearchCondition()
        return try {
            val json = JSONObject(str)
            SearchCondition(
                gender = json.optInt("gender", 0),
                armorType = json.optInt("armorType", 0),
                weaponSlotAuto = json.optBoolean("weaponSlotAuto", true),
                weaponSlot = json.optInt("weaponSlot", -1),
                useExclude = json.optBoolean("useExclude", true),
                searchCharm = json.optBoolean("searchCharm", true),
                targetSkills = json.optJSONArray("targetSkills")
                    ?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
                excludeSkills = json.optJSONArray("excludeSkills")
                    ?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            )
        } catch (e: Exception) { SearchCondition() }
    }

    // ============================================================
    // マイセット
    // ============================================================
    fun saveMySetList(context: Context) {
        val arr = JSONArray()
        for (mySet in AppData.mySetList) {
            val obj = JSONObject().apply {
                put("label", mySet.label)
                put("note", mySet.note)
                put("head", mySet.head?.name)
                put("body", mySet.body?.name)
                put("arm",  mySet.arm?.name)
                put("wst",  mySet.wst?.name)
                put("leg",  mySet.leg?.name)
                put("weaponSlot", mySet.weaponSlot)
                put("skills", JSONArray(mySet.activatedSkills))
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_MYSET, arr.toString()).apply()
    }

    fun loadMySetList(context: Context) {
        val str = prefs(context).getString(KEY_MYSET, null) ?: return
        try {
            val arr = JSONArray(str)
            AppData.mySetList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val head = obj.optString("head")?.let { n -> AppData.headList.firstOrNull { it.name == n } }
                val body = obj.optString("body")?.let { n -> AppData.bodyList.firstOrNull { it.name == n } }
                val arm  = obj.optString("arm")?.let  { n -> AppData.armList.firstOrNull  { it.name == n } }
                val wst  = obj.optString("wst")?.let  { n -> AppData.wstList.firstOrNull  { it.name == n } }
                val leg  = obj.optString("leg")?.let  { n -> AppData.legList.firstOrNull  { it.name == n } }
                val skills = obj.optJSONArray("skills")
                    ?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
                AppData.mySetList.add(MySet(
                    label = obj.optString("label", ""),
                    note  = obj.optString("note", ""),
                    head = head, body = body, arm = arm, wst = wst, leg = leg,
                    charm = null, decos = emptyList(),
                    weaponSlot = obj.optInt("weaponSlot", 0),
                    activatedSkills = skills
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ============================================================
    // 登録お守り
    // ============================================================
    fun saveUserCharms(context: Context) {
        val arr = JSONArray()
        for (charm in AppData.userCharms.getAll()) {
            val obj = JSONObject().apply {
                put("typeName", charm.typeName)
                put("rare", charm.rare)
                put("questRank", charm.questRank)
                put("slotCount", charm.slotCount)
                val pts = JSONArray()
                charm.skillPoints.forEach { (s, p) ->
                    pts.put(JSONObject().put("system", s).put("point", p))
                }
                put("skillPoints", pts)
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_CHARMS, arr.toString()).apply()
    }

    fun loadUserCharms(context: Context) {
        val str = prefs(context).getString(KEY_CHARMS, null) ?: return
        try {
            val arr = JSONArray(str)
            AppData.userCharms.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pts = obj.optJSONArray("skillPoints")?.let { a ->
                    (0 until a.length()).map {
                        val p = a.getJSONObject(it)
                        Pair(p.getString("system"), p.getInt("point"))
                    }
                } ?: emptyList()
                AppData.userCharms.add(Charm(
                    id = 0,
                    typeName = obj.optString("typeName", ""),
                    rare = obj.optInt("rare", 1),
                    questRank = obj.optInt("questRank", 1),
                    slotCount = obj.optInt("slotCount", 0),
                    skillPoints = pts
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ============================================================
    // 除外・固定状態
    // ============================================================
    fun saveExcludeState(context: Context) {
        prefs(context).edit().apply {
            putStringSet(KEY_EXCLUDED_EQUIP, AppData.excludedEquip)
            putStringSet(KEY_EXCLUDED_DECO, AppData.excludedDecos)
            // 固定防具
            val pinnedObj = JSONObject()
            AppData.pinnedEquip.forEach { (slot, equip) ->
                if (equip != null) pinnedObj.put("$slot", equip.name)
            }
            putString(KEY_PINNED, pinnedObj.toString())
        }.apply()
    }

    fun loadExcludeState(context: Context) {
        val p = prefs(context)
        AppData.excludedEquip.clear()
        AppData.excludedEquip.addAll(p.getStringSet(KEY_EXCLUDED_EQUIP, emptySet()) ?: emptySet())
        AppData.excludedDecos.clear()
        AppData.excludedDecos.addAll(p.getStringSet(KEY_EXCLUDED_DECO, emptySet()) ?: emptySet())
        // 固定
        val pinnedStr = p.getString(KEY_PINNED, null)
        if (pinnedStr != null) {
            try {
                val obj = JSONObject(pinnedStr)
                for (slotStr in listOf("0","1","2","3","4")) {
                    if (!obj.has(slotStr)) continue
                    val name = obj.getString(slotStr)
                    val slot = slotStr.toInt()
                    val equip = AppData.getEquipList(slot).firstOrNull { it.name == name }
                    AppData.pinnedEquip[slot] = equip
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
