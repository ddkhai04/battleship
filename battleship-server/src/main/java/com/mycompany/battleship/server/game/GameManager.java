package com.mycompany.battleship.server.game;

import com.mycompany.battleship.common.model.GameConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Sổ quản lý các phòng đang chơi.
 */
public class GameManager {
    private final ScheduledExecutorService scheduler;
    private final GameConfig config;
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerToRoom = new ConcurrentHashMap<>();

    public GameManager() { this(GameConfig.defaults()); }

    public GameManager(GameConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "game-timer");
            t.setDaemon(true);
            return t;
        });
    }

    /** Tạo phòng và bắt đầu trận (đếm ngược 3-2-1). inviterId đi trước. */
    public GameRoom createRoom(String inviterId, String inviteeId, GameEventListener listener) {
        if (playerToRoom.containsKey(inviterId) || playerToRoom.containsKey(inviteeId)) {
            throw new IllegalStateException("Người chơi đang ở trong một trận khác");
        }
        String roomId = UUID.randomUUID().toString();
        GameRoom room = new GameRoom(roomId, inviterId, inviteeId, config, scheduler, listener);
        rooms.put(roomId, room);
        playerToRoom.put(inviterId, roomId);
        playerToRoom.put(inviteeId, roomId);
        room.setOnClosed(() -> {
            rooms.remove(roomId);
            playerToRoom.remove(inviterId, roomId);
            playerToRoom.remove(inviteeId, roomId);
        });
        room.start();
        return room;
    }

    public GameRoom getRoom(String roomId) { 
        return rooms.get(roomId); 
    }

    /** Trả về danh sách tất cả các GameRoom đang chạy phục vụ tính năng Admin / Giám sát */
    public List<GameRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
    public GameRoom findByRoomId(String roomId) {
        for (GameRoom room : getAllRooms()) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }
    public Map<String, GameRoom> getRooms() {
        return rooms;
    }

    /** Phòng mà người chơi đang ở; null nếu không có. */
    public GameRoom findByPlayer(String playerId) {
        String roomId = playerToRoom.get(playerId);
        return roomId == null ? null : rooms.get(roomId);
    }

    public boolean isInGame(String playerId) { 
        return findByPlayer(playerId) != null; 
    }

    /** Gọi khi socket của người chơi bị ngắt: đang đánh thì bị xử thua. */
    public void onPlayerDisconnected(String playerId) {
        GameRoom room = findByPlayer(playerId);
        if (room != null) room.leave(playerId);
    }

    public void shutdown() { 
        scheduler.shutdownNow(); 
    }
}