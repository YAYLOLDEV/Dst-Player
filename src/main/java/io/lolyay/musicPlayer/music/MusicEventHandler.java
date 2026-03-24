package io.lolyay.musicPlayer.music;

import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;
import io.lolyay.discordmsend.client.ClientEventHandler;
import io.lolyay.discordmsend.network.protocol.codec.AudioConverter;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.events.*;
import io.lolyay.discordmsend.obj.Severity;
import io.lolyay.musicPlayer.MusicPlayerMeow;
import io.lolyay.musicPlayer.PlayerID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MusicEventHandler implements ClientEventHandler {
    private static final OpusDecoder decoder;
    private static final Map<UUID, PersonalMusicSender> personalPlayers = new HashMap<>();
    private static final Map<UUID, Long> personalPlayerCreationTimes = new HashMap<>();
    private static GlobalMusicSender globalPlayer = null;
    private final short[] decodeBuffer = new short[1920]; // Large enough for Stereo 20ms

    static {
        try {
            decoder = new OpusDecoder(48000, 2);
        } catch (OpusException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isGlobalGuildId(long guildId) {
        return guildId == MusicPlayerMeow.getInstance().musicManager.getGlobalGuildId();
    }

    public static PersonalMusicSender getPersonalPlayer(long guildId) {
        UUID playerName = PlayerID.getFromId((int) (guildId - MusicPlayerMeow.getInstance().musicManager.getServerId()));
        if (playerName == null) {
            MusicPlayerMeow.getInstance().getLogger().warning("Could not find player for guild ID: " + guildId);
            return null;
        }
        return personalPlayers.computeIfAbsent(playerName, PersonalMusicSender::new);
    }

    public static GlobalMusicSender getGlobalPlayer() {
        if (globalPlayer == null) {
            globalPlayer = new GlobalMusicSender();
        }
        return globalPlayer;
    }

    public static void stopGlobalAudioPlayer() {
        if (globalPlayer != null) {
            globalPlayer.destroy();
            globalPlayer = null;
        }
    }

    public static void stopPersonalAudioPlayer(UUID playerName) {
        PersonalMusicSender sender = personalPlayers.remove(playerName);
        if (sender != null) {
            sender.stop();
        }
    }


    public static void cleanGlobalAudioPlayer() {
        if (globalPlayer != null) {
            globalPlayer.clean();
        }
    }

    public static void cleanPersonalAudioPlayer(UUID playerName) {
        PersonalMusicSender sender = personalPlayers.get(playerName);
        if (sender != null) {
            sender.clean();
        }
    }

    @Override
    public void onPlayerPause(PlayerPauseS2CPacket packet) {
        // In UDP_MODE, the server sends all audio packets as fast as possible (burst).
        // The client buffers the entire track very quickly (~15ms).
        // Therefore, pause/resume commands from the server are irrelevant as we already have the data locally.
    }

    @Override
    public void onPlayerTrackStart(PlayerTrackStartS2CPacket playerTrackStartS2CPacket) {
        if (MusicPlayerMeow.getInstance().musicManager.isDebug) {
            MusicPlayerMeow.getInstance().getLogger().info("Track started for guild: " + playerTrackStartS2CPacket.guildId());
        }
        if (isGlobalGuildId(playerTrackStartS2CPacket.guildId())) {
            getGlobalPlayer().start();
        } else {
            PersonalMusicSender sender = getPersonalPlayer(playerTrackStartS2CPacket.guildId());
            if (sender != null) sender.start();
        }
    }


    @Override
    public void onPlayerResume(PlayerResumeS2CPacket packet) {
    }


    @Override
    public void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet) {

        if (MusicPlayerMeow.getInstance().musicManager.isDebug) {
            MusicPlayerMeow.getInstance().getLogger().info("Track download complete for guild: " + packet.guildId());
            if (isGlobalGuildId(packet.guildId())) {
                MusicPlayerMeow.getInstance().getLogger().info("Total buffered: " + getGlobalPlayer().getTotalFrames() + " opus frames.");
            } else {
                PersonalMusicSender sender = getPersonalPlayer(packet.guildId());
                if (sender != null) {
                    MusicPlayerMeow.getInstance().getLogger().info("Total buffered: " + sender.getTotalFrames() + " opus frames.");
                }
            }
        } else {
            MusicPlayerMeow.getInstance().getLogger().info("Track download complete for guild: " + packet.guildId());
        }

        // Reset playing flags so next track can start
        if (isGlobalGuildId(packet.guildId())) {
            MusicPlayerMeow.getInstance().musicManager.isGlobalPlaying = false;
        }

    }

    @Override
    public void onPlayerTrackError(PlayerTrackFailS2CPacket packet) {
        if (packet.severity() == Severity.COMMON || packet.severity() == Severity.SUSPICIOUS) {
            MusicPlayerMeow.getInstance().getLogger().info("Track warning (" + packet.severity() + ") for guild " + packet.guildId() + ": " + packet.message());
            return;
        }

        MusicPlayerMeow.getInstance().getLogger().severe("CRITICAL TRACK ERROR (FAULT) for guild " + packet.guildId() + ": " + packet);
        if (isGlobalGuildId(packet.guildId())) {
            MusicPlayerMeow.getInstance().musicManager.stopGlobalRadio();
            GlobalMusicSender player = getGlobalPlayer();
            if (player != null) player.stop();
        } else {
            UUID playerName = PlayerID.getFromId((int) (packet.guildId() - MusicPlayerMeow.getInstance().musicManager.getServerId()));
            if (playerName != null) {
                MusicPlayerMeow.getInstance().musicManager.stopPersonalMusic(playerName);
            }
            PersonalMusicSender sender = getPersonalPlayer(packet.guildId());
            if (sender != null) sender.stop();
        }
    }

    @Override
    public void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet) {
        MusicPlayerMeow.getInstance().getLogger().warning("Track stuck for guild: " + packet.guildId());
    }

    @Override
    public void onDisconnect(String reason) {
        MusicPlayerMeow.getInstance().getLogger().info("Disconnected: " + reason);
        // Clean up all players
        if (globalPlayer != null) {
            globalPlayer.destroy();
            globalPlayer = null;
        }
        personalPlayers.values().forEach(PersonalMusicSender::stop);
        personalPlayers.clear();
        personalPlayerCreationTimes.clear();
    }

    @Override
    public void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet) {
        // Not needed
    }

    @Override
    public void onAudio(AudioS2CPacket packet) {
        try {
            //WHY DO WE HAVE TO DECODE OPUS DATA JUST TO RE-ENCODE IT AGAIN ?????
            // meow fixed it hehe~
            // P-V 107
            int samples = (switch (packet.codec().getRaw()) {
                case AAC, VORBIS ->
                    throw new RuntimeException("Can't Parse codec " + packet.codec());

                case OPUS -> {
                    byte[] opusData = packet.audioBytes();
                    yield decoder.decode(
                            opusData,
                            0,
                            opusData.length,
                            decodeBuffer,
                            0,
                            960, // frame size
                            false
                    );
                }

                case PCM -> {
                    int shortsWritten = packet.audioBytes().length / 2;
                    AudioConverter.convertToShortArray(packet.audioBytes(), decodeBuffer);
                    yield shortsWritten / 2;
                }

            });

            if (samples > 0) {
                short[] outFrame = Arrays.copyOf(decodeBuffer, decodeBuffer.length); // new short[samples];
                // playback works fine here with java sound thing

              /*  for (int i = 0; i < samples; i++) {
                    int left = decodeBuffer[i * 2];
                    int right = decodeBuffer[i * 2 + 1];
                    float l = left / 32768.0f;
                    float r = right / 32768.0f;
                    float mixed = (l + r) * 0.6f;
                    if (mixed > 1.0f) mixed = 1.0f;
                    if (mixed < -1.0f) mixed = -1.0f;

                    outFrame[i] = (short) (mixed * 32767.0f);
                }*/

                if (isGlobalGuildId(packet.guildId())) {
                    GlobalMusicSender player = getGlobalPlayer();
                    player.offer(outFrame);
                    if (MusicPlayerMeow.getInstance().musicManager.isDebug && player.getTotalFrames() % 50 == 0) {
                        MusicPlayerMeow.getInstance().getLogger().info("Buffer status: " + player.getTotalFrames() + " frames buffered.");
                    }
                } else {
                    PersonalMusicSender sender = getPersonalPlayer(packet.guildId());
                    if (sender != null) {
                        sender.offer(outFrame, (int) packet.sequence());
                        if (MusicPlayerMeow.getInstance().musicManager.isDebug && sender.getTotalFrames() % 50 == 0) {
                            MusicPlayerMeow.getInstance().getLogger().info("Buffer status: " + sender.getTotalFrames() + " frames buffered.");
                        }
                    }
                }
            }
        } catch (OpusException e) {
            MusicPlayerMeow.getInstance().getLogger().severe("Opus Decode Failed: " + e.getMessage());
        }
    }

}
