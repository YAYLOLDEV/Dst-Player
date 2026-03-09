package io.lolyay.musicPlayerMeow.music;

import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import io.lolyay.musicPlayerMeow.utils.OpusEncoderFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class AbstractMusicSender implements Supplier<short[]> {
    protected final LinkedBlockingQueue<short[]> queue = new LinkedBlockingQueue<>();
    protected AudioPlayer audioPlayer;
    protected final AtomicBoolean isPlaying = new AtomicBoolean(false);
    protected final AtomicBoolean startRequested = new AtomicBoolean(false);
    protected final AtomicBoolean isPaused = new AtomicBoolean(false);
    protected StaticAudioChannel channel;
    
    private static final int MAX_SILENCE_FRAMES = 50; // 1 second of silence before stopping
    private int silenceCounter = 0;
    private static final short[] SILENCE_FRAME = new short[960]; // 20ms of silence
    protected long totalFrames = 0;

    public AbstractMusicSender() {
        if (VoiceChatPlugin.voicechatServerApi == null) {
            throw new IllegalStateException("VoiceChat API not initialized!");
        }
    }

    protected void initAudioPlayer(StaticAudioChannel channel) {
        this.channel = channel;
        if (this.audioPlayer != null) {
            destroy();
        }
        this.audioPlayer = VoiceChatPlugin.voicechatServerApi.createAudioPlayer(
                channel,
                OpusEncoderFactory.create(),
                this
        );
        queue.clear();
        silenceCounter = 0;
        totalFrames = 0;
    }

    public synchronized void start() {
        if (channel == null && audioPlayer == null) {
            return;
        }

        // Reset pause state on new start
        isPaused.set(false);
        
        if (startRequested.compareAndSet(false, true)) {
            if (!isPlaying.get()) {
                // Always recreate the player to ensure a fresh state
                // This fixes the issue where a stopped player (due to track end) cannot be restarted
                if (audioPlayer != null) {
                    try {
                        audioPlayer.stopPlaying();
                    } catch (Exception ignored) {}
                }
                
                this.audioPlayer = VoiceChatPlugin.voicechatServerApi.createAudioPlayer(
                        channel,
                        OpusEncoderFactory.create(),
                        this
                );
                
                audioPlayer.startPlaying();
                isPlaying.set(true);
                silenceCounter = 0;
                totalFrames = 0;
            }
        }
    }

    public synchronized void stop() {
        if (audioPlayer != null) {
            audioPlayer.stopPlaying();
        }
        isPlaying.set(false);
        startRequested.set(false);
        isPaused.set(false);
        queue.clear();
        silenceCounter = 0;
        onStop();
    }

    public synchronized void pause() {
        if (audioPlayer != null && isPlaying.get()) {
            isPaused.set(true);
        }
    }

    public synchronized void resume() {
        if (audioPlayer != null && isPlaying.get() && isPaused.get()) {
            isPaused.set(false);
        }
    }

    public void clean() {
        queue.clear();
        silenceCounter = 0;
        totalFrames = 0;
        isPaused.set(false);
        startRequested.set(false);
    }

    public void offer(short[] data) {
        queue.offer(data);
        totalFrames++;
    }
    
    public long getTotalFrames() {
        return totalFrames;
    }

    public void destroy() {
        stop();
        audioPlayer = null;
    }
    
    // Hook for subclasses
    protected void onStop() {}

    @Override
    public short[] get() {
        if (isPaused.get()) {
            return SILENCE_FRAME;
        }

        try {
            short[] frame = queue.poll(20, TimeUnit.MILLISECONDS);
            
            if (frame != null) {
                silenceCounter = 0;
                return frame;
            }
            
            if (silenceCounter < MAX_SILENCE_FRAMES) {
                silenceCounter++;
                return SILENCE_FRAME;
            } else {
                // Too much silence, stop playback
                if (isPlaying.get()) {
                    isPlaying.set(false);
                    startRequested.set(false);
                    onStop();
                }
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
