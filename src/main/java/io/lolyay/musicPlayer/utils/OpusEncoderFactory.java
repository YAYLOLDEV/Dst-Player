package io.lolyay.musicPlayer.utils;

import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.concentus.OpusApplication;
import de.maxhenkel.voicechat.concentus.OpusBandwidth;
import de.maxhenkel.voicechat.concentus.OpusSignal;
import io.github.jaredmdobson.concentus.OpusConstants;

import java.util.SimpleTimeZone;

public class OpusEncoderFactory implements OpusEncoder {

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SIZE = 960;
    private static final int CHANNELS = 2;        // Mono :sob:
    private static final int MAX_PAYLOAD_SIZE = 32000;

    private de.maxhenkel.voicechat.concentus.OpusEncoder encoder;
    private final byte[] buffer;
    private final int frameSize;

    public static OpusEncoderFactory create() {
        return new OpusEncoderFactory(SAMPLE_RATE, CHANNELS, FRAME_SIZE, MAX_PAYLOAD_SIZE, OpusApplication.OPUS_APPLICATION_AUDIO);
    }

    public OpusEncoderFactory(int sampleRate, int channels, int frameSize, int maxPayloadSize, OpusApplication application) {
        this.frameSize = frameSize;
        this.buffer = new byte[maxPayloadSize];

        try {
            this.encoder = new de.maxhenkel.voicechat.concentus.OpusEncoder(sampleRate, CHANNELS, OpusApplication.OPUS_APPLICATION_AUDIO);
            encoder.setBitrate(510000);
            encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
            encoder.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
            encoder.setApplication(OpusApplication.OPUS_APPLICATION_AUDIO);
            encoder.setMaxBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
            encoder.setUseVBR(true);
            encoder.setUseConstrainedVBR(false);
            encoder.setUseDTX(false);
            encoder.setPacketLossPercent(5);
            encoder.setLSBDepth(24);
            encoder.setPredictionDisabled(false);
            encoder.setComplexity(10);

        } catch (Exception e) {
            throw new IllegalStateException("Opus initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized byte[] encode(short[] rawAudio) {
        if (this.isClosed()) {
            throw new IllegalStateException("Encoder is closed");
        }

        try {
            int result = this.encoder.encode(rawAudio, 0, this.frameSize, this.buffer, 0, this.buffer.length);

            if (result < 0) {
                throw new RuntimeException("Opus encoding error. Code: " + result);
            }

            byte[] audio = new byte[result];
            System.arraycopy(this.buffer, 0, audio, 0, result);
            return audio;

        } catch (Exception e) {
            throw new RuntimeException("Critical failure during audio encoding", e);
        }
    }

    @Override
    public synchronized void resetState() {
        if (!this.isClosed()) {
            this.encoder.resetState();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return this.encoder == null;
    }

    @Override
    public synchronized void close() {
        this.encoder = null;
    }
}