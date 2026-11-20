package ch.tachyon.tunnel.dsp.fft;

import java.util.Arrays;


public class Windows {

    public final static float[] HannCoefs = { 0.5f, -0.25f };
    public final static float[] HammingCoefs = { 0.53836f, -0.23082f };
    public final static float[] BlackmanCoefs = { 0.42f, -0.25f, 0.04f };
    public final static float[] NutallCoefs = { 0.36335819f, -0.24458875f, 0.06829975f, -0.00532055f };
    public final static float[] FlatTopCoefs = { 0.25f, -0.24125f, 0.16125f, -0.04225f, 0.004f };


    public static void fillWindow(float[] window, int size, float[] coefs) {
        for (int i = 0; i < size; i++) {
            double x = Math.PI * 2.0 * (double) i / (double) size;
            double val = coefs[0];
            for (int c = 1; c < coefs.length; c++)
                val += (coefs[c] * 2.0f * Math.cos(x * (double) c));
            window[i] = (float) val;
        }
    }

    public static void fillSymmetricWindow(float[] window, int size, float[] coefs) {
        for (int i = 0; i < size; i++) {
            double x = Math.PI * 2.0 * (double) (i + 1) / (double) (size + 1);
            double val = coefs[0];
            for (int c = 1; c < coefs.length; c++)
                val += (coefs[c] * 2.0f * Math.cos(x * (double) c));
            window[i] = (float) val;
        }
    }

    public static void fillHannWindow(float[] window, int size) {
        fillWindow(window, size, HannCoefs);
    }

    public static void fillSymmetricHannWindow(float[] window, int size) {
        fillSymmetricWindow(window, size, HannCoefs);
    }

    public static void fillHannWindow(float[] window) {
        fillHannWindow(window, window.length);
    }

    public static void fillSymmetricHannWindow(float[] window) {
        fillSymmetricHannWindow(window, window.length);
    }

    public static void fillHammingWindow(float[] window, int size) {
        fillWindow(window, size, HammingCoefs);
    }

    public static void fillHammingWindow(float[] window) {
        fillHammingWindow(window, window.length);
    }

    public static void fillBlackmanWindow(float[] window, int size) {
        fillWindow(window, size, BlackmanCoefs);
    }

    public static void fillBlackmanWindow(float[] window) {
        fillBlackmanWindow(window, window.length);
    }

    public static void fillNutallWindow(float[] window, int size) {
        fillWindow(window, size, NutallCoefs);
    }

    public static void fillNutallWindow(float[] window) {
        fillNutallWindow(window, window.length);
    }

    public static void fillFlatTopWindow(float[] window, int size) {
        fillWindow(window, size, FlatTopCoefs);
    }

    public static void fillFlatTopWindow(float[] window) {
        fillNutallWindow(window, window.length);
    }

    /**
     * Fill a "shrunk" (or zero padded) window.
     * <p>
     * The window size is reduced be the given <code>shrink</code> factor and centered.
     * The array is filled with zero on both sides of the shrunk window.
     */
    public static void fillShrunkWindow(float[] window, float[] coefs, float shrink) {
        fillShrunkWindow(window, coefs, shrink, true);
    }

    /**
     * Fill a "shrunk" (or zero padded) window.
     * <p>
     * The window size is reduced be the given <code>shrink</code> factor and centered.
     * The array is filled with zero on both sides of the shrunk window.
     * @param energyCorrection whether to multiply the coefficients so that the resulting window
     * has the same energy as the unshrunk (normal) version of the window
     */
    public static void fillShrunkWindow(float[] window, float[] coefs, float shrink, boolean energyCorrection) {
        int shrunkSize = (int) ((float) window.length / shrink + 0.5f);
        Windows.fillWindow(window, shrunkSize, coefs);
        if (shrink != 1.0f) {
            int start = (window.length - shrunkSize) / 2;
            int stop = start + shrunkSize;
            System.arraycopy(window, 0, window, start, shrunkSize);
            if (energyCorrection) {
                float correction = (float) Math.sqrt(shrink);
                for (int i = start; i < stop; i++)
                    window[i] *= correction;
            }
            Arrays.fill(window, 0, start, 0.0f);
            Arrays.fill(window, stop, window.length, 0.0f);
        }
    }

    /**
     * Create a triangular window in a pre-allocated array.
     * @param window the window, filled by this call
     * @param size the size of the window
     */
    public static void fillTriangularWindow(float[] window, int size) {
        int half = size / 2;
        for (int i = 0; i < half; i++) {
            float value = (float) i / (float) half;
            window[i] = value;
            window[i + half] = 1.0f - value;
        }
    }

    public static void fillTriangularWindow(float[] window) {
        fillTriangularWindow(window, window.length);
    }

    private static double iZero(double x) {
        final double epsilon = 1E-21;
        double sum, u, halfx, temp;
        long n;

        sum = 1.0;
        u = 1.0;
        n = 1;
        halfx = x / 2.0;
        do {
            temp = halfx / (double) n;
            n += 1;
            temp *= temp;
            u *= temp;
            sum += u;
        } while (u >= epsilon * sum);
        return sum;
    }

    /**
     * Get the beta value to pass to {@link #fillKaiserWindow(float[], int, double)} in order to have
     * the given average side lobes attenuation
     * @param db average side lobes attenuation, in db, positive
     */
    public static double getKaiserBeta(double db) {
        double beta = 0.0;
        if (db >= 50.0f)
            beta = 0.1102 * (db - 8.7);
        else if (db > 21.0f)
            beta = 0.5842 * Math.pow(db - 21.0, 0.4) + 0.07886 * (db - 21.0);
        else
            beta = 0.0;
        return beta;
    }

    /**
     * Create the parametrable Kaiser window in a preallocated array. The Kaiser
     * window maximizes the ratio of the energy of the main lobe by the energy
     * of the side lobes, with a given minimum side lobes attenuation.
     * @param window the window to fill
     * @param size the size of the window
     * @param beta proportional to main lobe width
     */
    public static void fillKaiserWindow(float[] window, int size, double beta) {
        double ibeta = 1.0 / iZero(beta);
        double p = (double) size / 2.0;
        for (int i = 0; i < size; i++) {
            double x = ((double) i - p) / p;
            double val = iZero(beta * Math.sqrt(1.0 - x * x)) * ibeta;
            window[i] = (float) val;
        }
    }

    public static void fillKaiserWindow(float[] window, double beta) {
        fillKaiserWindow(window, window.length, beta);
    }

    public static void fillGaussianWindow(float[] window, int size, float sigma) {
        if (sigma <= 0.0f || sigma > 0.5f)
            throw new IllegalArgumentException("'sigma' must be between 0 and 0.5");
        float half = (float) (size - 1) / 2.0f;
        for (int i = 0; i < size; i++) {
            double x = ((double) i - half) / (sigma * half);
            window[i] = (float) Math.exp(-0.5 * x * x);
        }
    }

    public static void fillGaussianWindow(float[] window, float sigma) {
        fillGaussianWindow(window, window.length, sigma);
    }

    private static double acosh(double arg) {
        return Math.log(arg + Math.sqrt(arg * arg - 1)); // arg >= 1
    }

    private static double gegenbauer(int index, double alpha, double arg) {
        if (index < 0)
            throw new IllegalArgumentException();
        if (index == 0)
            return 1.0;
        else if (index == 1)
            return 2.0 * alpha * arg;

        if ((float) alpha == 0.0f)
            return chebyshev(index, arg);

        double prev2 = 0.0;
        double prev1 = 1.0;
        double result = 2.0 * alpha * arg;
        for (int i = 2; i <= index; i++) {
            prev2 = prev1;
            prev1 = result;
            double n = (double) i;
            result = (2.0 * arg * (n + alpha - 1.0) * prev1 - (n + 2.0 * alpha - 2.0) * prev2) / n;
        }
        return result;
    }

    private static double chebyshev(int index, double arg) {
        double prev2 = 0.0;
        double prev1 = 1.0;
        double result = arg;
        for (int i = 2; i <= index; i++) {
            prev2 = prev1;
            prev1 = result;
            result = 2.0 * arg * prev1 - prev2;
        }
        return result;
    }

    /**
     * Get the 'xu' parameter that can be passed to {@link #fillUltrasphericalWindow(float[], double, double, BooFFT)},
     * given an average side lobes attenuation
     * @param attenuationDb average side lobe attenuation, in dB, positive
     * @param u side lobe rolloff shape. 0 = no rolloff (horizontal), 1 = 6dB/octave, 2 = 12dB/octave, etc
     * @param size window size
     */
    public static double getXu(double attenuationDb, double u, int size) {
        if (size % 2 == 0)
            size++;
        int middle = (size - 1) / 2;
        double r = Math.pow(10.0, attenuationDb / 20.0);
        double x0 = Math.cosh(acosh(r) / (size - 1));

        if ((float) u == 0.0f)
            return x0; // Chebyshev's beta

        double y = Math.cos(Math.PI / (4.0 * (double) middle));
        double py = y;
        int iter = 0;
        do {
            py = y;
            y = y - gegenbauer(2 * middle, u, y) / (2.0 * u * gegenbauer(2 * middle - 1, u + 1.0, y));
            if ((float) py == (float) y)
                iter++;
        } while (iter < 5);

        return x0 * y / Math.cos(Math.PI / (4.0 * (double) middle));
    }

    /**
     * Fill an ultraspherical window
     * @param xu attenuation. See {@link #getXu(double, double, int)}
     * @param u side lobe rolloff shape. 0 = no rolloff (horizontal), 1 = 6dB/octave, 2 = 12dB/octave, etc
     * @param fft a {@link BooFFT} whose size is half the window size. If <tt>null</tt>, a new temporary instance will be created
     */
    public static void fillUltrasphericalWindow(float[] window, double xu, double u, BooFFT fft) {
        if (u < -1.5 || u > 20.0)
            throw new IllegalArgumentException("u must be >= -1.5 and <= 20.0");
        int length = window.length;
        if (fft == null)
            fft = new BooFFT(length / 2);
        Cmplx[] coeffs = new Cmplx[length / 2 + 1];
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = gegenbauer(length, u, xu * Math.cos(Math.PI * (double) i / (double) length));
            coeffs[i] = new Cmplx((float) coeff, 0.0f);
        }
        float[] uswt = new float[window.length];
        fft.backC2R(coeffs, uswt);

        float w0 = uswt[0];
        for (int i = 0; i < length; i++)
            window[i] = uswt[(i + length / 2) % length] / w0;
    }

    /**
     * Fill a Chebyshev window
     * @param attenuationDb average side-lobe attenuation, in dB, positive
     * @param fft a {@link BooFFT} whose size is half the window size. If <tt>null</tt>, a new temporary instance will be created
     */
    public static void fillChebyshevWindow(float[] window, double attenuationDb, BooFFT fft) {
        /*
         * While there is a specific formula for the Chebyshev window, it shows to be less stable than using
         * the ultraspherical window with u=0.
         */
        double xu = getXu(attenuationDb, 0.0, window.length);
        fillUltrasphericalWindow(window, xu, 0.0, fft);
    }

    public static void shrink(float[] window, float shrink) {
        shrink(window, shrink, true);
    }

    public static void shrink(float[] window, float shrink, boolean energyCorrection) {
        int shrunkSize = (int) ((float) window.length / shrink + 0.5f);
        // Build shrink version in [0..shrunkSize[
        for (int i = 0; i < shrunkSize; i++)
            window[i] = window[Math.min((int) (i * shrink + 0.5f), window.length - 1)];
        int start = (window.length - shrunkSize) / 2;
        int stop = start + shrunkSize;
        // Move to center
        System.arraycopy(window, 0, window, start, shrunkSize);
        // Apply energy correction
        if (shrink != 1.0f && energyCorrection) {
            float correction = (float) Math.sqrt(shrink);
            for (int i = start; i < stop; i++)
                window[i] *= correction;
        }
        // Zero sides
        Arrays.fill(window, 0, start, 0.0f);
        Arrays.fill(window, stop, window.length, 0.0f);
    }

    /**
     * Create a correction window, given pre and post window and overlaping
     * info. Note that the number of overlaping samples must be an integral
     * divisor of the window size.
     * @param preWindow the pre-analysis window
     * @param postWindow the post-analysis window
     * @param inverse the correction window, filled by this call
     * @param blockSize the size of the pre and post windows
     * @param hopSize the number of overlapping samples, and also the size of the
     * inverse window.
     * @return Whether the correction window could be created. <tt>false</tt>
     * if an overflow (result > 20) occured.
     */
    public static boolean fillInverseWindow(float[] preWindow, float[] postWindow, float[] inverse, int blockSize, int hopSize) {
        boolean success = true;
        int nbSteps = blockSize / hopSize;
        for (int i = 0; i < hopSize; i++) {
            float sum = 0.0f;
            int j = i;
            for (int s = 0; s < nbSteps; s++) {
                sum += preWindow[j] * postWindow[j];
                j = j + hopSize;
            }
            if (sum > 0.05f) {
                inverse[i] = 1.0f / sum;
            } else {
                inverse[i] = 0.0f; // Do not put Infinite, it's just slowing down the shit
                success = false;
            }
        }
        return success;
    }

    public static boolean fillInverseWindowNoise(float[] preWindow, float[] postWindow, float[] inverse, int blockSize, int hopSize) {
        boolean success = true;
        int nbSteps = blockSize / hopSize;
        for (int i = 0; i < hopSize; i++) {
            float sum = 0.0f;
            int j = i;
            for (int s = 0; s < nbSteps; s++) {
                sum += preWindow[j] * preWindow[j] * postWindow[j] * postWindow[j];
                j = j + hopSize;
            }
            if (sum > 0.05f) {
                inverse[i] = 1.0f / (float) Math.sqrt(sum);
            } else {
                inverse[i] = 0.0f; // Do not put Infinite, it's just slowing down the shit
                success = false;
            }
        }
        return success;
    }

    public static float getStdEnergy(int length) {
        float result = 0.25f * (float) length;
        return (float) Math.sqrt(result);
    }

    public static float getPostEnergy(float[] preWindow, float[] postWindow) {
        assert preWindow.length == postWindow.length;
        float result = 0.0f;
        for (int i = 0; i < preWindow.length; i++) {
            float prod = preWindow[i] * postWindow[i];
            result += prod * prod;
        }
        return (float) Math.sqrt(result);
    }

    public static boolean fillInverseWindow(float[][] preWindows, int preIndex,
            float[] energies, float[] postWindow, float[] inverse, int windowSize,
            int overlapShift) {
        int nbSteps = windowSize / overlapShift;
        float std = getStdEnergy(postWindow.length);
        boolean success = true;
        for (int i = 0; i < overlapShift; i++) {
            float sum = 0.0f;
            int j = i;
            int winNum = preIndex; // Start with current window
            for (int s = 0; s < nbSteps; s++) {
                sum += preWindows[winNum][j] * (std / energies[winNum]) * postWindow[j];
                j = j + overlapShift;
                winNum = winNum - 1;
                if (winNum < 0)
                    winNum += preWindows.length;
            }
            if (sum > 0.05f) {
                inverse[i] = 1.0f / sum;
            } else {
                inverse[i] = 0.0f;
                success = false;
            }
        }
        return success;
    }

    /**
     * Get the constant-valued inverse window when Hann analysis and/or synthesis
     * window is used
     * @param overlapping the overlapping factor
     * @param count the number of Hann window used (1=analysis only or synthesis only; 2=analysis and synthesis)
     * @return the constant-valued inverse window
     * @throws IllegalArgumentException if <code>overlapping &lt;= count</code>, in which case
     * the windows do not sum to a constant.
     */
    public static float getHannCorrection(int overlapping, int count) {
        if (overlapping <= count)
            throw new IllegalArgumentException("Constant value only if overlapping > count");
        return 4.0f / (float) overlapping * (float) count / (float) (count + 1);
    }

    /**
     * Convolve a real spectrum in place with the symmetric mask (c1, c0, c1)
     * centered on c0. This can be used to do post-FFT windowing with Hanning
     * and Hamming windows in O(n) time. It may even be faster than
     * point-by-point multiplication in the time domain which either need to
     * read a secondary array or to compute sines and cosines on the fly.
     *
     * @param f the spectrum to convolve (dc and nyquist should not be combined
     * in one complex number)
     * @param c0 the first (center) term of the mask
     * @param c1 the second (side) term of the mask
     */
    public static void convolve2(Cmplx[] f, float c0, float c1) {
        int size = f.length - 1;
        float pre = f[0].re; // previous value's real part
        float pim = f[0].im; // previous value's imaginary part
        // dc
        f[0].re = f[0].re * c0 + f[1].re * c1 * 2.0f;
        // freqs
        for (int i = 1; i < size; i++) {
            float re = f[i].re * c0 + (pre + f[i + 1].re) * c1;
            float im = f[i].im * c0 + (pim + f[i + 1].im) * c1;
            pre = f[i].re;
            pim = f[i].im;
            f[i].re = re;
            f[i].im = im;
        }
        // Nyquist
        f[size].re = f[size].re * c0 + pre * c1 * 2.0f;
        // f[size].im = f[size].im * c0;
    }

    /**
     * Same as
     * <pre>
     * convolve2(f, HannCoefs[0], HannCoefs[1]);
     * </pre>
     * but slightly faster as the coefficients are hard-coded.
     */
    public static void convolveHann(Cmplx[] f) {
        int size = f.length - 1;
        float pre = f[0].re; // previous value's real part
        float pim = f[0].im; // previous value's imaginary part
        // dc
        f[0].re = f[0].re * 0.5f - f[1].re * 0.5f;
        // freqs
        for (int i = 1; i < size; i++) {
            float re = f[i].re * 0.5f - (pre + f[i + 1].re) * 0.25f;
            float im = f[i].im * 0.5f - (pim + f[i + 1].im) * 0.25f;
            pre = f[i].re;
            pim = f[i].im;
            f[i].re = re;
            f[i].im = im;
        }
        // Nyquist
        f[size].re = f[size].re * 0.5f - pre * 0.5f;
    }

    public static void main(String[] args) {
        final int Size = 32;
        float[] window = new float[Size];
        BooFFT fft = new BooFFT(Size / 2);
        for (int db = 20; db <= 200; db += 10) {
            System.out.println();
            System.out.println("*** sidelobe level (db): -" + db);
            Windows.fillChebyshevWindow(window, (float) db, fft);
            for (int i = 0; i < Size; i += Size / 32) {
                int c = (int) (window[i] * 64.0f + 0.5f);
                for (int j = 0; j < c; j++)
                    System.out.print("H");
                System.out.println();
            }
        }
    }

}