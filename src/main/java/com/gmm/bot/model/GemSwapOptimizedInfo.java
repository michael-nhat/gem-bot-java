package com.gmm.bot.model;

import com.gmm.bot.enumeration.GemModifier;
import com.gmm.bot.enumeration.GemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
@Setter
public class GemSwapOptimizedInfo {
    private int index1;
    private int index2;
    private List<MatchingGemInfo> matchingGemInfoList;

    public Pair<Integer> getIndexSwapGem() {
        return new Pair<>(index1, index2);
    }

    public int getPriorityScore(Set<GemType> myHeroGemType, Set<GemType> enemyHeroGemType) {
        int result = 0;
        for (MatchingGemInfo matchingGemInfo : matchingGemInfoList) {
            int size = matchingGemInfo.getSizeMatch();
            if (size >= 5) result += 20;
            if (myHeroGemType.contains(matchingGemInfo.getType())) {
                result += size * 4;
            } else if (matchingGemInfo.getType() == GemType.SWORD) {
                result += size * 3;
            }
            if (enemyHeroGemType.contains(matchingGemInfo.getType())) {
                result += size;
            }
            for (GemModifier gemModifier : matchingGemInfo.getGemModifierList()) {
                result += gemModifier.getPriorityScore();
            }
        }
        return result;
    }
}
