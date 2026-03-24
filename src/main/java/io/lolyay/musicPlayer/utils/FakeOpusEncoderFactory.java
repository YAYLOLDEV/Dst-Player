package io.lolyay.musicPlayer.utils;

import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.concentus.OpusApplication;
import de.maxhenkel.voicechat.concentus.OpusBandwidth;
import de.maxhenkel.voicechat.concentus.OpusSignal;
import io.lolyay.discordmsend.network.protocol.codec.AudioConverter;

public class FakeOpusEncoderFactory implements OpusEncoder {

    public static FakeOpusEncoderFactory create() {
        System.err.println("CREATING");
        return new FakeOpusEncoderFactory();
    }


    @Override
    public synchronized byte[] encode(short[] rawAudio) {
        System.err.println("ENCODING");
        byte[] audio = new byte[rawAudio.length * 2];
        AudioConverter.convertToByteArray(rawAudio, audio);
        return audio;
    }

    @Override
    public synchronized void resetState() {

    }

    @Override
    public synchronized boolean isClosed() {
        return false;
    }

    @Override
    public synchronized void close() {
    }
}