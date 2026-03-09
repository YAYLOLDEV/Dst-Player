package io.lolyay.musicPlayer.utils;

import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.concentus.OpusApplication;
import de.maxhenkel.voicechat.concentus.OpusBandwidth;
import de.maxhenkel.voicechat.concentus.OpusConstants;
import de.maxhenkel.voicechat.concentus.OpusSignal;

public class OpusEncoderFactory implements OpusEncoder {

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SIZE = 960;
    private static final int CHANNELS = 1;        // Mono :sob:
    private static final int MAX_PAYLOAD_SIZE = 4000;

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
            this.encoder = new de.maxhenkel.voicechat.concentus.OpusEncoder(sampleRate, channels, application);
            this.encoder.setBitrate(OpusConstants.OPUS_BITRATE_MAX);
            this.encoder.setComplexity(10);
            this.encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
            this.encoder.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
            this.encoder.setUseVBR(true);
            this.encoder.setLSBDepth(24);
            this.encoder.setUseDTX(false);
            this.encoder.setPacketLossPercent(0);
            this.encoder.setPredictionDisabled(false);

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