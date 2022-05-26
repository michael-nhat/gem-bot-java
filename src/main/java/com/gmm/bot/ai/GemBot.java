package com.gmm.bot.ai;

import com.gmm.bot.enumeration.GemModifier;
import com.gmm.bot.enumeration.GemType;
import com.gmm.bot.model.*;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import sfs2x.client.entities.Room;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Scope("prototype")
@Slf4j
@Getter
public class GemBot extends BaseBot{

    @Override
    protected void swapGem(SFSObject params) {
        boolean isValidSwap = params.getBool("validSwap");
        if (!isValidSwap) {
            return;
        }
        handleGems(params);
    }

    @Override
    protected void handleGems(ISFSObject params) {
        ISFSObject gameSession = params.getSFSObject("gameSession");
        currentPlayerId = gameSession.getInt("currentPlayerId");
        //get last snapshot
        ISFSArray snapshotSfsArray = params.getSFSArray("snapshots");
        ISFSObject lastSnapshot = snapshotSfsArray.getSFSObject(snapshotSfsArray.size() - 1);
        boolean needRenewBoard = params.containsKey("renewBoard");
        // update information of hero
        handleHeroes(lastSnapshot);
        if (needRenewBoard) {
            grid.updateGems(params.getSFSArray("renewBoard"),null);
            taskScheduler.schedule(new FinishTurn(false), new Date(System.currentTimeMillis() + delaySwapGem));
            return;
        }
        // update gem
        grid.setGemTypes(botPlayer.getRecommendGemType());
        ISFSArray gemCodes = lastSnapshot.getSFSArray("gems");
        ISFSArray gemModifiers = lastSnapshot.getSFSArray("gemModifiers");
        grid.updateGems(gemCodes,gemModifiers);
        taskScheduler.schedule(new FinishTurn(false), new Date(System.currentTimeMillis() + delaySwapGem));
    }

    @Override
    protected void startTurn(ISFSObject params) {
        currentPlayerId = params.getInt("currentPlayerId");
        if (!isBotTurn()) {
            return;
        }
        /*List<Hero> herosFullMana = botPlayer.herosFullMana();
        if (!CollectionUtils.isEmpty(herosFullMana)) {
            taskScheduler.schedule(new SendReQuestSkill(herosFullMana), new Date(System.currentTimeMillis() + delaySwapGem));
            return;
        }
        taskScheduler.schedule(new SendRequestSwapGem(), new Date(System.currentTimeMillis() + delaySwapGem));*/
        handleStartTurn();
    }

    private void handleStartTurn() {
        handleCastSkill();
        handleMoveGem();
    }

    private void handleCastSkill() {
        //get list heros target
        List<Hero> heroesTarget = enemyPlayer.herosAlive().stream().sorted(Comparator.comparing(Hero::getHp)).collect(Collectors.toList());
        for (Hero hero :heroesTarget) {
            log.info("handleCastSkill: heroesTarget={} {} {} {} {}", hero.getId(), hero.getName(), hero.getAttack(), hero.getHp(), hero.getMana());
        }
        String heroTargetId = heroesTarget.get(0).getId().toString();
        if (!CollectionUtils.isEmpty(heroesTarget)) {
            heroTargetId = heroesTarget.get(0).getId().toString();
        }

        List<Hero> heroesBuff = botPlayer.heroesBuff();
        List<Hero> heroesAttack = botPlayer.heroesAttack();

        GemType gemType = selectGem();
        String gemIndex = String.valueOf(ThreadLocalRandom.current().nextInt(64));

        if (!CollectionUtils.isEmpty(heroesAttack)) {
            if (heroesAttack.size() == 1) {
                castSkill(heroesAttack.get(0).getId().toString(), heroTargetId, gemIndex, gemType.toString());
            } else {
                Hero hero = heroesAttack.stream().max(Comparator.comparing(Hero::getAttack)).orElse(null);
                castSkill(hero.getId().toString(), heroTargetId, gemIndex, gemType.toString());
            }
        }
        if (!CollectionUtils.isEmpty(heroesBuff)) {
            heroTargetId = botPlayer.dameHeroHighest().getId().toString();
            if (heroesBuff.size() == 1) {
                castSkill(heroesBuff.get(0).getId().toString(), heroTargetId, gemIndex, gemType.toString());
            } else {
                Hero hero = heroesBuff.stream().min(Comparator.comparing(Hero::getHp)).orElse(null);
                castSkill(hero.getId().toString(), heroTargetId, gemIndex, gemType.toString());
            }
        }
        log.info("handleCastSkill: finish cast");
    }

    private void castSkill(String heroCasterId, String heroTargetId, String gemIndex, String gemType) {
        data.putUtfString("casterId", heroCasterId);
        data.putUtfString("targetId", heroTargetId);
        data.putUtfString("selectedGem", gemType);
        data.putUtfString("gemIndex", gemIndex);
        data.putBool("isTargetAllyOrNot",false);
        log.info("castSkill: heroCasterId={} heroTargetId={} gemIndex={} gemType={}", heroCasterId, heroTargetId, gemIndex, gemType);
        sendExtensionRequest(ConstantCommand.USE_SKILL, data);
    }

    private void handleMoveGem() {
        taskScheduler.schedule(new SendRequestSwapGem(), new Date(System.currentTimeMillis() + delaySwapGem));
    }

    private void handleHeroes(ISFSObject params) {
        ISFSArray heroesBotPlayer = params.getSFSArray(botPlayer.getDisplayName());
        for (int i = 0; i < botPlayer.getHeroes().size(); i++) {
            botPlayer.getHeroes().get(i).updateHero(heroesBotPlayer.getSFSObject(i));
        }

        ISFSArray heroesEnemyPlayer = params.getSFSArray(enemyPlayer.getDisplayName());
        for (int i = 0; i < enemyPlayer.getHeroes().size(); i++) {
            enemyPlayer.getHeroes().get(i).updateHero(heroesEnemyPlayer.getSFSObject(i));
        }
    }

    @Override
    protected void startGame(ISFSObject gameSession, Room room) {
        // Assign Bot player & enemy player
        assignPlayers(room);

        // Player & Heroes
        ISFSObject objBotPlayer = gameSession.getSFSObject(botPlayer.getDisplayName());
        ISFSObject objEnemyPlayer = gameSession.getSFSObject(enemyPlayer.getDisplayName());

        ISFSArray botPlayerHero = objBotPlayer.getSFSArray("heroes");
        ISFSArray enemyPlayerHero = objEnemyPlayer.getSFSArray("heroes");

        for (int i = 0; i < botPlayerHero.size(); i++) {
            botPlayer.getHeroes().add(new Hero(botPlayerHero.getSFSObject(i)));
        }
        for (int i = 0; i < enemyPlayerHero.size(); i++) {
            enemyPlayer.getHeroes().add(new Hero(enemyPlayerHero.getSFSObject(i)));
        }

        // Gems
        grid = new Grid(gameSession.getSFSArray("gems"),null, botPlayer.getRecommendGemType());
        currentPlayerId = gameSession.getInt("currentPlayerId");
        log("Initial game ");
        taskScheduler.schedule(new FinishTurn(true), new Date(System.currentTimeMillis() + delaySwapGem));
    }

    protected GemType selectGem() {
        return botPlayer.getRecommendGemType().stream().filter(gemType -> grid.getGemTypes().contains(gemType)).findFirst().orElseGet(null);
    }

    protected boolean isBotTurn() {
        return botPlayer.getId() == currentPlayerId;
    }

    private class FinishTurn implements Runnable {
        private final boolean isFirstTurn;

        public FinishTurn(boolean isFirstTurn) {
            this.isFirstTurn = isFirstTurn;
        }

        @Override
        public void run() {
            SFSObject data = new SFSObject();
            data.putBool("isFirstTurn", isFirstTurn);
            log("sendExtensionRequest()|room:" + room.getName() + "|extCmd:" + ConstantCommand.FINISH_TURN + " first turn " + isFirstTurn);
            sendExtensionRequest(ConstantCommand.FINISH_TURN, data);
        }
    }

    private class SendRequestSwapGem implements Runnable {
        @Override
        public void run() {
            grid.setMyHeroGemType(botPlayer.getRecommendGemType());
            grid.setEnemyHeroGemType(enemyPlayer.getRecommendGemType());
            Pair<Integer> indexSwap = grid.recommendSwapGemWithDrop();
            data.putInt("index1", indexSwap.getParam1());
            data.putInt("index2", indexSwap.getParam2());

            sendExtensionRequest(ConstantCommand.SWAP_GEM, data);
        }
    }

    private Pair<Integer> swapGemCustomize() {
        List<GemType> gemTypes = botPlayer.herosAlive().stream()
                .map(Hero::getGemTypes).flatMap(Collection::stream).distinct().collect(Collectors.toList());
        log.info("swapGemCustomize: gemType={}", gemTypes);
        List<GemSwapInfo> listMatchGem = grid.suggestMatch();
        if (listMatchGem.isEmpty()) {
            return new Pair<>(-1, -1);
        }
        Gem gemSpecial = grid.getSpecialGem().stream().filter(g -> Arrays.asList(GemModifier.values()).contains(g.getModifier())).findFirst().orElse(null);
        Gem gemExtraTurn = grid.getSpecialGem().stream().filter(g -> g.getModifier() == GemModifier.EXTRA_TURN).findFirst().orElse(null);

        if (gemExtraTurn != null) {
            List<GemSwapInfo> listExtraturn = listMatchGem.stream().filter(g-> gemSpecial != null
                    && (g.getGemModifier() == gemExtraTurn.getModifier())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(listExtraturn)) {
                log.info("gem special turn from index1={} to index2={} with gemType={} and gemModifier={}",
                        listExtraturn.get(0).getIndex1(), listExtraturn.get(0).getIndex2(), listExtraturn.get(0).getType(), listExtraturn.get(0).getGemModifier());
                return listExtraturn.get(0).getIndexSwapGem();
            }
        }
        List<GemSwapInfo> listSpecial = listMatchGem.stream().filter(g-> gemSpecial != null
                && (g.getGemModifier() == gemSpecial.getModifier())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(listSpecial)) {
            log.info("gem special turn from index1={} to index2={} with gemType={} and gemModifier={}",
                    listSpecial.get(0).getIndex1(), listSpecial.get(0).getIndex2(), listSpecial.get(0).getType(), listSpecial.get(0).getGemModifier());
            return listSpecial.get(0).getIndexSwapGem();
        }
        List<GemSwapInfo> match5 =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getSizeMatch() > 4).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(match5)) {
            List<GemSwapInfo> match5CungMau = match5.stream().filter(m -> gemTypes.contains(m.getType())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(match5CungMau)) {
                log.info("match5 from index1={} to index2={} with gemType={}", match5CungMau.get(0).getIndex1(),
                        match5CungMau.get(0).getIndex2(), match5CungMau.get(0).getType());
                return match5CungMau.get(0).getIndexSwapGem();
            } else {
                log.info("match5 from index1={} to index2={} with gemType={}", match5.get(0).getIndex1(),
                        match5.get(0).getIndex2(), match5.get(0).getType());
                return match5.get(0).getIndexSwapGem();
            }
        }

        List<GemSwapInfo> match4 =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getSizeMatch() > 3).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(match4)) {
            List<GemSwapInfo> match4CungMau = match4.stream().filter(m -> gemTypes.contains(m.getType())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(match4CungMau)) {
                log.info("match4 from index1={} to index2={} with gemType={}", match4CungMau.get(0).getIndex1(),
                        match4CungMau.get(0).getIndex2(), match4CungMau.get(0).getType());
                return match4CungMau.get(0).getIndexSwapGem();
            }/* else {
                log.info("match4 from index1={} to index2={} with gemType={}", match4.get(0).getIndex1(),
                        match4.get(0).getIndex2(), match4.get(0).getType());
                return match4.get(0).getIndexSwapGem();
            }*/
        }
        Optional<GemSwapInfo> matchGemSword =
                listMatchGem.stream().filter(gemMatch -> gemMatch.getType() == GemType.SWORD).findFirst();
        if (matchGemSword.isPresent()) {
            log.info("match sword from index1={} to index2={} with gemType={}", matchGemSword.get().getIndex1(), matchGemSword.get().getIndex2(), matchGemSword.get().getType());
            return matchGemSword.get().getIndexSwapGem();
        }

        for (GemType type : gemTypes) {
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
}
