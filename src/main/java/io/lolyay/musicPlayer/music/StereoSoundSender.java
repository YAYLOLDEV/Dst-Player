package io.lolyay.musicPlayer.music;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.plugins.impl.audiochannel.AudioPlayerImpl;
import de.maxhenkel.voicechat.plugins.impl.audiochannel.AudioSupplier;
import io.lolyay.musicPlayer.utils.OpusEncoderFactory;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

public class StereoSoundSender extends AudioPlayerImpl {
    private final Supplier<short[]> supplier;
    private final AudioChannel channel;
    private final OpusEncoder enc;

    public StereoSoundSender(AudioChannel audioChannel, Supplier<short[]> audioSupplier) {
        super(audioChannel, null, audioSupplier);
        this.enc = OpusEncoderFactory.create();
        this.supplier = audioSupplier;
        this.channel = audioChannel;
    }

    @Override
    public void run() {
        int framePosition = 0;
        long startTime = System.nanoTime();

        short[] frame;
        while ((frame = this.supplier.get()) != null) {
            if (frame.length != (960 * 2)) { // STEREO!
                Voicechat.LOGGER.error("Got invalid audio frame size {}!={}", frame.length, 960);
                break;
            }

            this.channel.send(this.enc.encode(frame));
            ++framePosition;
            long waitTimestamp = startTime + (long) framePosition * 20000000L;
            long waitNanos = waitTimestamp - System.nanoTime();

            try {
                if (waitNanos > 0L) {
                    Thread.sleep(waitNanos / 1000000L, (int) (waitNanos % 1000000L));
                }
            } catch (InterruptedException var10) {
                break;
            }
        }

        this.enc.close();
        this.channel.flush();
    }
}
