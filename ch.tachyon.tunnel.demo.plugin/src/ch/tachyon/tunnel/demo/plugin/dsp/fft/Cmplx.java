package ch.tachyon.tunnel.demo.plugin.dsp.fft;

import ch.tachyon.tunnel.demo.plugin.dsp.FastMath;

/**
 * Mutable complex number using the float data type.
 * <p>
 * The instance methods of this class corresponding to complex operations are storing the result in the calling
 * instance (<code>this</code>), to avoid the overhead of creating new objects.
 * <p>
 * The idea of this class is that you create as few instances as
 * possible and that you reuse existing instances.
 * <p>
 * <b><font color="red">WARNING - WARNING</font></b><br>
 *  A <tt>Cmplx</tt> variable is a <i>reference</i> to
 * a <tt>Cmplx</tt> instance (as with any Java object). It means that if <code>c1</code> and <code>c2</code>
 * are two <code>Cmplx</code> instances, and you do
 * <pre>c1 = c2;</pre>
 * you are <b>most likely doing something wrong</b> as it will not copy the value of
 * <code>c2</code> into <code>c1</code>, but the <i>reference</i> <code>c2</code> into
 * <code>c1</code>. This means that now <code>c2</code> and <code>c1</code> refer to the
 * same instance, and changes to one will affect the other.
 * <p>
 * To copy the value, use
 * <pre>c1.set(c2);</pre>
 * <p>
 * <b>Warning</b>: Several advanced trigonometric functions have not been tested yet.
 */
public final class Cmplx implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 2377021765855242381L;

    /**
     * The real part of this complex number
     */
    public float re;

    /**
     * The imaginary part of this complex number
     */
    public float im;

    /**
     * The <code>i</code>, or <code>(0,1)</code> complex number.
     */
    public final static Cmplx I = new Cmplx(0.0f, 1.0f);

    private final static Cmplx minusI = new Cmplx(0.0f, -1.0f);

    /**
     * Create a new complex number with a value of zero.
     */
    public Cmplx() {

    }

    /**
     * Create a new complex number with the given value.
     * @param re real part
     * @param im imaginary part
     */
    public Cmplx(float re, float im) {
        this.re = re;
        this.im = im;
    }

    /**
     * Create a new complex number with the given value
     * @param other the value for the new complex number
     */
    public Cmplx(Cmplx other) {
        this.re = other.re;
        this.im = other.im;
    }

    /**
     * Get a clone of this complex number.
     * @return a clone of this Complex number
     */
    @Override
    public Cmplx clone() {
        try {
            return (Cmplx) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    /**
     * Compare two complex numbers for equality.
     * @param o an other object
     * @return if o is equal to this
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || !(o instanceof Cmplx))
            return false;
        Cmplx other = (Cmplx) o;
        return (this.re == other.re) && (this.im == other.im);
    }

    /**
     * Get the hashcode of this complex number.
     * @return a hashcode for this complex number
     */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(this.re) ^ Float.floatToIntBits(this.im);
    }

    /**
     * Get a string representation of this complex number.
     * @return a string representation of this complex number. The notation
     * (re,im) is used.
     */
    @Override
    public String toString() {
        return "(" + this.re + "," + this.im + ")";
    }

    /**
     * Sets the value of this complex number
     * @param re the real part
     * @param im the imaginary part
     */
    public void set(float re, float im) {
        this.re = re;
        this.im = im;
    }

    /**
     * Clear this complex number. Sets it to zero.
     * Equivalent to {@link #set(float, float) set(0.0f, 0.0f)}.
     */
    public void clear() {
        set(0.0f, 0.0f);
    }

    /**
     * Test whether this complex number is equal to zero.
     * @return whether this complex number is equal to zero
     */
    public boolean isZero() {
        return this.re == 0.0f && this.im == 0.0f;
    }

    /**
     * Sets the value of this complex number.
     * Same as {@link #copyFrom(Cmplx)}.
     * @param value the value to set this complex number to
     */
    public void set(Cmplx value) {
    	this.re = value.re;
    	this.im = value.im;
    }

    /**
     * Copy the real and imaginary parts of another complex number to this
     * complex number.
     * @param other the complex number to copy from.
     */
    public void copyFrom(Cmplx other) {
        this.re = other.re;
        this.im = other.im;
    }

    /**
     * Swap the real part and the imaginary part of this
     * complex number.
     */
    public void swapReIm() {
        float tmp = this.re;
        this.re = this.im;
        this.im = tmp;
    }

    /**
     * Swap this complex number with another one. After this
     * call, this complex number has the original value of the
     * given complex number and vice versa.
     * @param other the complex number to swap this complex number with
     */
    public void swapWith(Cmplx other) {
        float tmp = this.re;
        this.re = other.re;
        other.re = tmp;
        tmp = this.im;
        this.im = other.im;
        other.im = tmp;
    }

    /**
     * Create an array of complex number references, and allocate
     * each entry with a complex number instance with a value of zero.
     * @param size the size of the array to create
     * @return the array
     */
    public static Cmplx[] newArray(int size) {
        Cmplx[] result = new Cmplx[size];
        for (int i = 0; i < size; i++)
            result[i] = new Cmplx();
        return result;
    }

    public static Cmplx[][] newArray(int size1, int size2) {
        Cmplx[][] result = new Cmplx[size1][size2];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                result[i][j] = new Cmplx();
            }
        }
        return result;
    }

    public static Cmplx[][][] newArray(int size1, int size2, int size3) {
        Cmplx[][][] result = new Cmplx[size1][size2][size3];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                for (int k = 0; k < size3; k++)
                    result[i][j][k] = new Cmplx();
            }
        }
        return result;
    }

    /**
     * Add a complex number to this complex number and put the result in this
     * complex number.<BR>
     * Performs: this = this + val.
     * @param val the complex number to add
     */
    public void add(Cmplx val) {
        re += val.re;
        im += val.im;
    }

    public void add(float re, float im) {
        this.re += re;
        this.im += im;
    }

    /**
     * Add two complex numbers and put the result in this complex number.<BR>
     * Performs: this = val1 + val2.
     * @param val1 first complex number
     * @param val2 second complex number
     */
    public void sum(Cmplx val1, Cmplx val2) {
        re = val1.re + val2.re;
        im = val1.im + val2.im;
    }

    /**
     * Subtract a complex number to this complex number and put the result in
     * this complex number.<BR>
     * Performs: this = this - val.
     * @param val the complex number to subtract
     */
    public void sub(Cmplx val) {
        re -= val.re;
        im -= val.im;
    }

    /**
     * Subtract two complex numbers and put the result in this complex number.<BR>
     * Performs: this = val1 - val2;
     * @param val1 first complex number
     * @param val2 second complex number
     */
    public void dif(Cmplx val1, Cmplx val2) {
        re = val1.re - val2.re;
        im = val1.im - val2.im;
    }

    /**
     * Multiply a complex number to this complex number and put the result in
     * this complex number.<BR>
     * Performs: this = this * val.
     * @param val the complex number to subtract
     */
    public void mul(Cmplx val) {
        float re = this.re * val.re - this.im * val.im;
        float im = this.re * val.im + this.im * val.re;
        this.re = re;
        this.im = im;
    }

    /**
     * Multiply a complex number with a real number.<BR>
     * Performs: this = this * val.
     * @param val a real scalar
     */
    public void mul(float val) {
        this.re *= val;
        this.im *= val;
    }

    /**
     * Multiply two complex numbers and put the result in this complex number.<BR>
     * Performs: this = val1 * val2.
     * @param val1 first complex number
     * @param val2 second complex number
     */
    public void prod(Cmplx val1, Cmplx val2) {
        re = val1.re * val2.re - val1.im * val2.im;
        im = val1.re * val2.im + val1.im * val2.re;
    }

    /**
     * Inverse this complex number.<BR>
     * Performs: this = 1 / this.
     */
    public void inv() {
        float den = re * re + im * im;
        re = re / den;
        im = -im / den;
    }

    /**
     * Divide this complex number by another complex number.<BR>
     * Performs: this = this / val.
     * @param val the divider
     */
    public void div(Cmplx val) {
        float den = val.re * val.re + val.im * val.im;
        float re = this.re * val.re + this.im * val.im;
        float im = this.im * val.re - this.re * val.im;
        this.re = re / den;
        this.im = im / den;
    }

    /**
     * Divide a complex number by an other one and put the result (quotient) in
     * this complex number.<BR>
     * Performs: this = val1 / val2
     * @param val1 first complex number (dividend)
     * @param val2 second complex number (divisor)
     */
    public void quo(Cmplx val1, Cmplx val2) {
        float den = val2.re * val2.re + val2.im * val2.im;
        float re = val1.re * val2.re + val1.im * val2.im;
        float im = val1.im * val2.re - val1.re * val2.im;
        this.re = re / den;
        this.im = im / den;
    }

    /**
     * Convert this complex number into polar form. In polar form, no operation
     * is valid except {@link #toCartesian()}. {@link #re} is replaced with the
     * magnitude and {@link #im} with the phase.
     */
    public void toPolar() {
        double mag = Math.sqrt(this.re * this.re + this.im * this.im);
        double phi = Math.atan2(this.im, this.re);
        this.re = (float) mag;
        this.im = (float) phi;
    }

    /**
     * Faster approximation of {@link #toPolar()} using {@link FastMath}
     */
    public void toPolarApprox() {
        double mag = magApprox();
        double phi = phiApprox();
        this.re = (float) mag;
        this.im = (float) phi;
    }

    /**
     * Get the magnitude of this complex number.
     * @return the magnitude of this complex number
     */
    public float mag() {
//        return (float) Math.hypot(this.re, this.im);
        return (float) Math.sqrt(this.re * this.re + this.im * this.im);
    }

    /**
     * Get a fast approximation of the magnitude of this
     * complex number. The RMS between the approximation and
     * the true value is -32.6dB on avergage, -25.7dB on
     * worst case. Linear average error is zero (appart from
     * roundoff errors).
     * <p>
     * Computes: &alpha; * max(|re|, |im|) + &beta; * min(|re|, |im|)<br>
     * with &alpha; = 0.948059448969 and &beta; = 0.392699081699.
     * @return an approximation of the magnitude of this complex number
     */
    public float magApprox() {
        final double ALPHA = 0.948059448969;
        final double BETA = 0.392699081699;
        double absRe = (this.re < 0.0f ? -this.re : this.re);
        double absIm = (this.im < 0.0f ? -this.im : this.im);
        // ALPHA * max + BETA * min:
        if (absRe > absIm) {
            return (float) (ALPHA * absRe + BETA * absIm);
        } else {
            return (float) (ALPHA * absIm + BETA * absRe);
        }
    }

    /**
     * Get the square magnitude of this complex number
     * <p>
     * Computes: re * re + im * im
     * @return the square magnitude of this complex number
     */
    public float powerMag() {
        return this.re * this.re + this.im * this.im;
    }

    /**
     * Get the phase of this complex number.
     * Result is between -&pi; and &pi;.
     * @return the angular phase of this complex number (in radian)
     */
    public float phi() {
        return (float) Math.atan2(this.im, this.re);
    }

    public float phiApprox() {
        return (float) FastMath.atan2(this.im, this.re);
    }

    /**
     * Convert this complex number in cartesian form. This complex is expected
     * in polar form with {@link #re} set to magnitude and {@link #im} set to
     * phase.
     */
    public void toCartesian() {
        double re = this.re * Math.cos(this.im);
        double im = this.re * Math.sin(this.im);
        this.re = (float) re;
        this.im = (float) im;
    }

    /**
     * Convert this complex number in cartesian form. This complex is expected
     * in polar form with {@link #re} set to magnitude and {@link #im} set to
     * phase. Uses fast sin/cos approximation from {@link FastMath}.
     */
    public void toCartesianApprox() {
        double re = this.re * FastMath.fastCos(this.im);
        double im = this.re * FastMath.fastSin(this.im);
        this.re = (float) re;
        this.im = (float) im;
    }

    /**
     * Compute the natural complex exponent.<BR>
     * Performs: this = e<sup>this</sup>.
     */
    public void exp() {
        double phi = this.im;
        double mag = Math.exp(this.re);
        this.re = (float) (mag * Math.cos(phi));
        this.im = (float) (mag * Math.sin(phi));
    }

    /**
     * Compute the natural complex logarithm.<BR>
     * Performs: this = log<sub>e</sub>(this).
     */
    public void log() {
        double mag = Math.sqrt(this.re * this.re + this.im * this.im);
        double phi = Math.atan2(this.im, this.re);
        this.re = (float) Math.log(mag);
        this.im = (float) phi;
    }

    /**
     * Compute the complex square root.<BR>
     * Performs: this = &radic;<span style="text-decoration: overline">this</span>
     */
    public void sqrt() {
        double sqrtmag = Math.sqrt(Math.sqrt(re * re + im * im));
        double sqrtphi = Math.atan2(im, re) / 2.0;
        this.re = (float) (sqrtmag * Math.cos(sqrtphi));
        this.im = (float) (sqrtmag * Math.sin(sqrtphi));
    }

    /**
     * Raise this complex number to a complex exponent.<BR>
     * Perfomrms: this = this<sup>exp</sup>.
     * @param exp the exponent
     */
    public void pow(Cmplx exp) {
        // a^b = e^(b*ln(a))
        this.log();
        this.mul(exp);
        this.exp();
    }

    /**
     * Hyperbolic inverse cosine of a real number. This method is not present in
     * <code>java.lang.Math</code>
     * @param arg the argument
     * @return the hyperbolic inverse cosine of the argument
     */
    public static double acosh(double arg) {
        return Math.log(arg + Math.sqrt(arg * arg - 1)); // arg >= 1
    }

    /**
     * Hyperbolic inverse sine of a real number. This method is not present in
     * <code>java.lang.Math</code>
     * @param arg the argument
     * @return the hyperbolic inverse sine of the argument
     */
    public static double asinh(double arg) {
        return Math.log(arg + Math.sqrt(arg * arg + 1));
    }

    /**
     * Compute the complex cosine.<BR>
     * Performs: this = cos(this).
     */
    public void cos() {
        // cos(z) = cos(x)cosh(y) - isin(x)sinh(y)
        double re = Math.cos(this.re) * Math.cosh(this.im);
        double im = Math.sin(this.re) * Math.sinh(this.im);
        this.re = (float) re;
        this.im = -(float) im;
    }

    /**
     * Compute the complex sine.<BR>
     * Performs: this = sin(this).
     */
    public void sin() {
        // sin(z) = sin(x)cosh(y) + icos(x)sinh(y)
        double re = Math.sin(this.re) * Math.cosh(this.im);
        double im = Math.cos(this.re) * Math.sinh(this.im);
        this.re = (float) re;
        this.im = (float) im;
    }

    /**
     * Compute the complex tangent.<BR>
     * Performs: this = tan(this).
     */
    public void tan() {
        // / tan(x) = sin(x)/cos(x)
        // tan(x) = -i tanh ( ix )
        this.mul(I);
        this.tanh();
        this.mul(minusI);
    }

    /**
     * Compute the complex inverse cosine.<BR>
     * Performs: this = acos(this).
     */
    public void acos() {
        // / acos(z) = -i * log(z + i*sqrt(1 - z^2))
        // acos(z) = i * acosh(z)
        this.acosh();
        this.mul(I);
    }

    /**
     * Compute the complex inverse sine.<BR>
     * Performs: this = asin(this).
     */
    public void asin() {
        // asin(z) = -i * log(i*z + sqrt(1 - z^2))
        Cmplx t = new Cmplx(this);
        this.mul(t);
        this.mul(-1.0f);
        this.re += 1.0f;
        this.sqrt();
        t.mul(I);
        this.add(t);
        this.log();
        this.mul(minusI);
    }

    /**
     * Compute the complex inverse tangant.<BR>
     * Performs: this = atan(this).
     */
    public void atan() {
        // atan(z) = 0.5i * log((i+z)/(i-z))
        Cmplx t = new Cmplx(this);
        t.add(I);
        this.mul(-1.0f);
        this.add(I);
        this.inv();
        this.mul(t);
        this.log();
        this.mul(I);
        this.mul(0.5f);
    }

    /**
     * Compute the hyperbolic cosine.<br>
     * Performs: this = cosh(this).
     */
    public void cosh() {
        // cosh(z) = cosh(x)cos(y) + isinh(x)sin(y)
        double re = Math.cosh(this.re) * Math.cos(this.im);
        double im = Math.sinh(this.re) * Math.sin(this.im);
        this.re = (float) re;
        this.im = (float) im;
    }

    /**
     * Compute the hyperbolic sine.<br>
     * Performs: this = sinh(this).
     */
    public void sinh() {
        // sinh(z) = sinh(x)cos(y) + icosh(x)sin(y)
        double re = Math.sinh(this.re) * Math.cos(this.im);
        double im = Math.cosh(this.re) * Math.sin(this.im);
        this.re = (float) re;
        this.im = (float) im;
    }

    /**
     * Compute the hyperbolic tangant.<br>
     * Performs: this = tanh(this).
     */
    public void tanh() {
        // tanh(z) = sinh(z) / cosh(z)
        Cmplx tmp = new Cmplx(this);
        tmp.cosh();
        tmp.inv();
        this.sinh();
        this.mul(tmp);
    }

    /**
     * Compute the inverse hyperbolic cosine.<br>
     * Performs: this = acosh(this).
     */
    public void acosh() {
        // acosh(z) = -log(z + i*sqrt(1-z^2))
        Cmplx copy = new Cmplx(this);
        this.mul(copy);
        this.mul(-1.0f);
        this.re += 1.0f;
        this.sqrt();
        this.mul(I);
        this.add(copy);
        this.log();
        this.mul(-1.0f);
    }

    /**
     * Compute the inverse hyperbolic sine.<br>
     * Performs: this = asinh(this).<br>
     */
    public void asinh() {
        // asinh(z) = -log(sqrt(1+z^2)-z)
        Cmplx copy = new Cmplx(this);
        this.mul(copy);
        this.re += 1.0f;
        this.sqrt();
        this.sub(copy);
        this.log();
        this.mul(-1.0f);
    }

    /**
     * Compute the inverse hyperbolic tangant.<br>
     * Performs: this = atanh(this).
     */
    public void atanh() {
        // atanh(z) = 0.5*log((1+z)/(1-z))
        Cmplx copy = new Cmplx(this);
        copy.re += 1.0f;
        this.mul(-1.0f);
        this.re += 1.0f;
        this.inv();
        this.mul(copy);
        this.log();
        this.mul(0.5f);
    }

}
