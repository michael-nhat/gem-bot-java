package com.gmm.bot.model;

import com.gmm.bot.enumeration.GemType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
public class Player {
    private int id;
    private String displayName;
    private List<Hero> heroes;
    private Set<GemType> heroGemType;
    private boolean firstHero;

    public Player(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        heroes = new ArrayList<>();
        heroGemType = new LinkedHashSet<>();
    }

    public Optional<Hero> anyHeroFullMana() {
        return heroes.stream().filter(hero -> hero.isAlive() && hero.isFullMana()).findFirst();
    }

    public List<Hero> herosFullMana() {
        return heroes.stream().filter(hero -> hero.isAlive() && hero.isFullMana()).collect(Collectors.toList());
    }

    public Hero firstHeroAlive() {
        return heroes.stream().filter(Hero::isAlive).findFirst().orElse(null);
    }

    public List<Hero> herosAlive() {
        return heroes.stream().filter(Hero::isAlive).collect(Collectors.toList());
    }

    public Hero dameHeroHighest() {
        return herosAlive().stream().max(Comparator.comparingInt(Hero::getAttack)).orElse(null);
    }

    public Hero hpHeroHighest() {
        return heroes.stream().max(Comparator.comparingInt(Hero::getHp)).orElse(null);
    }

    public List<Hero> sortedByHPDesc() {
        return heroes.stream().sorted(Comparator.comparingInt(Hero::getHp)).collect(Collectors.toList());
    }

    public Hero hpHeroLowest() {
        return herosAlive().stream().min(Comparator.comparingInt(Hero::getHp)).orElse(null);
    }

    public Set<GemType> getRecommendGemType() {
        heroGemType.clear();
        heroes.stream().filter(Hero::isAlive).forEach(hero -> heroGemType.addAll(hero.getGemTypes()));
        return heroGemType;
    }

    public List<Hero> heroesBuff() {
        return herosAlive().stream().filter(hero -> hero.isFullMana() && hero.isHeroSelfSkill()).collect(Collectors.toList());
    }

    public List<Hero> heroesAttack() {
        return herosAlive().stream().filter(hero -> hero.isFullMana() && !hero.isHeroSelfSkill()).collect(Collectors.toList());
    }

    public List<Hero> listEnemyHeroAlive() {
        return heroes;
    }
}
