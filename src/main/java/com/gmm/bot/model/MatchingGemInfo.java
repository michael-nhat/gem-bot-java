package com.gmm.bot.model;

import com.gmm.bot.enumeration.GemModifier;
import com.gmm.bot.enumeration.GemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class MatchingGemInfo {
    private int sizeMatch;
    private GemType type;
    private List<GemModifier> gemModifierList;
}
