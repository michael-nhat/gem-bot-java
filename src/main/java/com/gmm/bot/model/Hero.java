package com.gmm.bot.model;


import com.gmm.bot.enumeration.GemType;
import com.gmm.bot.enumeration.HeroIdEnum;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class Hero {
    private int playerId;
    private HeroIdEnum id;
    private String name;
    private List<GemType> gemTypes = new ArrayList<>();
    private int maxHp; // Hp
    private int maxMana; // Mp
    private int attack;
    private int hp;
    private int mana;
    private int dameEatSword;
    private int dameCastSkill;
    private int dameThisTurn;

    public Hero(ISFSObject objHero) {
        this.playerId = objHero.getInt("playerId");
        this.id = HeroIdEnum.from(objHero.getUtfString("id"));
        this.name = id.name();
        this.attack = objHero.getInt("attack");
        this.hp = objHero.getInt("hp");
        this.mana = objHero.getInt("mana");
        this.maxMana = objHero.getInt("maxMana");
        ISFSArray arrGemTypes = objHero.getSFSArray("gemTypes");
        for (int i = 0; i < arrGemTypes.size(); i++) {
            this.gemTypes.add(GemType.from(arrGemTypes.getUtfString(i)));
        }
    }

    public void updateHero(ISFSObject objHero) {
        this.attack = objHero.getInt("attack");
        this.hp = objHero.getInt("hp");
        this.mana = objHero.getInt("mana");
        this.maxMana = objHero.getInt("maxMana");
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public boolean isFullMana() {
        return mana >= maxMana;
    }

    public boolean isHeroSelfSkill() {
        return Arrays.asList(HeroIdEnum.SEA_SPIRIT).contains(id);
    }

    private void calculatorDameEatSword() {

    }
}
