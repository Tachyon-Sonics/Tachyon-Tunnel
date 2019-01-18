package ch.tachyon.tunnel.demo.plugin.resample.impl;


/**
 * Factory for easily getting an {@link IResampler} implementation with given properties.
 * <p>
 * Note that an {@link IResampler} can only resample a single audio channel at a time. To resample
 * a stereo (or multi channel) audio signal, you must either create two (or more) {@link IResampler} with
 * the same parameters, or create one {@link IResampler} and use it to fully resample each channel one after
 * the other, invoking {@link IResampler#reset()} between the channels.
 */
public class ResamplerFactory {

    private final static int SINC_UPSAMPLE_FACTOR_LOW = 64; // 2^(nbits/2) --> 12 bits
    private final static int SINC_NB_ZERO_CROSS_LOW = 4;

    private final static int SINC_UPSAMPLE_FACTOR_MED = 256; // 2^(nbits/2) --> 16 bits
    private final static int SINC_NB_ZERO_CROSS_MED = 13;

    private final static int SINC_UPSAMPLE_FACTOR_HIGH = 1024; // 2^(nbits/2) --> 20 bits
    private final static int SINC_NB_ZERO_CROSS_HIGH = 40;

    private final static int SINC_UPSAMPLE_FACTOR_ULTRA = 4096; // 2^(nbits/2) --> 24 bits
    private final static int SINC_NB_ZERO_CROSS_ULTRA = 80;

    public final static int MIN_QUALITY = 0;
    public final static int MEDIUM_QUALITY = 2;
    public final static int MAX_QUALITY = 5;


    /**
     * Get an {@link IResampler} implementation with the given properties.
     * @param source the source from which the input samples will be read by the resampler
     * @param ratio the resampling ratio. Output rate / input rate, or output length / input length
     * @param quality the desired quality, between {@link #MIN_QUALITY} and {@link #MAX_QUALITY}. Note that
     * higher quality also means lower processing speed. If you are not sure, use {@link #MEDIUM_QUALITY} which
     * is suitable for most applications. Possible values are
     * <ul>
     * <li>0 - very low quality, not recommanded (sample and hold)
     * <li>1 - low quality, recommanded if fast processing is required (linear interpolation)
     * <li>2 - medium quality (sinc with small kernel)
     * <li>3 - high quality, suitable for most listening applications, 16bit precision (sinc with medium size kernel)
     * <li>4 - very high quality, suitable for editors, 20bits precision
     * <li>5 - ultra quality, 24bit precision (apart from round-off errors)
     * </ul>
     */
    public static IResampler getResampler(IAudioSource source, double ratio, int quality) {
        return getResampler(source, ratio, quality, false, 8192);
    }

    /**
     * Get an {@link IResampler} implementation with the given properties.
     * @param source the source from which the input samples will be read by the resampler
     * @param ratio the resampling ratio. Output rate / input rate, or output length / input length
     * @param quality the desired quality, between {@link #MIN_QUALITY} and {@link #MAX_QUALITY}
     * <ul>
     * <li>0 - very low quality, not recommanded (sample and hold)
     * <li>1 - low quality, recommanded if fast processing is required (linear interpolation)
     * <li>2 - medium quality (sinc with small kernel)
     * <li>3 - high quality, suitable for most listening applications, 16bit precision (sinc with medium size kernel)
     * <li>4 - very high quality, suitable for editors, 20bits precision
     * <li>5 - ultra quality, 24bit precision (apart from round-off errors)
     * </ul>
     * @param sweepable whether you want a resampler with which the resampling ratio can be changed while
     * processing. When <code>true</code>, the result can be casted to {@link ISweepableResampler}.
     */
    public static IResampler getResampler(IAudioSource source, double ratio, int quality, boolean sweepable) {
        return getResampler(source, ratio, quality, sweepable, 8192);
    }

    /**
     * Get an {@link IResampler} implementation with the given properties
     * @param source the source from which the input samples will be read by the resampler
     * @param ratio the resampling ratio. Output rate / input rate, or output length / input length
     * @param quality the desired quality, between {@link #MIN_QUALITY} and {@link #MAX_QUALITY}
     * <ul>
     * <li>0 - very low quality, not recommanded (sample and hold)
     * <li>1 - low quality, recommanded if fast processing is required (linear interpolation)
     * <li>2 - medium quality (sinc with small kernel)
     * <li>3 - high quality, suitable for most listening applications, 16bit precision (sinc with medium size kernel)
     * <li>4 - very high quality, suitable for editors, 20bits precision
     * <li>5 - ultra quality, 24bit precision (apart from round-off errors)
     * </ul>
     * @param sweepable whether you want a resampler with which the resampling ratio can be changed while
     * processing. When <code>true</code>, the result can be casted to {@link ISweepableResampler}.
     * @param bufferSize the desired buffer size. 8192 should be ok for most purpose. Use a smaller value such as
     * 128 or 64 if low latency is desired.
     */
    public static IResampler getResampler(IAudioSource source, double ratio, int quality, boolean sweepable, int bufferSize) {
        switch (quality) {
        case 0:
            return new SampleAndHoldResampler(source, ratio, bufferSize);
        case 1:
            return new LinearResampler(source, ratio, bufferSize);
        case 2:
            if (sweepable)
                return new SweepableSincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_LOW,
                        SINC_NB_ZERO_CROSS_LOW, 1.0f, bufferSize);
            else
                return new SincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_LOW,
                        SINC_NB_ZERO_CROSS_LOW, 1.0f, bufferSize);
        case 3:
            if (sweepable)
                return new SweepableSincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_MED,
                        SINC_NB_ZERO_CROSS_MED, 1.0f, bufferSize);
            else
                return new SincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_MED,
                        SINC_NB_ZERO_CROSS_MED, 1.0f, bufferSize);
        case 4:
            if (sweepable)
                return new SweepableSincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_HIGH,
                        SINC_NB_ZERO_CROSS_HIGH, 1.0f, bufferSize);
            else
                return new SincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_HIGH,
                        SINC_NB_ZERO_CROSS_HIGH, 1.0f, bufferSize);
        case 5:
            if (sweepable)
                return new SweepableSincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_ULTRA,
                        SINC_NB_ZERO_CROSS_ULTRA, 1.0f, bufferSize);
            else
                return new SincResampler(source, ratio, SINC_UPSAMPLE_FACTOR_ULTRA,
                        SINC_NB_ZERO_CROSS_ULTRA, 1.0f, bufferSize);
        }
        throw new IllegalArgumentException("quality must be between 0 and 5");
    }

}
