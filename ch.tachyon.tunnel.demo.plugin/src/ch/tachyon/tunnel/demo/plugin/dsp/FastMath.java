package ch.tachyon.tunnel.demo.plugin.dsp;

/**
 * Fast approximations of various math functions.
 */
public class FastMath {

    private final static double B = 4.0 / Math.PI;
    private final static double C = -4.0 / (Math.PI * Math.PI);
    private final static double P = 0.218;
    private final static double hPI = Math.PI / 2.0;
    private final static double PI2 = Math.PI * 2.0;
    private final static double sPI = Math.PI * 1.5;

    /**
     * Wrap an angle so that it is in the [-&pi;..&pi;] range.
     * <p>
     * <b>Warning:</b> the input angle is assumed to be in the [-2&pi;..2&pi;] range! This is hence a very limited wrapping,
     * just sufficient to properly implement some of the other fast methods.
     */
    private static double wrap(double angle) {
        angle = angle % PI2;
        if (angle > Math.PI)
            angle -= PI2;
        else if (angle < -Math.PI)
            angle += PI2;
        assert angle >= -Math.PI && angle <= Math.PI;
        return angle;
    }

    /**
     * Fast sine approximation
     * @param theta the argument, assumed to be in the [-2&pi;..2&pi;] range
     * @return sin(theta)
     */
    public static double fastSin(double theta) {
        return fastSin0(wrap(theta));
    }

    /**
     * Fast cosine approximation
     * @param theta the argument, assumed to be in the [-2&pi;..2&pi;] range
     * @return cos(theta)
     */
    public static double fastCos(double theta) {
        return fastSin0(wrap(theta + hPI));
    }

    /**
     * Fast sine approximation. Slightly faster than
     * {@link #fastSin(double)} but the argument must
     * be within the [-&pi;..&pi;] range!
     * @param theta the argument.
     * @return sin(theta)
     */
    public static double fastSin0(double theta) {
        double y = B * theta + C * theta * Math.abs(theta);
        y = P * (y * Math.abs(y) - y) + y;
        return y;
    }

    /**
     * Fast cosine approximation. Can be slightly faster than
     * {@link #fastCos(double)} but the argument must
     * be within the [-&pi;..&pi;] range!
     * @param theta the argument.
     * @return cos(theta)
     */
    public static double fastCos0(double theta) {
        if (theta > hPI) {
            theta -= sPI; // - 1.5π
        } else {
            theta += hPI; // + 0.5π
        }
        return fastSin0(theta);
    }

    /*
     * atan, atan2 by http://www.developer.nokia.com/Community/Discussion/showthread.php?72840-Maths-acos-asin-atan
     */

    private static final double sq2p1 = 2.414213562373095048802e0;
    private static final double sq2m1 = .414213562373095048802e0;
    private static final double p4 = .161536412982230228262e2;
    private static final double p3 = .26842548195503973794141e3;
    private static final double p2 = .11530293515404850115428136e4;
    private static final double p1 = .178040631643319697105464587e4;
    private static final double p0 = .89678597403663861959987488e3;
    private static final double q4 = .5895697050844462222791e2;
    private static final double q3 = .536265374031215315104235e3;
    private static final double q2 = .16667838148816337184521798e4;
    private static final double q1 = .207933497444540981287275926e4;
    private static final double q0 = .89678597403663861962481162e3;
    private static final double PIO2 = 1.5707963267948966135E0;

    private static double mxatan(double arg) {
        double argsq = arg * arg, value;
        value = ((((p4 * argsq + p3) * argsq + p2) * argsq + p1) * argsq + p0);
        value = value / (((((argsq + q4) * argsq + q3) * argsq + q2) * argsq + q1) * argsq + q0);
        return value * arg;
    }

    private static double msatan(double arg) {
        return arg < sq2m1 ? mxatan(arg) : arg > sq2p1 ? PIO2 - mxatan(1 / arg) : PIO2 / 2 + mxatan((arg - 1) / (arg + 1));
    }

    public static double atan(double arg) {
        return arg > 0 ? msatan(arg) : -msatan(-arg);
    }

    public static double atan2(double arg1, double arg2) {
        if (arg1 + arg2 == arg1)
            return arg1 >= 0 ? PIO2 : -PIO2;
        arg1 = atan(arg1 / arg2);
        return arg2 < 0 ? arg1 <= 0 ? arg1 + Math.PI : arg1 - Math.PI : arg1;
    }

    // Following is untested

    public static double exp(double val) {
        final long tmp = (long) (1512775 * val + 1072632447);
        return Double.longBitsToDouble(tmp << 32);
    }

    public static double log(double x) {
        return 6 * (x - 1) / (x + 1 + 4 * (Math.sqrt(x)));
    }

    public static double pow(final double a, final double b) {
        final int x = (int) (Double.doubleToLongBits(a) >> 32);
        final int y = (int) (b * (x - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long)y) << 32);
    }

    public static float floor(float value) {
        assert value >= 0.0f;
        return (float) (int) value;
    }

    public static double floor(double value) {
        assert value >= 0.0;
        return (double) (long) value;
    }

    public static double fastSqrt(double a) {
        final long x = Double.doubleToLongBits(a) >> 32;
        double y = Double.longBitsToDouble((x + 1072632448) << 31);
        return y;
    }

    public static double sqrt(double a) {
        final long x = Double.doubleToLongBits(a) >> 32;
        double y = Double.longBitsToDouble((x + 1072632448) << 31);
        y = (y + a / y) * 0.5;
        return y;
    }

    /**
     * Fast version of {@link Math#max(float, float)} that does not handle {@link Float#isNaN()} and
     * infinite values
     */
    public static float max(float v1, float v2) {
        return v1 > v2 ? v1 : v2;
    }

    public static float min(float v1, float v2) {
        return v1 > v2 ? v2 : v1;
    }

}
