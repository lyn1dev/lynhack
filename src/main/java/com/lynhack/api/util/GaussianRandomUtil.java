package com.lynhack.api.util;

import java.util.Random;

public final class GaussianRandomUtil {
    private static final Random RANDOM = new Random();

    private GaussianRandomUtil() {}

    /**
     * Generates a Gaussian (Normal/Bell Curve) distributed long value clamped between min and max.
     *
     * @param mean   The center/average value of the distribution (mu)
     * @param stdDev The standard deviation (sigma)
     * @param min    Minimum clamp bound
     * @param max    Maximum clamp bound
     * @return Clamped gaussian random value
     */
    public static long nextGaussianLong(double mean, double stdDev, long min, long max) {
        double z = RANDOM.nextGaussian();
        double sample = mean + (z * stdDev);
        return Math.round(Math.clamp(sample, (double) min, (double) max));
    }

    /**
     * Default humanized key hold duration (mu = 70ms, sigma = 10ms, bounds = 40ms to 100ms).
     */
    public static long nextKeyHoldDuration() {
        return nextGaussianLong(70.0, 10.0, 40L, 100L);
    }
}
