package io.lolyay.musicPlayerMeow.music;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import io.lolyay.discordmsend.client.ClientTrackInfo;
import io.lolyay.musicPlayerMeow.MusicPlayerMeow;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PersonalMusicSender extends AbstractMusicSender {
    private final UUID playerName;
    private ClientTrackInfo currentTrack;

    public PersonalMusicSender(UUID name) {
        super();
        this.playerName = name;
        initAudioPlayer(VoiceChatPlugin.getOrCreateChannelForPlayer(name));
        System.out.println("Created Player for " + name);
    }

    public void setCurrentTrack(ClientTrackInfo track) {
        this.currentTrack = track;
    }

    public ClientTrackInfo getCurrentTrack() {
        return currentTrack;
    }

    @Override
    public synchronized void start() {
        if (!startRequested.get()) {
            System.out.println("Player Started for " + playerName);
            super.start();
            try {
                Player player = org.bukkit.Bukkit.getPlayer(playerName);
                if (player != null && VoiceChatPlugin.voicechatServerApi != null) {
                    VoicechatConnection connection = VoiceChatPlugin.voicechatServerApi.getConnectionOf(player.getUniqueId());
                    if (connection != null) {
                        VoiceChatPlugin.getOrCreateChannelForPlayer(playerName).addTarget(connection);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        System.out.println("Player Stopped/Cleaned for " + playerName);
        // Notify MusicManager to stop the source (Discord bot)
        // We use a new thread or check if this is safe to call here
        try {
            MusicPlayerMeow.getInstance().musicManager.stopPersonalMusic(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Compatibility method for MusicEventHandler
    public void offer(short[] data, int seq) {
        super.offer(data);
    }
}
