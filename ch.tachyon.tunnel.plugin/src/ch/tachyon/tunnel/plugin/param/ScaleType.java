package ch.tachyon.tunnel.plugin.param;

/**
 * Indicate how the value of a parameter must be displayed on a slider.
 * More precisely, this specifies the transformation to apply between the actual value
 * and the slider's position.
 * <p>
 * In all cases, the slider minimum and maximum values match the parameter minimum and maximum values.
 */
public enum ScaleType {
    /**
     * Linear scaling from actual value to slider value.
     * The slider position is proportional to the value.
     * This is the most common values for parameters for which the value that does
     * nothing is 0.
     */
    LINEAR("Linear") {

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            return value;
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            return value;
        }

    },
    /**
     * Logarithmic scaling from actual value to slider value.
     * The slider position is proportional to the logarithm of the value.
     * This scaling can be used for any parameter corresponding to a ratio, and for which the
     * "center" value that does nothing is 1.
     * <p>
     * This option is only legal if the minimum and maximum are both non-negative or both non-positive.
     * Else, {@link #CUBEROOT} will be used.
     */
    LOGARITHMIC("Logarithmic") {

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            if ((minValue >= 0.0) == (maxValue >= 0.0)) {
                double min = minValue;
                double max = maxValue;
                if (min == 0.0) {
                    double incr = maxValue / 1000.0;
                    min += incr;
                    max += incr;
                    value += incr;
                }
                double logMin = log2(Math.abs(min));
                double logMax = log2(Math.abs(max));
                double logValue = log2(Math.abs(value));
                double normalized = (logValue - logMin) / (logMax - logMin);
                return minValue + normalized * (max - min);
            } else {
                return CUBEROOT.forwScale(value, minValue, maxValue);
            }
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            if ((minValue >= 0) == (maxValue >= 0)) {
                double min = minValue;
                double max = maxValue;
                if (minValue == 0.0) {
                    double incr = maxValue / 1000.0;
                    min += incr;
                    max += incr;
                }
                double normalized = (value - minValue) / (max - min);
                double logMin = log2(Math.abs(min));
                double logMax = log2(Math.abs(max));
                double scaled = logMin + normalized * (logMax - logMin);
                double result =  exp2(scaled);
                if (minValue == 0.0)
                    result -= maxValue / 1000.0;
                return result;
            } else {
                return CUBEROOT.backScale(value, minValue, maxValue);
            }
        }

    },
    /**
     * Cubic scaling from actual value to slider value.
     * The slider position is proportional to the cube of the value. This is rarely used.
     * It can be used to favor values with a large absolute value.
     */
    CUBE("Cubic") {

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            double cbMin = Math.pow(minValue, 3.0);
            double cbMax = Math.pow(maxValue, 3.0);
            double cbValue = Math.pow(value, 3.0);
            double normalized = (cbValue - cbMin) / (cbMax - cbMin);
            return minValue + normalized * (maxValue - minValue);
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            double normalized = (value - minValue) / (maxValue - minValue);
            double cbMin = Math.pow(minValue, 3.0);
            double cbMax = Math.pow(maxValue, 3.0);
            double scaled = cbMin + normalized * (cbMax - cbMin);
            return Math.cbrt(scaled);
        }

    },
    /**
     * Square root scaling from actual value to slider value.
     * The slider position is proportional to the square root of the value. This can
     * be used to favor values around 0 for a parameter that has extreme min and max, but for which values
     * around 0 are most relevant in practice.
     */
    SQUAREROOT("Square root") {
        
        private double sqRoot(double value) {
            if (value < 0.0)
                return - Math.sqrt(-value);
            else
                return Math.sqrt(value);
        }

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            double crMin = sqRoot(minValue);
            double crMax = sqRoot(maxValue);
            double crValue = sqRoot(value);
            double normalized = (crValue - crMin) / (crMax - crMin);
            return  minValue + normalized * (maxValue - minValue);
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            double normalized = (value - minValue) / (maxValue - minValue);
            double crMin = sqRoot(minValue);
            double crMax = sqRoot(maxValue);
            double scaled = crMin + normalized * (crMax - crMin);
            return (scaled >= 0.0 ? scaled * scaled : -scaled * scaled);
        }

    },
    /**
     * Cube root scaling from actual value to slider value.
     * The slider position is proportional to the cube root of the value. This can
     * be used to favor values around 0 for a parameter that has extreme min and max, but for which values
     * around 0 are most relevant in practice. Stronger than {@link #SQUAREROOT}
     */
    CUBEROOT("Cube root") {

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            double crMin = Math.cbrt(minValue);
            double crMax = Math.cbrt(maxValue);
            double crValue = Math.cbrt(value);
            double normalized = (crValue - crMin) / (crMax - crMin);
            return  minValue + normalized * (maxValue - minValue);
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            double normalized = (value - minValue) / (maxValue - minValue);
            double crMin = Math.cbrt(minValue);
            double crMax = Math.cbrt(maxValue);
            double scaled = crMin + normalized * (crMax - crMin);
            return Math.pow(scaled, 3.0);
        }

    },
    /**
     * Logarithmic and cube root scaling from actual value to slider value.
     * The slider position is proportional to the cube root of the logarithm
     * of the value. The can be used to favor values around 1 for a parameter that is a ratio, and has
     * extreme min and max, but for which values around 1 are most relevant in practice.
     * <p>
     * This option is only legal if the minimum and maximum are both non-negative or both non-positive.
     * Else {@link #CUBEROOT} only will be used.
     */
    LOG_CUBEROOT("Log + Cube root") {

        @Override
        public double forwScale(double value, double minValue, double maxValue) {
            if ((minValue >= 0.0) == (maxValue >= 0.0)) {
                double min = minValue;
                double max = maxValue;
                if (min == 0.0) {
                    double incr = maxValue / 1000.0;
                    min += incr;
                    max += incr;
                    value += incr;
                }
                double lcMin = Math.cbrt(log2(Math.abs(min)));
                double lcMax = Math.cbrt(log2(Math.abs(max)));
                double lcValue = Math.cbrt(log2(Math.abs(value)));
                double normalized = (lcValue - lcMin) / (lcMax - lcMin);
                return minValue + normalized * (max - min);
            } else {
                return CUBEROOT.forwScale(value, minValue, maxValue);
            }
        }

        @Override
        public double backScale(double value, double minValue, double maxValue) {
            if ((minValue >= 0) == (maxValue >= 0)) {
                double min = minValue;
                double max = maxValue;
                if (minValue == 0.0) {
                    double incr = maxValue / 1000.0;
                    min += incr;
                    max += incr;
                }
                double normalized = (value - minValue) / (max - min);
                double lcMin = Math.cbrt(log2(Math.abs(min)));
                double lcMax = Math.cbrt(log2(Math.abs(max)));
                double scaled = lcMin + normalized * (lcMax - lcMin);
                double result =  exp2(Math.pow(scaled, 3.0));
                if (minValue == 0.0)
                    result -= maxValue / 1000.0;
                return result;
            } else {
                return CUBEROOT.backScale(value, minValue, maxValue);
            }
        }

    };

    private String name;

    private ScaleType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public abstract double forwScale(double value, double minValue, double maxValue);

    /**
     * Convert slider value to actual value
     */
    public abstract double backScale(double value, double minValue, double maxValue);


    private static final double LOG_2 = Math.log(2.0);

    private static double log2(double value) {
        return Math.log(value) / LOG_2;
    }

    private static double exp2(double value) {
        return Math.exp(value * LOG_2);
    }

}
