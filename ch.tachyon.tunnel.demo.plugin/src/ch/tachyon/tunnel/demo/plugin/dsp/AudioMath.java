package ch.tachyon.tunnel.demo.plugin.dsp;


public class AudioMath {

    /**
     * Minimum float value that can be inverted (1.0f / value) without producing infinity
     */
    public static final float MIN_INV = 0x1.0p-126f; // 1.17549435E-38f

    /**
     * Minimum gain in (dB) below which the gain is considered to be minus infinity
     * (or zero on a linear scale)
     */
    public final static double MIN_GAIN = -100.0;


    public static int log2(int size) {
        assert (size & (size - 1)) == 0: "size must be a power of two";
        int log = 0;
        if (size >= (1 << 16)) {
            log += 16;
            size >>>= 16;
        }
        if (size >= (1 << 8)) {
            log += 8;
            size >>>= 8;
        }
        if (size >= (1 << 4)) {
            log += 4;
            size >>>= 4;
        }
        if (size >= (1 << 2)) {
            log += 2;
            size >>>= 2;
        }
        if (size >= 2)
            log++;
        return log;
    }

    public static double dbToLevel(double value) {
        return Math.pow(10.0, value / 20.0);
    }

    public static double levelToDb(double value) {
        return 20.0 * Math.log10(value);
    }

    public static double dbToLevel0(double value) {
        if (value <= MIN_GAIN)
            return 0.0;
        return dbToLevel(value);
    }

    public static double levelToDb0(double value) {
        double result = levelToDb(value);
        if (result < MIN_GAIN)
            return MIN_GAIN;
        return result;
    }

    public static double dbToPowerLevel(double value) {
        return Math.pow(10.0, value / 10.0);
    }

    public static double powerLevelToDb(double value) {
        return 10.0 * Math.log10(value);
    }

    public static double powerLevelToDb0(double value) {
        double result = powerLevelToDb(value);
        if (result < MIN_GAIN)
            return MIN_GAIN;
        return result;
    }

    public static float energy(float[] samples) {
        float total = 0.0f;
        for (float value : samples)
            total += value * value;
        return (float) Math.sqrt(total / samples.length);
    }

    /**
     * Get the greatest common (integer) divisor of two values.
     */
    public static double gcd(double a, double b) {
        double epsilon = 1.0 / (double)(1L << 32);
        while (a > epsilon) {
            if (a < b) {
                // Swap
                double swap = a;
                a = b;
                b = swap;
            }
            a = a % b;
        }
        return b;
    }

    public static double gcd(double... values) {
        if (values.length == 0)
            throw new IllegalArgumentException("gcd() requires at least one value");
        double result = values[0];
        for (int i = 1; i < values.length; i++)
            result = gcd(result, values[i]);
        return result;
    }

    public static boolean isPowerOfTwo(int value) {
        if (value <= 0)
            return false;
        return (value & (value - 1)) == 0;
    }

}
