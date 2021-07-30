package ch.tachyon.tunnel.plugin.opt.doc;

import java.lang.annotation.*;

/**
 * Gives a hint on what category the plugin should be classified.
 * <p>
 * Both a category and optional subcategories can be specified, using the ":" separator.
 * <p>
 * While any string can be used, it is recommended to:
 * <ul>
 * <li>Stick to the standard categories (for the top-level category) if possible
 * <li>Specify the main category only with {@link #value()}; for optionnal other categories, use {@link #alternatives()}
 * <li>Use the generic effect name as a subcategory if the effect is known to have several implementations or flavors
 * (for example: "Timbre:Vocoder", "Time:Pitch Shifting"). Note that hosts may ignore any subcategories, or only
 * consider them if many effects fall in the same top-level category.
 * </ul>
 * A host may choose to group several categories into one, such as "Amplitude and Dynamics".
 * <p>
 * Standard categories:
 * <ul>
 * <li><b>Amplitude</b> (amplitude processing: fading, normalizing, auto-gain, slow AM modulation, etc)
 * <li><b>Dynamics</b> (dynamics processing: compressor, expander, clipping, etc)
 * <li><b>Time</b> (delay, reverse, flanger, chorus, etc)
 * <li><b>Reverb</b> (echo, reverb, convolution, etc)
 * <li><b>Channels</b> (stereo/multi-channel to mono, mone to stereo/multi-channel, HRTF, etc)
 * <li><b>Mixing</b> (mix channels, combine channels, cross-synthesis, etc)
 * <li><b>Filters</b> (frequency filters: lowpass, highpass, bandpass, equalizers, allpass, etc. Phase based effects can be put here or in the "Time" category)
 * <li><b>Tempo</b> (time stretching, resample, etc; plugins doing both time stretching and pitch shifting go here)
 * <li><b>Pitch</b> (pitch shifting, frequency shifting, fast AM modulation, FM modulation, harmonizing, formant shifting, etc)
 * <li><b>Distortion</b> (distort, clip, tube distortion, analog distortion, etc)
 * <li><b>Timbre</b> (robotize, whisperize, vocoder, voice changer, etc)
 * <li><b>Spectral</b> (spectrum / FFT based effect that do not fall in other categories. Spectral sweep, spectral compander, etc)
 * <li><b>Simulation</b> (simulations: vinyl effect, tape effect. Also for multi-category effects creating an "ambiance")
 * <li><b>Separation</b> (separation or extraction of signal parts: sinus + noise, center channel extraction, transient extraction)
 * <li><b>Restoration</b> (noise reduction, de-clicker, de-clipper, gap interpolation, phase correction, etc)
 * <li><b>Voice</b> (any voice-specific effect can use this category instead of the other ones if it is optimized for voice and does not work well with music: voice/gender changer, formant shifting, etc)
 * <li><b>Scientific</b> (spectral inverter, scientific filters, etc, also for effects targetted to non-musical signals)
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Category {

    /**
     * The main category for this plugin
     */
    public String value();

    /**
     * Optional alternative categories for this plugin
     */
    public String[] alternatives() default {};

}
