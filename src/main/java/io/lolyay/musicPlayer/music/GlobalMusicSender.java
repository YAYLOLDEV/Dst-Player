package io.lolyay.musicPlayer.music;

public class GlobalMusicSender extends AbstractMusicSender {

    public GlobalMusicSender() {
        super();
        if (VoiceChatPlugin.getGlobalChannel() == null) {
            throw new IllegalStateException("Global channel not created!");
        }
        initAudioPlayer(VoiceChatPlugin.getGlobalChannel());
    }
}
