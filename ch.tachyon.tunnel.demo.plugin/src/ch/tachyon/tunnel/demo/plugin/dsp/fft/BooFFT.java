package ch.tachyon.tunnel.demo.plugin.dsp.fft;


/**
 * Fast Fourier Transform for complex and real data.
 * <p>
 * An instance of this class can perform complex transforms of the given size
 * (in complex numbers) and real transforms of twice the given size. Only sizes
 * that are powers of 2 are supported.
 * <p>
 * The transforms are all out-of-place (they require both a source and a
 * destination array). Nevertheless, they do never modify the source array.
 * <p>
 * The first time an instance is requested for a given size, roots of the unity
 * and scrambling arrays are pre-computed.
 * <p>
 * For real transforms, the real arrays must be twice the size of this FFT. The
 * resulting spectrum must be of {@link #size() size} + 1.
 * <p>
 * This code is based on <a href="http://cr.yp.to/djbfft.html">djbfft</a>.
 * <p>
 * <b>Implementation notes:</b><br>
 * This implementation is based on a single fast kernel: an in-place, scrambled,
 * unscaled backward complex Fourier transform. All other variants are
 * implemented using this kernel function plus various "correction" functions.
 * Forward Fourier transform for instance is expressed by swapping real and
 * imaginary parts and using the backward Fourier transform.
 */
public class BooFFT {

    private final int length;
    private final int log;
    private final static float sqrthalf = (float) 0.70710678118654752440084436210485;

    // Roots of the unity for small sizes
    private Cmplx[] d16;
    private Cmplx[] d32;
    private Cmplx[] d64;
    private Cmplx[] d128;
    private Cmplx[] d256;
    private Cmplx[] d512;
    // Roots of the unity for all sizes. Sizes <= 512 refer to the previous arrays
    protected Cmplx[][] roots;

    // Work arrays. Both arrays refer to the same objects (reference aliasing), but in different order
    private Cmplx[] work;
    private Cmplx[] workScrambled;



    /**
     * Create an FFT instance. The returned instance can only perform
     * complex FFTs of the given length, and real FFTs of twice the
     * given length.
     * <p>
     * Creating a new instance is expensive. Instances should be reused for
     * multiple FFTs as much as possible.
     * @param length the length, in complex numbers
     * @throws IllegalArgumentException if the requested length is not a power of 2.
     */
    public BooFFT(int length) {
        if ((length & (length - 1)) != 0)
            throw new IllegalArgumentException("lenght must be a power of 2");
        this.length = length;
        this.log = log2(length);

        // Init roots
        UnityRoots generator = UnityRoots.getInstance();
        roots = new Cmplx[(log < 10 ? 10 : log) + 2][];
        for (int i = roots.length - 1; i >= 10; i--) {
            int div = (i == log + 1 ? 4 : 8);
            int size = (1 << i);
            roots[i] = generator.getRoots(size, size / div);
        }
        d512 = fillRoots(generator, 512, 9);
        d256 = fillRoots(generator, 256, 8);
        d128 = fillRoots(generator, 128, 7);
        d64 = fillRoots(generator, 64, 6);
        d32 = fillRoots(generator, 32, 5);
        d16 = fillRoots(generator, 16, 4);
        fillRoots(generator, 8, 3);
        fillRoots(generator, 4, 2);

        // Create working array and scrambled version of working array
        initPrivateData();
    }

    private void initPrivateData() {
        work = Cmplx.newArray(this.length);
        workScrambled = new Cmplx[this.length];
        for (int i = 0; i < length; i++)
            /*
             * Mind the following line! Both arrays are arrays of
             * *references* to complex numbers, and we are copying
             * *references*. Hence, both arrays will refer to the
             * *same* list of complex numbers. Array 'work' refer to
             * them in normal order and 'workScrambled' refer to them
             * in scrambled order. This is *on purpose* aliasing to
             * implement easy and fast scrambling/unscrambling.
             */
            workScrambled[replace(i, length)] = work[i];
    }

    /**
     * Get the ordered index corresponding to the given scrambled index.
     * @param i the scrambled index
     * @param n the FFT size
     * @return the ordered index
     */
    private int replace(int i, int n) {
        if (n <= 2)
            return i;
        int m = n / 2;
        if (i < m)
            return replace(i, m) * 2;
        i -= m;
        m /= 2;
        if (i < m)
            return replace(i, m) * 4 + 1;
        i -= m;
        return (replace(i, m) * 4 - 1) & (n - 1);
    }

    private Cmplx[] fillRoots(UnityRoots generator, int size, int log) {
        Cmplx[] result = generator.getRoots(size, size / 4);
        this.roots[log] = result;
        return result;
    }

    /**
     * The length (in complex numbers) of the vectors this FFT engine can
     * transform.
     * @return The length of the vectors this FFT engine can transform
     */
    public int size() {
        return this.length;
    }

    // Butterflies & Passes

    protected final void butterfly(Cmplx a0, Cmplx a1, Cmplx a2, Cmplx a3, float wre, float wim) {
        float t5 = a2.re * wre + a2.im * wim;
        float t6 = a2.im * wre - a2.re * wim;
        float t7 = a3.re * wre - a3.im * wim;
        float t8 = a3.im * wre + a3.re * wim;
        float t1 = t5 + t7;
        float t2 = t6 + t8;
        float t3 = t6 - t8;
        float t4 = t7 - t5;
        a2.re = a0.re - t1;
        a2.im = a0.im - t2;
        a3.re = a1.re - t3;
        a3.im = a1.im - t4;
        a0.re += t1;
        a0.im += t2;
        a1.re += t3;
        a1.im += t4;
    }

    protected final void butterflyHalf(Cmplx a0, Cmplx a1, Cmplx a2, Cmplx a3) {
        float t5 = (a2.re + a2.im) * sqrthalf;
        float t6 = (a2.im - a2.re) * sqrthalf;
        float t7 = (a3.re - a3.im) * sqrthalf;
        float t8 = (a3.im + a3.re) * sqrthalf;
        float t1 = t5 + t7;
        float t2 = t6 + t8;
        float t3 = t6 - t8;
        float t4 = t7 - t5;
        a2.re = a0.re - t1;
        a2.im = a0.im - t2;
        a3.re = a1.re - t3;
        a3.im = a1.im - t4;
        a0.re += t1;
        a0.im += t2;
        a1.re += t3;
        a1.im += t4;
    }

    protected final void butterflyFirst(Cmplx a0, Cmplx a1, Cmplx a2, Cmplx a3) {
        float t1 = a2.re + a3.re;
        float t2 = a2.im + a3.im;
        float t3 = a2.im - a3.im;
        float t4 = a3.re - a2.re;
        a2.re = a0.re - t1;
        a2.im = a0.im - t2;
        a3.re = a1.re - t3;
        a3.im = a1.im - t4;
        a0.re += t1;
        a0.im += t2;
        a1.re += t3;
        a1.im += t4;
    }

    private final void pass(Cmplx[] a, int a0, Cmplx[] w, int n2) {
        final int a1 = n2 + a0;
        final int a2 = a1 + n2;
        final int a3 = a2 + n2;
        butterflyFirst(a[a0], a[a1], a[a2], a[a3]);
        int k = n2 - 1;
        int b = 1;
        do {
            butterfly(a[b + a0], a[b + a1], a[b + a2], a[b + a3], w[b].re, w[b].im);
            b++;
            k--;
        } while (k > 0);
    }

    protected final void pass2(Cmplx[] a, int a0, Cmplx[] w, int n) {
        int a1 = 2 * n + a0;
        int a2 = 4 * n + a0;
        int a3 = 6 * n + a0;
        butterflyFirst(a[a0], a[a1], a[a2], a[a3]);
        int wb = 1;
        do {
            a0++;
            a1++;
            a2++;
            a3++;
            butterfly(a[a0], a[a1], a[a2], a[a3], w[wb].re, w[wb].im);
            wb++;
        } while (wb < n);
        a0++;
        a1++;
        a2++;
        a3++;
        butterflyHalf(a[a0], a[a1], a[a2], a[a3]);
        wb--;
        do {
            a0++;
            a1++;
            a2++;
            a3++;
            butterfly(a[a0], a[a1], a[a2], a[a3], w[wb].im, w[wb].re);
            wb--;
        } while (wb > 0);
    }

    // Hard-coded small FFT sizes

    private final void fft2(Cmplx[] a, int off) {
        Cmplx a0 = a[off];
        Cmplx a1 = a[off + 1];
        float t1 = a0.re + a1.re;
        float t2 = a0.im + a1.im;
        float t3 = a0.re - a1.re;
        float t4 = a0.im - a1.im;
        a0.re = t1;
        a0.im = t2;
        a1.re = t3;
        a1.im = t4;
    }

    private final void fft4(Cmplx[] a, int off) {
        Cmplx a0 = a[off];
        Cmplx a1 = a[off + 1];
        Cmplx a2 = a[off + 2];
        Cmplx a3 = a[off + 3];
        float t1 = a0.re + a1.re;
        float t2 = a0.re - a1.re;
        float t3 = a0.im + a1.im;
        float t4 = a0.im - a1.im;
        float t5 = a2.re + a3.re;
        float t6 = a2.re - a3.re;
        float t7 = a2.im + a3.im;
        float t8 = a2.im - a3.im;
        a0.re = t1 + t5;
        a0.im = t3 + t7;
        a1.re = t2 + t8;
        a1.im = t4 - t6;
        a2.re = t1 - t5;
        a2.im = t3 - t7;
        a3.re = t2 - t8;
        a3.im = t4 + t6;
    }

    private final void fft8(Cmplx[] a, int off) {
        fft4(a, off);
        fft2(a, off + 4);
        fft2(a, off + 6);
        butterflyFirst(a[off + 0], a[off + 2], a[off + 4], a[off + 6]);
        butterflyHalf(a[off + 1], a[off + 3], a[off + 5], a[off + 7]);
    }

    private final void fft16(Cmplx[] a, int off) {
        fft8(a, off);
        fft4(a, off + 8);
        fft4(a, off + 12);
        pass(a, off, d16, 4);
    }

    private final void fft32(Cmplx[] a, int off) {
        fft16(a, off);
        fft8(a, off + 16);
        fft8(a, off + 24);
        pass(a, off, d32, 8);
    }

    private final void fft64(Cmplx[] a, int off) {
        fft32(a, off);
        fft16(a, off + 32);
        fft16(a, off + 48);
        pass(a, off, d64, 16);
    }

    private final void fft128(Cmplx[] a, int off) {
        fft64(a, off);
        fft32(a, off + 64);
        fft32(a, off + 96);
        pass(a, off, d128, 32);
    }

    private final void fft256(Cmplx[] a, int off) {
        fft128(a, off);
        fft64(a, off + 128);
        fft64(a, off + 192);
        pass(a, off, d256, 64);
    }

    private final void fft512(Cmplx[] a, int off) {
        fft256(a, off);
        fft128(a, off + 256);
        fft128(a, off + 384);
        pass(a, off, d512, 128);
    }

    // Generic routines for higher sizes

    protected void fftN(Cmplx[] a, int off, int log) {
        switch (log) {
        case 1:
            fft2(a, off);
            break;
        case 2:
            fft4(a, off);
            break;
        case 3:
            fft8(a, off);
            break;
        case 4:
            fft16(a, off);
            break;
        case 5:
            fft32(a, off);
            break;
        case 6:
            fft64(a, off);
            break;
        case 7:
            fft128(a, off);
            break;
        case 8:
            fft256(a, off);
            break;
        case 9:
            fft512(a, off);
            break;
        default:
            if (log < 1)
                return;
            fftBig(a, off, log);
        }
    }

    protected void fftBig(final Cmplx[] a, final int off, final int log) {
        int size = (1 << log);
        final int size4 = size / 4;
        fftN(a, off, log - 1);
        fftN(a, off + size4 + size4, log - 2);
        fftN(a, off + size4 + size4 + size4, log - 2);
        pass2(a, off, roots[log], size / 8);
    }

    /**
     * Compute the base-2 logarithm of a number that is a
     * power of 2. If the number is not a power of 2, the
     * integer part of the logarithm is returned. The result
     * is undefined for negative values and zero.
     * @param value the number whose logarithm to compute
     * @return lg2(value)
     */
    public static int log2(int value) {
        int log = 0;
        if (value >= (1 << 16)) {
            log += 16;
            value >>>= 16;
        }
        if (value >= (1 << 8)) {
            log += 8;
            value >>>= 8;
        }
        if (value >= (1 << 4)) {
            log += 4;
            value >>>= 4;
        }
        if (value >= (1 << 2)) {
            log += 2;
            value >>>= 2;
        }
        if (value >= 2)
            log++;
        return log;
    }

    // Friendly methods

    /**
     * Complex forward Fourier transform.
     * @param src the array to transform. This array is not modified by this method.
     * @param dst the array in which to put the result.
     * @throws IllegalArgumentException if the sizes of the arrays do not
     * match the size of this FFT engine.
     */
    public void forwC2C(Cmplx[] src, Cmplx[] dst) {
        if (src.length != this.length || dst.length != this.length)
            throw new IllegalArgumentException("The size of the arrays must match the size of this FFT");
        fromCmplx(src, workScrambled);
        fft(work);
        fix(work, dst, (float)length);
    }

    /**
     * Complex backward Fourier transform.
     * @param src the array to transform. This array is not modified by this method.
     * @param dst the array in which to put the result.
     * @throws IllegalArgumentException if the sizes of the arrays do not
     * match the size of this FFT engine.
     */
    public void backC2C(Cmplx[] src, Cmplx[] dst) {
        if (src.length != this.length || dst.length != this.length)
            throw new IllegalArgumentException("The size of the arrays must match the size of this FFT");
        unfix(src, workScrambled);
        fft(work);
        toCmplx(work, dst);
    }

    /**
     * Real forward Fourier transform.
     * <p>
     * Note that <tt>f.length == (r.length / 2) + 1</tt> must hold. DC and Nyquist
     * components are <i>not</i> merged together; they both have a zero imaginary
     * part.
     * @param r the array to tranform. This array is not modified by this method.
     * @param f the array in which to put the result.
     * @throws IllegalArgumentException if the sizes of the arrays do not
     * match the size of this FFT engine.
     */
    public void forwR2C(float[] r, Cmplx[] f) {
        if (r.length != this.length * 2 || f.length != this.length + 1)
            throw new IllegalArgumentException("The size of the arrays must match the size of this FFT");
        fromReal(r, workScrambled);
        fft(work);
        realFix(work, f);
    }

    /**
     * Real backward Fourier transform.
     * <p>
     * Note that <tt>f.length == (r.length / 2) + 1</tt> must hold. DC and Nyquist
     * components are <i>not</i> merged together; they both have a zero imaginary
     * part.
     * @param f the array to transform. This array is not modified by this method.
     * @param r the array in which to put the result.
     * @throws IllegalArgumentException if the sizes of the arrays do not
     * match the size of this FFT engine.
     */
    public void backC2R(Cmplx[] f, float[] r) {
        if (r.length != this.length * 2 || f.length != this.length + 1)
            throw new IllegalArgumentException("The size of the arrays must match the size of this FFT");
        realUnfix(f, workScrambled);
        fft(work);
        toReal(work, r);
    }

    // Advanced and help methods

    /**
     * Backward Fourier transform. Unscaled, scrambles the result.
     */
    private void fft(Cmplx[] a) {
        fftN(a, 0, log);
    }

    private void fromCmplx(Cmplx[] src, Cmplx[] aScrambled) {
        assert src.length == aScrambled.length;
        for (int i = 0; i < aScrambled.length; i++) {
            /*
             * Because fft() does a backward transform, we have to
             * pre-scramble the data, and swap real and imaginary
             * parts in order to express a forward transform.
             */
            aScrambled[i].im = src[i].re;
            aScrambled[i].re = src[i].im;
        }
    }

    private void toCmplx(Cmplx[] a, Cmplx[] dst) {
        assert a.length == dst.length;
        for (int i = 0; i < a.length; i++) {
            dst[i].re = a[i].re;
            dst[i].im = a[i].im;
        }
    }

    private void fix(Cmplx[] a, Cmplx[] r, float scaleFactor) {
        float scale = 1.0f / scaleFactor;
        for (int i = 0; i < length; i++) {
            /* Swap back real and imaginary parts */
            r[i].im = a[i].re * scale;
            r[i].re = a[i].im * scale;
        }
    }

    private void unfix(Cmplx[] r, Cmplx[] aScrambled) {
        /* Pre-scramble the array before a backward transform */
        for (int i = 0; i < length; i++)
            aScrambled[i].set(r[i]);
    }

    private void fromReal(float[] r, Cmplx[] aScrambled) {
        assert aScrambled.length * 2 == r.length;
        int j = 0;
        for (int i = 0; i < length; i++) {
            aScrambled[i].im = r[j++];
            aScrambled[i].re = r[j++];
        }
    }

    private void toReal(Cmplx[] a, float[] r) {
        assert a.length * 2 == r.length;
        int j = 0;
        for (int i = 0; i < length; i++) {
            r[j++] = a[i].re;
            r[j++] = a[i].im;
        }
    }

    private void realFix(Cmplx[] a, Cmplx[] r) {
        realFix(a, r, this.length);
    }

    private void realFix(Cmplx[] a, Cmplx[] r, float scale) {
        /*
         * - Do the complex-to-real conversion
         * - Scale the result
         * - Swap real and imaginary parts (we had to swap them to express the
         *   forward FFT using fft(), that actually does a backward FFT)
         */
        final int dc = 0;
        final int nq = length / 2;
        float scale1 = 1.0f / scale;
        float scale2 = scale1 / 2.0f;
        Cmplx[] w = roots[log + 1];
        r[dc].re = (a[dc].im + a[dc].re) * scale1;
        r[dc].im = 0.0f;
        r[length].re = (a[dc].im - a[dc].re) * scale1;
        r[length].im = 0.0f;
        for (int i = 1, j = length - 1; i < nq; i++, j--) {
            Cmplx ai = a[i];
            Cmplx aj = a[j];
            Cmplx wi = w[i];
            float t1 = ai.im + aj.im;
            float t2 = ai.re - aj.re;
            float t3 = aj.re + ai.re;
            float t4 = aj.im - ai.im;
            float t5 = t3 * wi.re - t4 * wi.im;
            float t6 = t3 * wi.im + t4 * wi.re;
            r[i].re = (t1 + t5) * scale2;
            r[i].im = (t6 + t2) * scale2;
            r[j].re = (t1 - t5) * scale2;
            r[j].im = (t6 - t2) * scale2;
        }
        r[nq].re = a[nq].im * scale1;
        r[nq].im = a[nq].re * scale1;
    }

    private void realUnfix(Cmplx[] r, Cmplx[] aScrambled) {
        /*
         * - Do the real-to-complex conversion
         * - Pre-scramble the indexes
         */
        final int dc = 0;
        final int nq = length / 2;
        Cmplx[] w = roots[log + 1];
        aScrambled[dc].re = (r[dc].re + r[length].re) / 2.0f;
        aScrambled[dc].im = (r[dc].re - r[length].re) / 2.0f;
        for (int i = 1, j = length - 1; i < nq; i++, j--) {
            Cmplx ri = r[i];
            Cmplx rj = r[j];
            Cmplx wi = w[i];
            float t1 = ri.re + rj.re;
            float t2 = ri.im - rj.im;
            float t3 = ri.re - rj.re;
            float t4 = ri.im + rj.im;
            float t5 = t4 * wi.im + t3 * wi.re;
            float t6 = t4 * wi.re - t3 * wi.im;
            aScrambled[i].re = (t1 - t6) / 2.0f;
            aScrambled[i].im = (t5 + t2) / 2.0f;
            aScrambled[j].re = (t1 + t6) / 2.0f;
            aScrambled[j].im = (t5 - t2) / 2.0f;
        }
        aScrambled[nq].re = r[nq].re;
        aScrambled[nq].im = r[nq].im;
    }

}
