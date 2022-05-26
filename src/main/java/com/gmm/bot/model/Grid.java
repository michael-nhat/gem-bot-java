package com.gmm.bot.model;

import com.gmm.bot.ai.ConstantCommand;
import com.gmm.bot.enumeration.GemModifier;
import com.gmm.bot.enumeration.GemType;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class Grid {
    private List<Gem> gems = new ArrayList<>();
    private Set<GemType> gemTypes = new HashSet<>();
    private Set<GemType> myHeroGemType;
    private Set<Gem> specialGem = new HashSet<>();
    private Player player;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Grid(ISFSArray gemsCode,ISFSArray gemModifiers, Set<GemType> heroGemType) {
        updateGems(gemsCode,gemModifiers);
        this.myHeroGemType = heroGemType;
    }

    public void updateGems(ISFSArray gemsCode,ISFSArray gemModifiers ) {
        gems.clear();
        gemTypes.clear();
        specialGem.clear();
        if(gemModifiers != null){
            for (int i = 0; i < gemsCode.size(); i++) {
                Gem gem = new Gem(i, GemType.from(gemsCode.getByte(i)), GemModifier.from(gemModifiers.getByte(i)));
                if (gem.getModifier() != null && gem.getModifier() != GemModifier.NONE) {
                    specialGem.add(gem);
                }
                gems.add(gem);
                gemTypes.add(gem.getType());
            }
        } else {
            for (int i = 0; i < gemsCode.size(); i++) {
                Gem gem = new Gem(i, GemType.from(gemsCode.getByte(i)));
                gems.add(gem);
                gemTypes.add(gem.getType());
            }
        }

    }


    public Pair<Integer> recommendSwapGem(Player botPlayer, Player enemyPlayer) {
        log.info("recommendSwapGem: botPlayer gemType={}", botPlayer.getHeroGemType());
        List<Hero> botHeroes = botPlayer.getHeroes();
        for (Hero h : botHeroes) {
            log.info("Hero: botPlayer id={} name={} attack={} hp/maxHp={}/{} mana/maxMana={}/{} gemType={} playerId={}", h.getId(), h.getName(), h.getAttack(),
                    h.getHp(), h.getMaxHp(), h.getMana(), h.getMaxMana(), h.getGemTypes(), h.getPlayerId());
        }
        List<GemSwapInfo> listMatchGem = suggestMatch();
        if (listMatchGem.isEmpty()) {
            return new Pair<>(-1, -1);
        }
        List<Hero> heroes = player.getHeroes();

        List<GemType> prioritizeGem = new ArrayList<>(Arrays.asList(GemType.YELLOW, GemType.GREEN, GemType.BLUE, GemType.EMPTY, GemType.RED, GemType.PURPLE));

        Collections.shuffle(prioritizeGem);
        for (Hero hero : heroes) {
            if ((hero.isAlive() && hero.isFullMana()) || !hero.isAlive()) {
                logger.info("prioritizeGem: {}", prioritizeGem);
                logger.info("hero.getGemTypes(): {}", hero.getGemTypes());
                prioritizeGem.removeAll(hero.getGemTypes());
            }
        }

        List<GemSwapInfo> matchGemSizeThanFive =
                listMatchGem.stream().filter(gemMatch -> {
                    if (gemMatch.getSizeMatch() >= 5 &&  (prioritizeGem.contains(gemMatch.getType())
                            || myHeroGemType.contains(gemMatch.getType()))) {
                        return true;
                    } else return gemMatch.getSizeMatch() >= 5;
                }).collect(Collectors.toList());

        List<GemType> matchGemTypeSizeThanFive = matchGemSizeThanFive.stream().map(GemSwapInfo::getType).collect(Collectors.toList());

        List<GemType> gemTypeList = specialGem.stream().map(Gem::getType).collect(Collectors.toList());
        logger.info("gemTypeList: {}", gemTypeList);
        List<GemModifier> gemModifierList = Arrays.asList(GemModifier.values());
        GemSwapInfo gemSwapInfoExtraTurn = matchGemSizeThanFive
                .stream()
                .filter(matchGemSizeThanFour -> gemModifierList.contains(matchGemSizeThanFour.getGemModifier()))
                .findFirst()
                .orElse(null);

        logger.info("special gem: {}, name: {}", gemSwapInfoExtraTurn, gemSwapInfoExtraTurn == null
                ? null : gemSwapInfoExtraTurn.getGemModifier().getCode());

        List<GemSwapInfo> gemSwapInfoSpecial = matchGemSizeThanFive
                .stream()
                .filter(matchGemSizeThanFour -> gemTypeList.contains(matchGemSizeThanFour.getType()))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(matchGemSizeThanFive)) {
            if ((gemSwapInfoExtraTurn != null && matchGemTypeSizeThanFive.contains(gemSwapInfoExtraTurn.getType()))
                    || !CollectionUtils.isEmpty(gemSwapInfoSpecial)) {
                logger.info("DO_SPECIAL_SWAP");
                return matchGemSizeThanFive.get(0).getIndexSwapGem();
            }
            return matchGemSizeThanFive.get(0).getIndexSwapGem();
        }
        logger.info("DO_SWAP");
        myHeroGemType = myHeroGemType.stream().filter(heroGemType -> {
            Hero hero = heroes.stream().filter(hero1 -> hero1.getGemTypes().contains(heroGemType)).findFirst().orElse(null);
            return hero != null;
        }).collect(Collectors.toSet());
        List<GemSwapInfo> matchGemSizeThanThree =
                listMatchGem.stream().filter(gemMatch -> {
                    if (gemMatch.getSizeMatch() >= 3 && (prioritizeGem.contains(gemMatch.getType())
                            || myHeroGemType.contains(gemMatch.getType())) ) {
                        return true;
                    } else return gemMatch.getSizeMatch() >= 3;
                }).collect(Collectors.toList());

        List<GemType> matchGemTypeSizeThanThree = matchGemSizeThanThree.stream().map(GemSwapInfo::getType).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(matchGemSizeThanThree)) {
            if ((gemSwapInfoExtraTurn != null && matchGemTypeSizeThanThree.contains(gemSwapInfoExtraTurn.getType()))
                    || !CollectionUtils.isEmpty(gemSwapInfoSpecial)) {
                logger.info("DO_SPECIAL_SWAP");
                return matchGemSizeThanThree.get(0).getIndexSwapGem();
            }
            return matchGemSizeThanThree.get(0).getIndexSwapGem();
        }
        for (Gem g: specialGem) {
            log.info("specialGem:  {} {} {} {} {}", g.getIndex(), g.getType(), g.getX(), g.getY(), g.getModifier());
        }
        Gem gemSpecial = specialGem.stream().filter(g -> Arrays.asList(GemModifier.values()).contains(g.getModifier())).findFirst().orElse(null);
        List<GemSwapInfo> listExtraturn = listMatchGem.stream().filter(g-> gemSpecial != null
                && (g.getGemModifier() == gemSpecial.getModifier())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(listExtraturn)) {
            log.info("gem special turn from index1={} to index2={} with gemType={} and gemModifier={}",
                    listExtraturn.get(0).getIndex1(), listExtraturn.get(0).getIndex2(), listExtraturn.get(0).getType(), listExtraturn.get(0).getGemModifier());
            return listExtraturn.get(0).getIndexSwapGem();
        }
        Optional<GemSwapInfo> match5 =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getSizeMatch() > 4).findFirst();
        if (match5.isPresent()) {
            log.info("match5 from index1={} to index2={} with gemType={}", match5.get().getIndex1(), match5.get().getIndex2(), match5.get().getType());
            return match5.get().getIndexSwapGem();
        }
        Optional<GemSwapInfo> match4 =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getSizeMatch() > 3).findFirst();
        if (match4.isPresent()) {
            log.info("match4 from index1={} to index2={} with gemType={}", match4.get().getIndex1(), match4.get().getIndex2(), match4.get().getType());
            return match4.get().getIndexSwapGem();
        }
        Optional<GemSwapInfo> matchGemSword =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getType() == GemType.SWORD).findFirst();
        if (matchGemSword.isPresent()) {
            log.info("match sword from index1={} to index2={} with gemType={}", matchGemSword.get().getIndex1(), matchGemSword.get().getIndex2(), matchGemSword.get().getType());
            return matchGemSword.get().getIndexSwapGem();
        }
        for (GemType type : botPlayer.getHeroGemType()) {
            Optional<GemSwapInfo> matchGem =
                    listMatchGem.stream().filter(gemMatch -> gemMatch.getType() == type).findFirst();
            if (matchGem.isPresent()) {
                log.info("match3 from index1={} to index2={} with gemType={}", matchGem.get().getIndex1(), matchGem.get().getIndex2(), matchGem.get().getType());
                return matchGem.get().getIndexSwapGem();
            }
        }
        log.info("normal from index1={} to index2={} with gemType={}", listMatchGem.get(0).getIndex1(), listMatchGem.get(0).getIndex2(), listMatchGem.get(0).getType());
        return listMatchGem.get(0).getIndexSwapGem();
    }

    public List<GemSwapInfo> suggestMatch() {
        List<GemSwapInfo> listMatchGem = new ArrayList<>();
        for (Gem currentGem : gems) {
            Gem swapGem = null;
            // If x > 0 => swap left & check
            if (currentGem.getX() > 0) {
                swapGem = gems.get(getGemIndexAt(currentGem.getX() - 1, currentGem.getY()));
                checkMatchSwapGem(listMatchGem, currentGem, swapGem);
            }
            // If x < 7 => swap right & check
            if (currentGem.getX() < 7) {
                swapGem = gems.get(getGemIndexAt(currentGem.getX() + 1, currentGem.getY()));
                checkMatchSwapGem(listMatchGem, currentGem, swapGem);
            }
            // If y < 7 => swap up & check
            if (currentGem.getY() < 7) {
                swapGem = gems.get(getGemIndexAt(currentGem.getX(), currentGem.getY() + 1));
                checkMatchSwapGem(listMatchGem, currentGem, swapGem);
            }
            // If y > 0 => swap down & check
            if (currentGem.getY() > 0) {
                swapGem = gems.get(getGemIndexAt(currentGem.getX(), currentGem.getY() - 1));
                checkMatchSwapGem(listMatchGem, currentGem, swapGem);
            }
        }
        return listMatchGem;
    }

    private void checkMatchSwapGem(List<GemSwapInfo> listMatchGem, Gem currentGem, Gem swapGem) {
        swap(currentGem, swapGem, gems);
        Set<Gem> matchGems = matchesAt(currentGem.getX(), currentGem.getY());
        swap(currentGem, swapGem, gems);
        if (!matchGems.isEmpty()) {
            listMatchGem.add(new GemSwapInfo(currentGem.getIndex(), swapGem.getIndex(), matchGems.size(), currentGem.getType(), currentGem.getModifier()));
        }
    }

    private int getGemIndexAt(int x, int y) {
        return x + y * 8;
    }

    private void swap(Gem a, Gem b, List<Gem> gems) {
        int tempIndex = a.getIndex();
        int tempX = a.getX();
        int tempY = a.getY();

        // update reference
        gems.set(a.getIndex(), b);
        gems.set(b.getIndex(), a);

        // update data of element
        a.setIndex(b.getIndex());
        a.setX(b.getX());
        a.setY(b.getY());

        b.setIndex(tempIndex);
        b.setX(tempX);
        b.setY(tempY);
    }

    private Set<Gem> matchesAt(int x, int y) {
        Set<Gem> res = new HashSet<>();
        Gem center = gemAt(x, y);
        if (center == null) {
            return res;
        }

        // check horizontally
        List<Gem> hor = new ArrayList<>();
        hor.add(center);
        int xLeft = x - 1, xRight = x + 1;
        while (xLeft >= 0) {
            Gem gemLeft = gemAt(xLeft, y);
            if (gemLeft != null) {
                if (!gemLeft.sameType(center)) {
                    break;
                }
                hor.add(gemLeft);
            }
            xLeft--;
        }
        while (xRight < 8) {
            Gem gemRight = gemAt(xRight, y);
            if (gemRight != null) {
                if (!gemRight.sameType(center)) {
                    break;
                }
                hor.add(gemRight);
            }
            xRight++;
        }
        if (hor.size() >= 3) res.addAll(hor);

        // check vertically
        List<Gem> ver = new ArrayList<>();
        ver.add(center);
        int yBelow = y - 1, yAbove = y + 1;
        while (yBelow >= 0) {
            Gem gemBelow = gemAt(x, yBelow);
            if (gemBelow != null) {
                if (!gemBelow.sameType(center)) {
                    break;
                }
                ver.add(gemBelow);
            }
            yBelow--;
        }
        while (yAbove < 8) {
            Gem gemAbove = gemAt(x, yAbove);
            if (gemAbove != null) {
                if (!gemAbove.sameType(center)) {
                    break;
                }
                ver.add(gemAbove);
            }
            yAbove++;
        }
        if (ver.size() >= 3) res.addAll(ver);

        return res;
    }

    // Find Gem at Position (x, y)
    private Gem gemAt(int x, int y) {
        for (Gem g : gems) {
            if (g != null && g.getX() == x && g.getY() == y) {
                return g;
            }
        }
        return null;
    }

    private void printArrayGems() {
        int width = 8;
        int height = (gems.size() - 1) / width;
        for (int i = height; i >= 0; i--) {
            for (int j = 0; j < 8; j++) {
                System.out.print((gems.get(j + i * width).getType().getCode() + "\t"));
            }
            System.out.println();
        }
        System.out.println();
    }
}
