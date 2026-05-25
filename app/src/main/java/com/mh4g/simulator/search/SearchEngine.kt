package com.mh4g.simulator.search

import com.mh4g.simulator.data.*
import kotlin.math.min

/**
 * 防具検索エンジン（PC版 d.aパッケージ相当）
 * ビットマスク法による高速検索実装
 */
class SearchEngine {

    private var cancelled = false
    fun cancel() { cancelled = true }

    /**
     * メイン検索
     * @return 検索結果リスト（空きスロット検索含む）
     */
    fun search(
        cond: SearchCondition,
        onProgress: ((Int) -> Unit)? = null
    ): SearchResults {
        cancelled = false
        val data = AppData

        // スキル系統→必要ポイント マップ作成
        val targetSystems = resolveTargetSystems(cond.targetSkills)
        val excludeSystems = resolveExcludeSystems(cond.excludeSkills)

        if (targetSystems.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList())

        // 各部位の候補リストを構築
        val headCands = buildCandidates(data.headList, cond)
        val bodyCands = buildCandidates(data.bodyList, cond)
        val armCands  = buildCandidates(data.armList,  cond)
        val wstCands  = buildCandidates(data.wstList,  cond)
        val legCands  = buildCandidates(data.legList,  cond)

        // お守り候補
        val charmCands: List<Charm?> = if (cond.useCharms) {
            listOf(null) + data.userCharms.getAll()
        } else listOf(null)

        // 固定装備がある場合、固定で上書き
        val fixedHead = if (cond.useExclude) data.pinnedEquip[SLOT_HEAD] else null
        val fixedBody = if (cond.useExclude) data.pinnedEquip[SLOT_BODY] else null
        val fixedArm  = if (cond.useExclude) data.pinnedEquip[SLOT_ARM]  else null
        val fixedWst  = if (cond.useExclude) data.pinnedEquip[SLOT_WST]  else null
        val fixedLeg  = if (cond.useExclude) data.pinnedEquip[SLOT_LEG]  else null

        val effectiveHead = if (fixedHead != null) listOf(fixedHead) else headCands
        val effectiveBody = if (fixedBody != null) listOf(fixedBody) else bodyCands
        val effectiveArm  = if (fixedArm  != null) listOf(fixedArm)  else armCands
        val effectiveWst  = if (fixedWst  != null) listOf(fixedWst)  else wstCands
        val effectiveLeg  = if (fixedLeg  != null) listOf(fixedLeg)  else legCands

        val results = mutableListOf<SearchResult>()
        val systemNames = targetSystems.keys.toList()

        // 武器スロット数
        val weaponSlots = when {
            cond.weaponSlotAuto -> listOf(0, 1, 2, 3)
            else -> listOf(maxOf(0, minOf(3, cond.weaponSlot)))
        }

        var checked = 0
        val total = effectiveHead.size.toLong() * effectiveBody.size *
                effectiveArm.size * effectiveWst.size * effectiveLeg.size

        outer@ for (head in effectiveHead) {
            for (body in effectiveBody) {
                for (arm in effectiveArm) {
                    if (cancelled) break@outer
                    for (wst in effectiveWst) {
                        for (leg in effectiveLeg) {
                            for (charm in charmCands) {
                                if (cancelled) break@outer

                                val slotCount = (head?.slotCount ?: 0) +
                                        (body?.slotCount ?: 0) +
                                        (arm?.slotCount ?: 0) +
                                        (wst?.slotCount ?: 0) +
                                        (leg?.slotCount ?: 0) +
                                        (charm?.slotCount ?: 0)

                                for (weapSl in weaponSlots) {
                                    val totalSlots = slotCount + weapSl
                                    // スキルポイント合算
                                    val ptMap = calcSkillPoints(head, body, arm, wst, leg, charm)

                                    // 装飾品で目標スキルを達成できるか試みる
                                    val decoResult = tryFillDecos(ptMap, targetSystems, excludeSystems, totalSlots)
                                    if (decoResult != null) {
                                        val result = buildResult(head, body, arm, wst, leg, charm,
                                            decoResult, weapSl, targetSystems, excludeSystems)
                                        if (result != null) {
                                            results.add(result)
                                            if (results.size >= cond.maxResults) break@outer
                                        }
                                    }
                                }
                            }
                            checked++
                            if (checked % 10000 == 0) {
                                onProgress?.invoke((checked * 100 / maxOf(1, total)).toInt())
                            }
                        }
                    }
                }
            }
        }

        // ソート：装備なし数 → 最終防御力 → 耐性合計
        results.sortWith(compareBy(
            { it.noEquipCount() },
            { -it.totalDefMax },
            { -(it.totalResFire + it.totalResWater + it.totalResThunder + it.totalResIce + it.totalResDragon) }
        ))

        // 空きスロットパターン計算
        val resultsWithEmpty = if (results.isNotEmpty()) {
            results.map { r ->
                r.copy(emptySlotPatterns = calcEmptySlotPatterns(r, excludeSystems))
            }
        } else results

        // お守り検索（結果0件かつcond.searchCharmがtrue）
        val charmSuggestions = if (results.isEmpty() && cond.searchCharm) {
            searchRequiredCharms(cond, targetSystems, excludeSystems)
        } else emptyList()

        return SearchResults(resultsWithEmpty, emptyList(), charmSuggestions)
    }

    /** スキル名リスト → スキル系統→必要ポイント マップ */
    private fun resolveTargetSystems(skillNames: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (skillName in skillNames) {
            val skill = AppData.skillByName[skillName] ?: continue
            // 最低でもskill.pointが必要（絶対値で）
            val existing = map[skill.system]
            if (existing == null || skill.point > (existing)) {
                map[skill.system] = skill.point
            }
        }
        return map
    }

    /** 除外スキル → スキル系統→除外閾値ポイント マップ */
    private fun resolveExcludeSystems(skillNames: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (skillName in skillNames) {
            val skill = AppData.skillByName[skillName] ?: continue
            // マイナスポイントのスキルを除外
            val existing = map[skill.system]
            if (existing == null || skill.point < existing) {
                map[skill.system] = skill.point  // 負の値
            }
        }
        return map
    }

    /** 候補防具リストを構築（フィルタリング） */
    private fun buildCandidates(list: List<Equipment>, cond: SearchCondition): List<Equipment?> {
        val result = mutableListOf<Equipment?>(null) // nullは「なし」
        for (equip in list) {
            // 除外チェック
            if (cond.useExclude && AppData.isExcluded(equip)) continue
            // 性別フィルタ
            if (cond.gender != 0 && equip.gender != 0 && equip.gender != cond.gender) continue
            // 剣士/ガンナーフィルタ
            if (cond.armorType != 0 && equip.type != 0 && equip.type != cond.armorType) continue
            result.add(equip)
        }
        return result
    }

    /** 装備一式のスキルポイント合算 */
    private fun calcSkillPoints(vararg equips: Any?): MutableMap<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (equip in equips) {
            val pts: List<Pair<String, Int>> = when (equip) {
                is Equipment -> equip.skillPoints
                is Charm -> equip.skillPoints
                else -> emptyList()
            }
            for ((sysName, pt) in pts) {
                map[sysName] = (map[sysName] ?: 0) + pt
            }
        }
        return map
    }

    /**
     * 装飾品で不足ポイントを補えるか試みる
     * @return 使用装飾品リスト、または達成不可ならnull
     */
    private fun tryFillDecos(
        currentPts: Map<String, Int>,
        targetSystems: Map<String, Int>,
        excludeSystems: Map<String, Int>,
        totalSlots: Int
    ): List<Decoration?>? {
        // まず除外スキルチェック（装飾品なしで既に除外されていたらアウト）
        for ((sysName, threshold) in excludeSystems) {
            val cur = currentPts[sysName] ?: 0
            if (threshold < 0 && cur <= threshold) return null
        }

        // 不足ポイントを計算
        val needed = mutableMapOf<String, Int>()
        for ((sysName, reqPt) in targetSystems) {
            val cur = currentPts[sysName] ?: 0
            if (cur < reqPt) needed[sysName] = reqPt - cur
        }

        if (needed.isEmpty()) {
            // 既に全スキル達成
            return emptyList()
        }

        // 貪欲法で装飾品選択（スロット制限内）
        val usedDecos = mutableListOf<Decoration?>()
        val ptMap = currentPts.toMutableMap()
        var slotsLeft = totalSlots

        // 目標スキルの装飾品を優先的に使用
        for ((sysName, reqPt) in needed.entries.sortedByDescending { it.value }) {
            val availableDecos = AppData.decoBySystem[sysName]
                ?.sortedByDescending { it.getSkillPoint(sysName) }
                ?: continue

            while ((ptMap[sysName] ?: 0) < reqPt && slotsLeft > 0) {
                val deco = availableDecos.firstOrNull { it.slotCost <= slotsLeft } ?: break
                usedDecos.add(deco)
                slotsLeft -= deco.slotCost
                for ((s, p) in listOfNotNull(
                    Pair(deco.skill1, deco.skill1Point),
                    deco.skill2?.let { Pair(it, deco.skill2Point) }
                )) {
                    ptMap[s] = (ptMap[s] ?: 0) + p
                }
            }

            if ((ptMap[sysName] ?: 0) < reqPt) return null // 達成不可
        }

        // 除外スキルが装飾品効果で発動してしまわないかチェック
        for ((sysName, threshold) in excludeSystems) {
            val cur = ptMap[sysName] ?: 0
            if (threshold < 0 && cur <= threshold) return null
        }

        return usedDecos
    }

    /** SearchResultを構築 */
    private fun buildResult(
        head: Equipment?, body: Equipment?, arm: Equipment?,
        wst: Equipment?, leg: Equipment?, charm: Charm?,
        decos: List<Decoration?>, weaponSlot: Int,
        targetSystems: Map<String, Int>,
        excludeSystems: Map<String, Int>
    ): SearchResult? {
        val ptMap = calcSkillPoints(head, body, arm, wst, leg, charm)
        for (deco in decos) {
            deco ?: continue
            ptMap[deco.skill1] = (ptMap[deco.skill1] ?: 0) + deco.skill1Point
            deco.skill2?.let { s2 ->
                ptMap[s2] = (ptMap[s2] ?: 0) + deco.skill2Point
            }
        }

        // 発動スキルリスト生成
        val activated = mutableListOf<ActivatedSkill>()
        for ((sysName, totalPt) in ptMap) {
            val sysSkills = AppData.skillsBySystem[sysName] ?: continue
            for (skill in sysSkills) {
                val activates = if (skill.point > 0) totalPt >= skill.point
                else totalPt <= skill.point
                if (activates) {
                    activated.add(ActivatedSkill(
                        skill.name, sysName, totalPt,
                        targetSystems.containsKey(sysName)
                    ))
                }
            }
        }

        val defMax = listOf(head, body, arm, wst, leg).sumOf { it?.defMax ?: 0 }

        // 使用スロット数
        val usedSlots = decos.sumOf { it?.slotCost ?: 0 }
        val totalSlots = (head?.slotCount ?: 0) + (body?.slotCount ?: 0) +
                (arm?.slotCount ?: 0) + (wst?.slotCount ?: 0) +
                (leg?.slotCount ?: 0) + (charm?.slotCount ?: 0) + weaponSlot
        val freeSlots = totalSlots - usedSlots

        return SearchResult(
            head = head, body = body, arm = arm, wst = wst, leg = leg,
            charm = charm, decos = decos, weaponSlotUsed = weaponSlot,
            activatedSkills = activated,
            totalDefMax = defMax,
            totalResFire    = listOf(head, body, arm, wst, leg).sumOf { it?.resFire    ?: 0 },
            totalResWater   = listOf(head, body, arm, wst, leg).sumOf { it?.resWater   ?: 0 },
            totalResThunder = listOf(head, body, arm, wst, leg).sumOf { it?.resThunder ?: 0 },
            totalResIce     = listOf(head, body, arm, wst, leg).sumOf { it?.resIce     ?: 0 },
            totalResDragon  = listOf(head, body, arm, wst, leg).sumOf { it?.resDragon  ?: 0 },
            freeSlots = freeSlots
        )
    }

    /** 空きスロット活用パターンを計算 */
    private fun calcEmptySlotPatterns(
        result: SearchResult,
        excludeSystems: Map<String, Int>
    ): List<EmptySlotPattern> {
        if (result.freeSlots <= 0) return emptyList()

        val basePtMap = calcSkillPoints(result.head, result.body, result.arm,
            result.wst, result.leg, result.charm)
        for (deco in result.decos) {
            deco ?: continue
            basePtMap[deco.skill1] = (basePtMap[deco.skill1] ?: 0) + deco.skill1Point
            deco.skill2?.let { s2 ->
                basePtMap[s2] = (basePtMap[s2] ?: 0) + deco.skill2Point
            }
        }

        // 空きスロットに入れられる装飾品の全パターン（最大20パターン）
        val patterns = mutableListOf<EmptySlotPattern>()
        generateDecoPatterns(result.freeSlots, emptyList(), basePtMap, excludeSystems, patterns, 20)
        return patterns.sortedByDescending { it.score }.take(10)
    }

    private fun generateDecoPatterns(
        slotsLeft: Int,
        current: List<Decoration?>,
        basePtMap: Map<String, Int>,
        excludeSystems: Map<String, Int>,
        results: MutableList<EmptySlotPattern>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        if (slotsLeft <= 0) {
            results.add(buildEmptyPattern(current, basePtMap, excludeSystems))
            return
        }

        val usableDeco = AppData.decoList.filter { it.slotCost <= slotsLeft }
        if (usableDeco.isEmpty()) {
            results.add(buildEmptyPattern(current, basePtMap, excludeSystems))
            return
        }

        // 重複を避けるため最後の装飾品以降のみ追加
        val startIdx = if (current.isEmpty()) 0
        else AppData.decoList.indexOf(current.lastOrNull { it != null })
            .coerceAtLeast(0)

        var added = false
        for (i in startIdx until min(AppData.decoList.size, startIdx + 30)) {
            if (results.size >= maxResults) break
            val deco = AppData.decoList[i]
            if (deco.slotCost > slotsLeft) continue
            generateDecoPatterns(slotsLeft - deco.slotCost,
                current + deco, basePtMap, excludeSystems, results, maxResults)
            added = true
        }
        if (!added) results.add(buildEmptyPattern(current, basePtMap, excludeSystems))
    }

    private fun buildEmptyPattern(
        decos: List<Decoration?>,
        basePtMap: Map<String, Int>,
        excludeSystems: Map<String, Int>
    ): EmptySlotPattern {
        val ptMap = basePtMap.toMutableMap()
        for (deco in decos) {
            deco ?: continue
            ptMap[deco.skill1] = (ptMap[deco.skill1] ?: 0) + deco.skill1Point
            deco.skill2?.let { s2 ->
                ptMap[s2] = (ptMap[s2] ?: 0) + deco.skill2Point
            }
        }
        val additional = mutableListOf<ActivatedSkill>()
        val minus = mutableListOf<ActivatedSkill>()
        for ((sysName, totalPt) in ptMap) {
            val sysSkills = AppData.skillsBySystem[sysName] ?: continue
            for (skill in sysSkills) {
                val activates = if (skill.point > 0) totalPt >= skill.point
                else totalPt <= skill.point
                if (activates && skill.point > 0) {
                    additional.add(ActivatedSkill(skill.name, sysName, totalPt, false))
                } else if (activates && skill.point < 0) {
                    minus.add(ActivatedSkill(skill.name, sysName, totalPt, false))
                }
            }
        }
        val score = additional.size * 10 - minus.size * 5
        return EmptySlotPattern(decos, additional, minus, score)
    }

    /** お守り検索（結果0件時に発動するお守り候補） */
    private fun searchRequiredCharms(
        cond: SearchCondition,
        targetSystems: Map<String, Int>,
        excludeSystems: Map<String, Int>
    ): List<Charm> {
        // 最低限必要なお守りのスキルポイント組み合わせを探索
        val suggestions = mutableListOf<Charm>()
        // 最大3スロット、最大2スキル系統の組み合わせを試す
        for (slotCount in 0..3) {
            for (systemEntry in targetSystems.entries.take(2)) {
                val neededPt = systemEntry.value
                for (pt in listOf(neededPt, neededPt - 1, neededPt - 2).filter { it > 0 }) {
                    suggestions.add(Charm(
                        id = -1,
                        typeName = "（候補お守り）",
                        rare = 0,
                        questRank = 0,
                        slotCount = slotCount,
                        skillPoints = listOf(Pair(systemEntry.key, pt))
                    ))
                }
            }
        }
        return suggestions.distinctBy { "${it.slotCount}:${it.skillPoints}" }.take(10)
    }
}

data class SearchResults(
    val results: List<SearchResult>,
    val emptyPatternResults: List<SearchResult>,  // 空きスロットパターン展開後
    val charmSuggestions: List<Charm>             // お守り候補（0件時）
)
