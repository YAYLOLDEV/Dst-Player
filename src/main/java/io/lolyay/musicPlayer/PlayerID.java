package io.lolyay.musicPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerID {
    private static final ConcurrentHashMap<UUID, Integer> map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, UUID> reverseMap = new ConcurrentHashMap<>();
    private static int nextId = 1;

    public static int toInt(UUID playerName) {
        return map.computeIfAbsent(playerName, k -> {
            int id = nextId++;
            reverseMap.put(id, playerName);
            return id;
        });
    }

    public static UUID getFromId(int id){
        return reverseMap.get(id);
    }
}
