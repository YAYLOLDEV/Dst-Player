package io.lolyay.musicPlayer.music;

import javax.sound.sampled.*;
import java.util.concurrent.LinkedBlockingQueue;

public class LocalPlayer {
    private static final SourceDataLine line;
    // LinkedBlockingQueue is thread-safe and handles waiting/signaling automatically
    private static final LinkedBlockingQueue<short[]> queue = new LinkedBlockingQueue<>();

    static {
        try {
            AudioFormat af = new AudioFormat(48000, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af);
            line.start();

            // Background playback worker
            Thread playbackThread = new Thread(() -> {
                while (true) {
                    try {
                        short[] samples = queue.take(); // Blocks until data is available
                        byte[] byteBuffer = new byte[samples.length * 2];

                        for (int i = 0; i < samples.length; i++) {
                            // Little-endian conversion
                            byteBuffer[i * 2] = (byte) (samples[i] & 0xFF);
                            byteBuffer[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
                        }
                        line.write(byteBuffer, 0, byteBuffer.length);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });

            playbackThread.setDaemon(true);
            playbackThread.start();

        } catch (LineUnavailableException e) {
            throw new RuntimeException("Could not initialize Audio Line", e);
        }
    }

    /**
     * Adds 48kHz 2-channel PCM samples to the playback queue.
     */
    public static void addSample(short[] audio, int samplesPerChannel) {
        // Opus stereo returns (samplesPerChannel * channels) total shorts
        int totalShorts = samplesPerChannel * 2;

        short[] trim = new short[totalShorts];
        System.arraycopy(audio, 0, trim, 0, totalShorts);
        queue.add(trim);
    }
}