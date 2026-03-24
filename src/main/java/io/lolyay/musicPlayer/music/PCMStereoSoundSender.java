package io.lolyay.musicPlayer.music;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.plugins.impl.audiochannel.AudioPlayerImpl;
import io.lolyay.musicPlayer.utils.FakeOpusEncoderFactory;
import io.lolyay.musicPlayer.utils.OpusEncoderFactory;

import java.util.function.Supplier;

public class PCMStereoSoundSender extends AudioPlayerImpl {
    private final Supplier<short[]> supplier;
    private final AudioChannel channel;
    private final OpusEncoder enc;

    public PCMStereoSoundSender(AudioChannel audioChannel, Supplier<short[]> audioSupplier) {
        super(audioChannel, FakeOpusEncoderFactory.create(), audioSupplier);
        this.enc = FakeOpusEncoderFactory.create();
        this.supplier = audioSupplier;
        this.channel = audioChannel;
    }

    @Override
    public void run() {
        int framePosition = 0;
        long startTime = System.nanoTime();

        short[] frame;
        while ((frame = this.supplier.get()) != null) {
            byte[] d = this.enc.encode(frame);
            this.channel.send(d);
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
