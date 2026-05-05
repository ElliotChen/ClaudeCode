package tw.elliot.cctest.config;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID Version 7 generator per RFC 9562.
 * Uses Unix epoch milliseconds in the most significant 48 bits,
 * with random data filling the remaining bits.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        long randomBits = RANDOM.nextLong();

        long msb = (timestamp << 16) | (0x7000L) | (randomBits & 0x0FFFL);
        long lsb = (0x8000000000000000L) | (randomBits >>> 2 & 0x3FFFFFFFFFFFFFFFL);

        return new UUID(msb, lsb);
    }
}
