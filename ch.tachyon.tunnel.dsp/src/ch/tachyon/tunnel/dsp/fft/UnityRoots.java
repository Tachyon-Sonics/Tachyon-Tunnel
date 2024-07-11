package ch.tachyon.tunnel.dsp.fft;

import ch.tachyon.tunnel.dsp.utils.SimpleCacheMap;


/**
 * Singleton class for getting complex roots of the unity.
 */
public class UnityRoots {

    private static UnityRoots generator;
    private SimpleCacheMap<Integer, Cmplx[]> nToRoots = new SimpleCacheMap<>();

    /**
     * This class is a singleton. Use static method getGenerator to get this
     * class' unique instance.
     */
    private UnityRoots() {
    }

    /**
     * Return this class' singleton object.
     *
     * @return the singleton of this class. An object able to generate roots of
     * unity.
     */
    public static synchronized UnityRoots getInstance() {
        if (generator == null)
            generator = new UnityRoots();
        return generator;
    }

    /*
     * Currently the following sharing mechanism is used: If an array of enough
     * roots already exists for the given divisor, it is returned and therefore
     * shared. In all other cases, a new array with new references is build
     * again from scratch. Reference sharing of individual Cmplx instances is not used
     * as it results in scattered memory accesses.
     */

    /**
     * Get at least the 'count' first 'div'-th roots of the Unity. The resulting
     * array as well as elements of the array may or may not be shared accross
     * multiple calls to this method. The method may return more roots than
     * requested.
     * @param div the root divisor. Return the 'div'-th roots of unity
     * @param count the minimal number of roots to return
     * @return the first 'count' (or more) 'div'-th root of 1 as an array of
     * {@link Cmplx} objects
     */
    public synchronized Cmplx[] getRoots(int div, int count) {
        Cmplx[] roots = nToRoots.getItem(div);
        if (roots == null) {
            roots = Cmplx.newArray(count);
            double cosInc = Math.cos(Math.PI * 2.0 / div);
            double sinInc = Math.sin(Math.PI * 2.0 / div);
            roots[0].set(1.0f, 0.0f);
            double lre = 1.0;
            double lim = 0.0;
            for (int i = 1; i < count; i++) {
                /*
                 * cos(A+B) = cos(A)cos(B) - sin(A)sin(B)
                 * sin(A+B) = sin(A)cos(B) + cos(A)sin(B)
                 * with B = first root and A = previous root
                 */
                double re = cosInc * lre - sinInc * lim;
                double im = sinInc * lre + cosInc * lim;
                lre = re;
                lim = im;
                roots[i].set((float) re, (float) im);
            }
            nToRoots.putItem(div, roots);
        } else if (roots.length < count) {
            Cmplx[] nroots = Cmplx.newArray(count);
            /* Copy available roots */
            System.arraycopy(roots, 0, nroots, 0, roots.length);
            double cosInc = Math.cos(Math.PI * 2.0 / div);
            double sinInc = Math.sin(Math.PI * 2.0 / div);
            /* Complete starting from last available root */
            double lre = Math.cos(Math.PI * 2.0 * (roots.length - 1) / div);
            double lim = Math.sin(Math.PI * 2.0 * (roots.length - 1) / div);
            for (int i = roots.length; i < count; i++) {
                double re = cosInc * lre - sinInc * lim;
                double im = sinInc * lre + cosInc * lim;
                lre = re;
                lim = im;
                nroots[i].set((float) re, (float) im);
            }
            nToRoots.putItem(div, nroots);
            roots = nroots;
        }
        return roots;
    }

}
