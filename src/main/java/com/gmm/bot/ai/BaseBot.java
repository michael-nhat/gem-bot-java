package com.gmm.bot.ai;

import com.gmm.bot.enumeration.BattleMode;
import com.gmm.bot.enumeration.GemType;
import com.gmm.bot.model.Grid;
import com.gmm.bot.model.Hero;
import com.gmm.bot.model.Pair;
import com.gmm.bot.model.Player;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import sfs2x.client.SmartFox;
import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;
import sfs2x.client.core.SFSEvent;
import sfs2x.client.entities.Room;
import sfs2x.client.entities.User;
import sfs2x.client.requests.ExtensionRequest;
import sfs2x.client.requests.JoinRoomRequest;
import sfs2x.client.requests.LoginRequest;
import sfs2x.client.util.ConfigData;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.gmm.bot.ai.ConstantCommand.LOBBY_FIND_GAME;

@Slf4j
@Getter
public abstract class BaseBot implements IEventListener {
    private final int ENEMY_PLAYER_ID = 0;
    private final int BOT_PLAYER_ID = 2;
    @Autowired
    protected ThreadPoolTaskScheduler taskScheduler;
    @Value("${smartfox.host}")
    protected String host;
    @Value("${smartfox.zone}")
    protected String zone;
    @Value("${smartfox.port}")
    protected int port;
    @Value("${gemswap.delay}")
    protected int delaySwapGem;
    @Value("${find.game.delay}")
    protected int delayFindGame;
    protected SmartFox sfsClient;
    protected Room room;
    protected Player botPlayer;
    protected Player enemyPlayer;
    protected int currentPlayerId;
    protected Grid grid;
    protected volatile boolean isJoinGameRoom;
    protected String username;
    protected String password;
    protected String token;
    protected SFSObject data;
    protected boolean disconnect;

    public void start() {
        try {
            this.logStatus("init", "Initializing");
            this.init();
            this.connect();
        } catch (Exception e) {
            this.log("Init bot error =>" + e.getMessage());
        }
    }

    private void init() {
        username = "hiep.ngoxuan";
        sfsClient = new SmartFox();
        data = new SFSObject();
        isJoinGameRoom = false;
        disconnect = false;
        this.token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJoaWVwLm5nb3h1YW4iLCJhdXRoIjoiUk9MRV9VU0VSIiwiTEFTVF9MT0dJTl9USU1FIjoxNjUzNTgyNTM5NjAyLCJleHAiOjE2NTUzODI1Mzl9.ZCA5hRudtndLmr0R5qSmX90Pvt0_PJ-Ocxpkpqbl3GJOJGyfFHpBVcpAwPKWC5v79ytitlBlS2ogVgNQj5i1Rw"
        ;this.sfsClient.addEventListener(SFSEvent.CONNECTION, this);
        this.sfsClient.addEventListener(SFSEvent.CONNECTION_LOST, this);
        this.sfsClient.addEventListener(SFSEvent.LOGIN, this);
        this.sfsClient.addEventListener(SFSEvent.LOGIN_ERROR, this);
        this.sfsClient.addEventListener(SFSEvent.ROOM_JOIN, this);
        this.sfsClient.addEventListener(SFSEvent.ROOM_JOIN_ERROR, this);
        this.sfsClient.addEventListener(SFSEvent.EXTENSION_RESPONSE, this);
    }

    protected void connect() {
        this.logStatus("connecting", " => Connecting to smartfox server " + host + "|" + port + " zone: " + zone);

        this.sfsClient.setUseBlueBox(true);
        this.sfsClient.connect(this.host, this.port);

        ConfigData cf = new ConfigData();
        cf.setHost(host);
        cf.setPort(port);
        cf.setUseBBox(true);
        cf.setZone(zone);

        try {
            this.sfsClient.connect(cf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        this.logStatus("disconnect|", " manual called disconnect from client");
        try {
            sfsClient.disconnect();
            disconnect = true;
        } catch (Exception e) {
            log.error("disconnect|" + this.username + "|error =>" + e.getMessage());
        }
    }

    public void dispatch(BaseEvent event) {
        String eventType = event.getType();

        switch (eventType) {
            case SFSEvent.CONNECTION:
                this.onConnection(event);
                break;
            case SFSEvent.CONNECTION_LOST:
                this.onConnectionLost(event);
                break;
            case SFSEvent.LOGIN:
                this.onLoginSuccess(event);
                break;
            case SFSEvent.LOGIN_ERROR:
                this.onLoginError(event);
                break;
            case SFSEvent.ROOM_JOIN:
                this.onRoomJoin(event);
                break;
            case SFSEvent.ROOM_JOIN_ERROR:
                this.onRoomJoinError(event);
                break;
            case SFSEvent.EXTENSION_RESPONSE:
                this.onExtensionResponse(event);
                break;
            default:
        }
    }

    private void onConnection(BaseEvent event) {
        if (event.getArguments().get("success").equals(true)) {
            this.logStatus("try-login", "Connected to smartfox|" + event.getArguments().toString());
            this.login();
        } else {
            this.logStatus("onConnection|success == false", "Failed to connect");
        }
    }

    protected void onConnectionLost(BaseEvent event) {
        this.logStatus("onConnectionLost", "userId connection lost server: " + event.getArguments().toString());
        disconnect = true;
        sfsClient.removeAllEventListeners();
    }


    protected void onLoginError(BaseEvent event) {
        this.logStatus("login-error", "Login failed");
        disconnect();
    }

    protected void onRoomJoin(BaseEvent event) {
        logStatus("Join-room", "Joined room " + this.sfsClient.getLastJoinedRoom().getName());
        room = (Room) event.getArguments().get("room");
        if (room.isGame()) {
            return;
        }
        data.putUtfString("type", "");
        data.putUtfString("adventureId", "");
        sendZoneExtensionRequest(LOBBY_FIND_GAME, data);
        log("Send request Find game from lobby");
        //taskScheduler.schedule(new FindRoomGame(), new Date(System.currentTimeMillis() + delayFindGame));
    }

    protected void onRoomJoinError(BaseEvent event) {
        if (this.sfsClient.getLastJoinedRoom() != null) {
            this.logStatus("join-room", "Joined room " + this.sfsClient.getLastJoinedRoom().getName());
        }
        taskScheduler.schedule(new FindRoomGame(), new Date(System.currentTimeMillis() + delayFindGame));
    }

    protected void onExtensionResponse(BaseEvent event) {
        String cmd = event.getArguments().containsKey("cmd") ? event.getArguments().get("cmd").toString() : "";
        SFSObject params = (SFSObject) event.getArguments().get("params");

        logStatus("onExtensionResponse", cmd);
        switch (cmd) {
            case ConstantCommand.START_GAME:
                ISFSObject gameSession = params.getSFSObject("gameSession");
                startGame(gameSession, room);
                break;
            case ConstantCommand.END_GAME:
                endGame();
                break;
            case ConstantCommand.START_TURN:
                startTurn(params);
                break;
            case ConstantCommand.ON_SWAP_GEM:
                swapGem(params);
                break;
            case ConstantCommand.ON_PLAYER_USE_SKILL:
                handleGems(params);
                break;
            case ConstantCommand.PLAYER_JOINED_GAME:
                sendExtensionRequest(ConstantCommand.I_AM_READY, new SFSObject());
                break;
        }
    }

    protected abstract void startGame(ISFSObject gameSession, Room room);

    protected abstract void swapGem(SFSObject params);

    protected abstract void handleGems(ISFSObject params);

    protected abstract void startTurn(ISFSObject params);

    private void endGame() {
        isJoinGameRoom = false;
    }



    protected void assignPlayers(Room room) {
        List<User> users = room.getPlayerList();
        User user1 = users.get(0);
        log("id user1: " + user1.getPlayerId() + " name:"+ user1.getName());
        if(users.size() == 1){
            if (user1.isItMe()) {
                botPlayer = new Player(user1.getPlayerId(), "player1");
                enemyPlayer = new Player(ENEMY_PLAYER_ID, "player2");
            } else {
                botPlayer = new Player(BOT_PLAYER_ID, "player2");
                enemyPlayer = new Player(ENEMY_PLAYER_ID, "player1");
            }
            return;
        }
        User user2 = users.get(1);
        log("id user2: " + user2.getPlayerId()+ " name:"+user2.getName());
        if (user1.isItMe()) {
            botPlayer = new Player(user1.getPlayerId(), "player"+user1.getPlayerId());
            enemyPlayer = new Player(user2.getPlayerId(), "player"+user2.getPlayerId());
        } else {
            botPlayer = new Player(user2.getPlayerId(), "player"+user2.getPlayerId());
            enemyPlayer = new Player(user1.getPlayerId(), "player"+user1.getPlayerId());
        }
    }

    protected void logStatus(String status, String logMsg) {
        log.info(this.username + "|" + status + "|" + logMsg + "\n");
    }

    protected void log(String msg) {
        log.info(this.username + "|" + msg);
    }

    private void onLoginSuccess(BaseEvent event) {
        try {
            log("onLogin()|" + event.getArguments().toString());

            // Find game after login
            data.putUtfString("type", "");
            data.putUtfString("adventureId", "");
//            sendZoneExtensionRequest(LOBBY_FIND_GAME, data);
        } catch (Exception e) {
            log("onLogin|error => " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void login() {
        log("login()");
        SFSObject parameters = new SFSObject();
        parameters.putUtfString(ConstantCommand.BATTLE_MODE, BattleMode.NORMAL.name());
        parameters.putUtfString(ConstantCommand.ID_TOKEN, this.token);
        parameters.putUtfString(ConstantCommand.NICK_NAME, username);
        this.sfsClient.send(new LoginRequest(username, "", zone, parameters));
    }

    public void sendExtensionRequest(String extCmd, ISFSObject params) {
        this.sfsClient.send(new ExtensionRequest(extCmd, params, room));
    }

    public void sendZoneExtensionRequest(String extCmd, ISFSObject params) {
        this.sfsClient.send(new ExtensionRequest(extCmd, params));
    }

    private class FindRoomGame implements Runnable {
        @Override
        public void run() {
            data.putUtfString("type", "");
            data.putUtfString("adventureId", "");
            sendZoneExtensionRequest(LOBBY_FIND_GAME, data);
            log("SendZoneExtension LOBBY_FIND_GAME");
        }
    }
}