package io.lolyay.musicPlayer;

import java.util.UUID;
import java.security.SecureRandom;

public class CustomUUID {
    private static final SecureRandom random = new SecureRandom();

    public static UUID generateCustomV8() {
        long msb = random.nextLong();
        long lsb = random.nextLong();
        msb &= ~0xF000L;
        msb |= 0x8000L;
        lsb &= ~0xC000000000000000L;
        lsb |= 0x8000000000000000L;
        return new UUID(msb, lsb);
    }

    public static UUID generateCustomV9() {
        long msb = random.nextLong();
        long lsb = random.nextLong();
        msb &= ~0xF000L;
        msb |= 0x9000L;
        lsb &= ~0xC000000000000000L;
        lsb |= 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}