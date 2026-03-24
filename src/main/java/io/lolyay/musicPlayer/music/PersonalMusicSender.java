package io.lolyay.musicPlayer.music;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.musicPlayer.MusicPlayerMeow;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PersonalMusicSender extends AbstractMusicSender {
    private final UUID playerName;
    private TrackMetadata currentTrack;

    public PersonalMusicSender(UUID name) {
        super();
        this.playerName = name;
        initAudioPlayer(VoiceChatPlugin.getOrCreateChannelForPlayer(name));
        System.out.println("Created Player for " + name);
    }

    public void setCurrentTrack(TrackMetadata track) {
        this.currentTrack = track;
    }

    public TrackMetadata getCurrentTrack() {
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
        try {
            MusicPlayerMeow.getInstance().musicManager.stopPersonalMusic(playerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void offer(short[] data, int seq) {
        super.offer(data);
    }
}
