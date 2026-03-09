package io.lolyay.musicPlayer.music;

import io.lolyay.discordmsend.client.ClientTrackInfo;
import io.lolyay.discordmsend.client.DstClient;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.musicPlayer.MusicPlayerMeow;
import io.lolyay.musicPlayer.PlayerID;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.lolyay.musicPlayer.music.MusicEventHandler.getPersonalPlayer;


public class MusicManager {
    private final long serverId;
    private final long globalGuildId;
    private final DstClient client;
    private final int volume;
    private final CUserData cUserData = new CUserData(
            "DstMusicPlayer - Paper",
            "0.1-BETA",
            "LOLYAY",
            new ClientFeatures(
                    ClientFeatures.Feature.ALLOW_LIST_IN_ACTIVE_CONNECTIONS,
                    ClientFeatures.Feature.USES_OPUS,
                    ClientFeatures.Feature.UDP_ME_PLZ // well not techincally UDP, just lets say sends you audio REALLY QUICKLY (opposite of streaming)
            )
    );
    
    public volatile boolean isGlobalPlaying = false;
    public volatile boolean isDebug = false;

    public DstClient getClient() {
        return client;
    }

    public long getServerId() {
        return serverId;
    }
    
    public long getGlobalGuildId() {
        return globalGuildId;
    }

    public MusicManager(long serverId, int volume) {
        this.volume = volume;
        this.serverId = serverId;
        this.globalGuildId = serverId; // Use a high offset for global channel
        String token = MusicPlayerMeow.getInstance().getConfig().getString("music-token");
        this.client = DstClient.createDirect(token, cUserData, new MusicEventHandler());
    }

    public CompletableFuture<ClientTrackInfo> playGlobalRadio(String query){
        System.out.println("[RADIO] Starting global radio search for: " + query);
        
        if (isGlobalPlaying) {
            System.out.println("[RADIO] Stopping previous global playback");
            client.stop(globalGuildId);
        }
        
        isGlobalPlaying = true;
        
        return client.searchTracksMultiple(query, 5)
                .orTimeout(5, TimeUnit.SECONDS)
                .thenCompose(tracks -> {
                    System.out.println("[RADIO] Track found, playing on guild: " + globalGuildId);
                    MusicEventHandler.cleanGlobalAudioPlayer();
                    client.playTrack(tracks.getFirst(), globalGuildId);
                    client.setVolume(globalGuildId, volume);
                    return client.getTrackInfo(tracks.getFirst());
                })
                .exceptionally(ex -> {
                    System.out.println("[RADIO] Error: " + ex.getMessage());
                    isGlobalPlaying = false;
                    throw new RuntimeException(ex);
                });
    }
    
    private final java.util.Set<Long> createdPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    public CompletableFuture<ClientTrackInfo> playPersonalSong(UUID playerUUID, String query, String playerName){
        long guildId = serverId + PlayerID.toInt(playerUUID);
        
        System.out.println("[MUSIC] Starting personal music search for " + playerName + " (guild: " + guildId + ") - Query: " + query);

        if (!createdPlayers.contains(guildId)) {
            System.out.println("[MUSIC] Creating new player for " + playerName + " (guild: " + guildId + ")");
            try {
                createdPlayers.add(guildId);
            } catch (Exception e) {
                System.out.println("[MUSIC] Warning: Player creation failed (may already exist): " + e.getMessage());
            }
        }

        getPersonalPlayer(guildId).clean();

        return client.searchTracksMultiple(query, 5)
                .orTimeout(5, TimeUnit.SECONDS)
                .thenCompose(tracks -> {
                    System.out.println("[MUSIC] Track found, playing for " + playerName + " on guild: " + guildId);
                    client.playTrack(tracks.getFirst(), guildId);
                    client.setVolume(guildId, volume);
                    return client.getTrackInfo(tracks.getFirst());
                })
                .thenApply(trackInfo -> {
                    PersonalMusicSender sender = getPersonalPlayer(guildId);
                    if (sender != null) {
                        sender.setCurrentTrack(trackInfo);
                    }
                    return trackInfo;
                })
                .exceptionally(ex -> {
                    System.out.println("[MUSIC] Error for " + playerName + ": " + ex.getMessage());
                    throw new RuntimeException(ex);
                });
    }
    
    // Stop GLOBAL radio
    public void stopGlobalRadio() {
        System.out.println("[RADIO] Stopping global radio (guild: " + globalGuildId + ")");
        System.out.println("[RADIO] Stop called from: " + Thread.currentThread().getStackTrace()[2]);
        client.stop(globalGuildId);
        isGlobalPlaying = false;
    }
    
    // Stop PERSONAL player music
    public void stopPersonalMusic(UUID playerName) {
        long guildId = serverId + PlayerID.toInt(playerName);
        System.out.println("[MUSIC] Stopping personal music for " + playerName + " (guild: " + guildId + ")");
        client.stop(guildId);
    }

    public void start(){
        String host = MusicPlayerMeow.getInstance().getConfig().getString("music-host");
        int port = MusicPlayerMeow.getInstance().getConfig().getInt("music-port");
        try {
            client.connect(host, port);
            Thread.sleep(500); // server has race condition here, better wait
            client.setDefaultVolume(volume);
            MusicPlayerMeow.getInstance().getLogger().info("Successfully connected to music server!");
        } catch (Exception e) {
            MusicPlayerMeow.getInstance().getLogger().severe("Failed to connect to music server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop(){
        client.stop(globalGuildId);
        client.disconnect("Stopping...");
        isGlobalPlaying = false;
    }

}
