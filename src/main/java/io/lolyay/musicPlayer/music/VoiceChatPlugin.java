package io.lolyay.musicPlayer.music;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import io.lolyay.musicPlayer.PlayerID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.lolyay.musicPlayer.CustomUUID.generateCustomV8;
import static io.lolyay.musicPlayer.CustomUUID.generateCustomV9;
import static io.lolyay.musicPlayer.music.MusicEventHandler.getPersonalPlayer;


public class VoiceChatPlugin implements VoicechatPlugin {
    public static final String GLOBAL_CATEGORY_ID = "mplaymusicg";
    public static final String CATEGORY_ID = "mplaymusicp";
    public static VoicechatServerApi voicechatServerApi;
    private static StaticAudioChannel globalChannel;
    public static final Map<UUID,StaticAudioChannel> channelMap = new HashMap<>();
    private static final Map<UUID, UUID> playerChannels = new HashMap<>();

    @Override
    public String getPluginId() {
        return "lsbutils";
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
    }

    @Override
    public void registerEvents(EventRegistration eventRegistration) {
        eventRegistration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnected);
        eventRegistration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);
        eventRegistration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }
    
    private void onPlayerConnected(PlayerConnectedEvent playerConnectedEvent) {
        UUID playerUUID = playerConnectedEvent.getConnection().getPlayer().getUuid();
        
        createChannelForPlayer(playerUUID);
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent playerConnectedEvent) {
        UUID playerUUID = playerConnectedEvent.getPlayerUuid();
        PersonalMusicSender sender = getPersonalPlayer(PlayerID.toInt(playerUUID));
        if (sender != null) {
            sender.stop();
            sender.clean();
        }
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        voicechatServerApi = event.getVoicechat();

        VoicechatServerApi api = event.getVoicechat();
        VolumeCategory musicDiscs = api.volumeCategoryBuilder()
                .setId(CATEGORY_ID)
                .setName("Your Music")
                .setDescription("The volume of your playing music!")
                .build();

        api.registerVolumeCategory(musicDiscs);

        VolumeCategory glob = api.volumeCategoryBuilder()
                .setId(GLOBAL_CATEGORY_ID)
                .setName("Global Music")
                .setDescription("The volume of globally (Radio) playing music!")
                .build();

        api.registerVolumeCategory(glob);
        createGlobalChannel();
    }
    
    public static boolean channelExists(UUID channelID){
        return channelMap.containsKey(channelID);
    }
    
    public static StaticAudioChannel getChannel(UUID channelID){
        return channelMap.get(channelID);
    }
    
    public static StaticAudioChannel getGlobalChannel() {
        return globalChannel;
    }

    public static StaticAudioChannel getOrCreateChannelForPlayer(UUID playerUUID){
        if (playerUUID == null) {
            return null;
        }
        
        UUID channelID = playerChannels.get(playerUUID);
        if (channelID != null && channelExists(channelID)) {
            return getChannel(channelID);
        }
        
        channelID = generateCustomV8();
        StaticAudioChannel channel = voicechatServerApi.createStaticAudioChannel(channelID);
        channel.setCategory(CATEGORY_ID);
        
        channelMap.put(channelID, channel);
        playerChannels.put(playerUUID, channelID);
        return channel;
    }
    
    private static void createChannelForPlayer(UUID playerUUID) {
        UUID channelID = generateCustomV8();
        StaticAudioChannel channel = voicechatServerApi.createStaticAudioChannel(channelID);
        channel.setCategory(CATEGORY_ID);
        
        if (voicechatServerApi.getConnectionOf(playerUUID) != null) {
             channel.addTarget(voicechatServerApi.getConnectionOf(playerUUID));
        }
       
        channelMap.put(channelID, channel);
        playerChannels.put(playerUUID, channelID);
    }
    
    public static void addPlayerToGlobal(UUID playerUUID){
        if (globalChannel != null && voicechatServerApi != null) {
            VoicechatConnection connection = voicechatServerApi.getConnectionOf(playerUUID);
            if (connection != null) {
                globalChannel.addTarget(connection);
            }
        }
    }
    
    public static void addAllPlayersToGlobal() {
        if (globalChannel != null && voicechatServerApi != null) {
            for (Player connection : Bukkit.getServer().getOnlinePlayers()) {
                try {
                    VoicechatConnection vConnection = voicechatServerApi.getConnectionOf(connection.getUniqueId());
                    if (vConnection != null) {
                        globalChannel.addTarget(vConnection);
                    }
                } catch (Exception e) {
                    //Not connected!
                }
            }
        }
    }
    
    public static void removePlayerFromGlobal(UUID playerUUID){
        if (globalChannel != null && voicechatServerApi != null) {
            VoicechatConnection connection = voicechatServerApi.getConnectionOf(playerUUID);
            if (connection != null) {
                globalChannel.removeTarget(connection);
            }
        }
    }
    
    public static void addPlayerToChannel(UUID playerUUID, UUID channelUUID){
        StaticAudioChannel channel = getChannel(channelUUID);
        if (channel != null && voicechatServerApi != null) {
            VoicechatConnection connection = voicechatServerApi.getConnectionOf(playerUUID);
            if (connection != null) {
                channel.addTarget(connection);
            }
        }
    }
    
    public static void removePlayerFromChannel(UUID playerUUID, UUID channelUUID){
        StaticAudioChannel channel = getChannel(channelUUID);
        if (channel != null && voicechatServerApi != null) {
            VoicechatConnection connection = voicechatServerApi.getConnectionOf(playerUUID);
            if (connection != null) {
                channel.removeTarget(connection);
            }
        }
    }

    public static void createGlobalChannel(){
        UUID channelID = generateCustomV9();
        StaticAudioChannel channel = voicechatServerApi.createStaticAudioChannel(channelID);
        channel.setCategory(GLOBAL_CATEGORY_ID);
        channelMap.put(channelID, channel);
        globalChannel = channel;
    }
}
