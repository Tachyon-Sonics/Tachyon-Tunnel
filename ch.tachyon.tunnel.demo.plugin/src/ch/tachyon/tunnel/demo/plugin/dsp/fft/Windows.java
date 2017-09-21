package ch.tachyon.tunnel.demo.plugin.dsp.fft;


public class Windows {

    public final static float[] HannCoefs = { 0.5f, -0.25f };
    public final static float[] HammingCoefs = { 0.53836f, -0.23082f };
    public final static float[] BlackmanCoefs = { 0.42f, -0.25f, 0.04f };
    public final static float[] NutallCoefs = { 0.36335819f, -0.24458875f, 0.06829975f, -0.00532055f };
    public final static float[] FlatTopCoefs = { 0.25f, -0.24125f, 0.16125f, -0.04225f, 0.004f };


    public static void fillWindow(float[] window, float[] coefs) {
        int size = window.length;
        for (int i = 0; i < size; i++) {
            double x = Math.PI * 2.0 * (double) i / (double) size;
            double val = coefs[0];
            for (int c = 1; c < coefs.length; c++)
                val += (coefs[c] * 2.0f * Math.cos(x * (double) c));
            window[i] = (float) val;
        }
    }

    public static void fillHannWindow(float[] window) {
        fillWindow(window, HannCoefs);
    }

    public static void fillHammingWindow(float[] window, int size) {
        fillWindow(window, HammingCoefs);
    }

    public static void fillBlackmanWindow(float[] window, int size) {
        fillWindow(window, BlackmanCoefs);
    }

    public static void fillNutallWindow(float[] window, int size) {
        fillWindow(window, NutallCoefs);
    }

    public static void fillFlatTopWindow(float[] window, int size) {
        fillWindow(window, FlatTopCoefs);
    }

    public static void fillTriangularWindow(float[] window) {
        int half = window.length / 2;
        for (int i = 0; i < half; i++) {
            float value = (float) i / (float) half;
            window[i] = value;
            window[i + half] = 1.0f - value;
        }
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
     * Get the beta value to pass to {@link #fillKaiserWindow(float[], double)} in order to have the given average side lobes attenuation
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
     * Create the parametrable Kaiser window in a preallocated array. The Kaiser window maximizes the ratio of the energy of the main lobe by the energy of the side lobes, with a given minimum side lobes attenuation.
     * @param window the window to fill
     * @param beta proportional to main lobe width
     */
    public static void fillKaiserWindow(float[] window, double beta) {
        int size = window.length;
        double ibeta = 1.0 / iZero(beta);
        double p = (double) size / 2.0;
        for (int i = 0; i < size; i++) {
            double x = ((double) i - p) / p;
            double val = iZero(beta * Math.sqrt(1.0 - x * x)) * ibeta;
            window[i] = (float) val;
        }
    }

    public static void fillGaussianWindow(float[] window, float sigma) {
        if (sigma <= 0.0f || sigma > 0.5f)
            throw new IllegalArgumentException("'sigma' must be between 0 and 0.5");
        int size = window.length;
        float half = (float) (size - 1) / 2.0f;
        for (int i = 0; i < size; i++) {
            double x = ((double) i - half) / (sigma * half);
            window[i] = (float) Math.exp(-0.5 * x * x);
        }
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
     * Get the 'xu' parameter that can be passed to {@link #fillUltrasphericalWindow(float[], double, double, BooFFT)}, given an average side lobes attenuation
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
        BooFFT rFft = new BooFFT(window.length / 2);
        float[] uswt = new float[window.length];
        rFft.backC2R(coeffs, uswt);

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
         * While there is a specific formula for the Chebyshev window, it shows to be less stable than using the ultraspherical window with u=0.
         */
        double xu = getXu(attenuationDb, 0.0, window.length);
        fillUltrasphericalWindow(window, xu, 0.0, fft);
    }

    /**
     * Create a correction window, given pre and post window and overlaping info. Note that the number of overlaping samples must be an integral divisor of the window size.
     * @param preWindow the pre-analysis window
     * @param postWindow the post-analysis window
     * @param inverse the correction window, filled by this call
     * @param windowSize the size of the pre and post windows
     * @param shift the number of overlapping samples, and also the size of the inverse window.
     * @return Whether the correction window could be created. <tt>false</tt> if an overflow (result > 20) occured.
     */
    public static boolean fillInverseWindow(float[] preWindow, float[] postWindow, float[] inverse, int windowSize, int shift) {
        boolean success = true;
        int nbSteps = windowSize / shift;
        for (int i = 0; i < shift; i++) {
            float sum = 0.0f;
            int j = i;
            for (int s = 0; s < nbSteps; s++) {
                sum += preWindow[j] * postWindow[j];
                j = j + shift;
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

    /**
     * Get the constant-valued inverse window when Hann analysis and/or synthesis window is used
     * @param overlapping the overlapping factor
     * @param count the number of Hann window used (1=analysis only or synthesis only; 2=analysis and synthesis)
     * @return the constant-valued inverse window
     * @throws IllegalArgumentException if <code>overlapping &lt;= count</code>, in which case the windows do not sum to a constant.
     */
    public static float getHannCorrection(int overlapping, int count) {
        if (overlapping <= count)
            throw new IllegalArgumentException("Constant value only if overlapping > count");
        return 4.0f / (float) overlapping * (float) count / (float) (count + 1);
    }

    /**
     * Convolve a real spectrum in place with the symmetric mask (c1, c0, c1) centered on c0. This can be used to do post-FFT windowing with Hanning and Hamming windows in O(n) time. It may even be faster than point-by-point multiplication
     * in the time domain which either need to read a secondary array or to compute sines and cosines on the fly.
     *
     * @param f the spectrum to convolve (dc and nyquist should not be combined in one complex number)
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
     * 
     * <pre>
     * convolve2(f, HannCoefs[0], HannCoefs[1]);
     * </pre>
     * 
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

    /**
     * Convolve a real spectrum in place with the symmetric mask (c2, c1, c0, c1, c2) centered on c0. This can be used to do post-FFT windowing with Blackmann-Haris windows in O(n) time.
     * @param f the spectrum to convolve (dc and nyquist should not be combined in one complex number)
     * @param c0 the first (center) term of the mask
     * @param c1 the second term of the mask
     * @param c2 the third term of the mask
     */
    public static void convolve3(Cmplx[] f, float c0, float c1, float c2) {
        float pre2 = f[0].re;
        float pim2 = f[0].im;
        float pre1 = f[1].re;
        float pim1 = f[1].im;
        // dc and f1
        f[0].re = f[0].re * c0 + f[1].re * c1 * 2.0f + f[2].re * c2 * 2.0f;
        // f[0].im = f[0].im * c0;
        f[1].re = f[1].re * c0 + (pre2 + f[2].re) * c1 + (f[1].re + f[3].re) * c2;
        f[1].im = f[1].im * c0 + (pim2 + f[2].im) * c1 + (-f[1].im + f[3].im) * c2;
        // freqs
        for (int i = 2; i < f.length - 2; i++) {
            float re = f[i].re * c0 + (pre1 + f[i + 1].re) * c1 + (pre2 + f[i + 2].re) * c2;
            float im = f[i].im * c0 + (pim1 + f[i + 1].im) * c1 + (pim2 + f[i + 2].im) * c2;
            pre2 = pre1;
            pim2 = pim1;
            pre1 = f[i].re;
            pim1 = f[i].im;
            f[i].re = re;
            f[i].im = im;
        }
        // Nyquist -1 & -0
        int nq1 = f.length - 2;
        int nq0 = f.length - 1;
        float nq1re = f[nq1].re;
        f[nq1].re = f[nq1].re * c0 + (pre1 + f[nq0].re) * c1 + (pre2 + f[nq1].re) * c2;
        f[nq1].im = f[nq1].im * c0 + (pim1 + f[nq0].im) * c1 + (pim2 - f[nq1].im) * c2;
        f[nq0].re = f[nq0].re * c0 + nq1re * c1 * 2.0f + pre1 * c2 * 2.0f;
        // f[nq0].im = f[nq0].im * c0;
    }

    /**
     * Convolve a real spectrum in place with a symmetric mask centered on the term of index 0. The mask must be smaller than half of the size of f. If the length of the coefficient array is 2 or 3, this method calls convolve2 or convolve3.
     * @param f the spectrum to convolve (dc and nyquist should not be combined in one complex number)
     * @param c the mask to use
     */
    public static void convolveX(Cmplx[] f, float[] c) {
        if (c.length == 2) {
            convolve2(f, c[0], c[1]);
            return;
        } else if (c.length == 3) {
            convolve3(f, c[0], c[1], c[2]);
            return;
        }
        Cmplx[] b = Cmplx.newArray(c.length);
        for (int i = 0; i < c.length; i++)
            b[i].copyFrom(f[i]);
        for (int i = 0; i < c.length; i++) {
            float re = f[i].re * c[0];
            float im = f[i].im * c[0];
            for (int j = 1; j < c.length; j++) {
                int k = (j > i ? j - i : i - j);
                float s = (j > i ? -1.0f : 1.0f);
                re += (b[k].re + f[i + j].re) * c[j];
                im += (b[k].im * s + f[i + j].im) * c[j];
            }
            f[i].set(re, im);
        }
        int bi = c.length - 1;
        for (int i = c.length; i < f.length - c.length; i++) {
            bi = (bi + 1) % c.length;
            float re = f[i].re * c[0];
            float im = f[i].im * c[0];
            for (int j = 1; j < c.length; j++) {
                int k = (bi + c.length - j) % c.length;
                re += (b[k].re + f[i + j].re) * c[j];
                im += (b[k].im + f[i + j].im) * c[j];
            }
            b[bi].copyFrom(f[i]);
            f[i].set(re, im);
        }
        for (int i = f.length - c.length; i < f.length; i++) {
            bi = (bi + 1) % c.length;
            float re = f[i].re * c[0];
            float im = f[i].im * c[0];
            for (int j = 1; j < c.length; j++) {
                int k = (bi + c.length - j) % c.length;
                int r = i + j;
                float s = 1.0f;
                if (r >= f.length) {
                    r = (f.length - 1) * 2 - r;
                    s = -1.0f; // negate imaginary part
                }
                re += (b[k].re + f[r].re) * c[j];
                im += (b[k].im + f[r].im * s) * c[j];
            }
            b[bi].copyFrom(f[i]);
            f[i].set(re, im);
        }
        b = null;
    }

    public static void main(String[] args) {
        final int Size = 32;
        float[] window = new float[Size];
        BooFFT fft = new BooFFT(Size);
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