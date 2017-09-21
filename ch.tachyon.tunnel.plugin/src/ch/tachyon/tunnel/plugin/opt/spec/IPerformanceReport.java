package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.plugin.opt.callback.*;
import ch.tachyon.tunnel.plugin.opt.doc.*;


/**
 * Indicate the performance of the implemented processing, in terms of speed, quality and latency.
 * <p>
 * Performance is based on the type of processing implemented by the plugin (such as reverb, compression,
 * resampling, etc), and on performance-related implementation choices (such as time-frequency resolution,
 * oversampling amount, length of filter, etc).
 * <p>
 * The purpose of this interface is, for example, to notify the user that an effect X (for example noise
 * reduction) is likely slower than an effect Y (say distortion); or that an effect X implemented in the
 * time domain is faster but of lower quality and higher latency than the same effect X implemented in
 * the frequency domain. It does not say anything about the performance of one implementation with
 * respect to another implementation of the same effect using the same overal algorithm and parameters.
 * <p>
 * The chosen values should be based as much as possible on the objective metrics and examples
 * specified in the javadoc of the individual methods (see {@link #getSpeedPerf()}, {@link #getQualityPerf()}
 * and {@link #getLatencyPerf()}).
 * <p>
 * The values may change when the value of a parameter is changed. In fact the host may invoke any of the
 * methods multiple times (for instance whenever a parameter is modified). However, the methods are not invoked
 * before the plugin is loaded (before {@link ILoadUnload#load(ch.tachyon.tunnel.plugin.IProcessingInfo)})
 * or while processing (after {@link IStartStop#startProcessing(ch.tachyon.tunnel.plugin.IProcessingInfo)}).
 * <p>
 * In case the performance differs depending on characteristics of the audio, or if it is frequency dependant
 * (for example the latency), the value should be based on a reasonable average.
 * <p>
 * If a plugin targets only specifc signals (such as voice), the returned values can be based on results on
 * these types of signals only (this is mostly relevant for {@link #getQualityPerf()}). In that case the
 * targetted type(s) of signals should be mentionned in the documentation (see {@link Documentation}) and
 * preferrably even in the description (see {@link Description}}.
 */
public interface IPerformanceReport {

    /**
     * Reports the performance of the implemented processing in terms of speed. Best values means faster.
     * <ul>
     * <li><b>{@link PerformanceType#ZERO}</b> is not a legal value and should not be used
     * <li><b>{@link PerformanceType#VERY_GOOD}</b> A few operations per samples. Amplification, distortions
     * (without oversampling), etc
     * <li><b>{@link PerformanceType#GOOD}</b> Small Fourier transforms with size * overlapping factor &lt; 1024, sinc-based resampling and basic frequency domain processing
     * <li><b>{@link PerformanceType#MEDIUM}</b> Fourier transforms with size * overlapping factor between 1024 and 32768 and moderate frequency domain processing
     * <li><b>{@link PerformanceType#BAD}</b> Larger Fourier transforms or heavy frequency domain processing. Simple multi-resolution or adaptive tiling
     * <li><b>{@link PerformanceType#VERY_BAD}</b> Complex multi-resolution or adaptive tiling
     * <li><b>{@link PerformanceType#NOT_SPECIFIED}</b> if for any reason the speed is not known (for instance
     * when bridging existing plugins based on a different plugin interface) or has not been measured.
     * </ul>
     */
    public PerformanceType getSpeedPerf();

    /**
     * Indicate the quality of the implemented processing. The returned value should solely depend on the
     * implemented effect and processing parameters, but not on the implementation details.
     * <ul>
     * <li><b>{@link PerformanceType#ZERO}</b> Trivial transformation with no artifact even after an
     * <i>infinite</i> number of applications. This is limited to fully reversible (with bit-exact result)
     * operations such as waveform inversion, reversing the signal, replacing the signal with silence,
     * and clipboard operations (cut, copy, paste, etc). Amplitude changes and any more complex effects
     * are classified lower.
     * <li><b>{@link PerformanceType#VERY_GOOD}</b> Windowed sinc resampling, amplitude changes, distortion
     * with &gt;= 8x oversampling (windowed-sinc based), delays; reverberation, frequency filters or
     * equalizers based on convolution. Effects working in the frequency domain (except when used
     * solely to implement overlap-add convolution), and effects that affect the phase in a non-linear way
     * cannot use this value.
     * <li><b>{@link PerformanceType#GOOD}</b> distortions without oversampling, IIR-based frequency filters
     * and equalizers, simple frequency domain effects that are hardly subject to the time-frequency tradoff and
     * that are linear-phase, such as frequency filters and equalizers. Frequency domain effects affecting the
     * phase on purpose can also use this value. A Fourier based frequency filter using very narrow bands
     * (such as a comb filter) might use the <code>MEDIUM</code> value. Frequency domain effects that are not
     * only based on linear operations or that require an analysis of the signal cannot use this value and must
     * be classified lower.
     * <li><b>{@link PerformanceType#MEDIUM}</b> Linear resampling, most non-trivial frequency domain effects
     * that are linear phase but subject to the time-frequency trad-off such as robotization, whisperization,
     * vocoding, etc. Frequency domain effects affecting the phase in a non-linear way or that have inherent
     * artifacts should not use this value but BAD.
     * <li><b>{@link PerformanceType#BAD}</b> Sample and hold resampling, frequency domain effects that are
     * non-linear phase, such as time stretching, pitch shifting, harmonizing, etc. Frequency domain effects
     * with known inherent artifacts such as noise reduction.
     * <br />
     * Borderline between <code>MEDIUM</code> and <code>BAD</code> are frequency domain effects that affect
     * the phase in non-linear way, but still <i>fully</i> preserve the vertical <i>and</i> horizontal phase
     * coherence (an example is frequency shifting).
     * <li><b>{@link PerformanceType#VERY_BAD}</b> Experimental effects, such as vocal removal (a center channel
     * extractor can be classified as BAD or MEDIUM if presented as such and not as a vocal remover), instrument
     * extraction or any processing based on the isolation of instruments or high-level sounds, etc.
     * <li><b>{@link PerformanceType#NOT_APPLICABLE}</b> Can only be used for any effects for which the
     * notion of quality does not make sense. This is usually only limited to
     * <ul>
     * <li>"Artistic" effects that highly distort the sound on purpose, hardly preserving any property of the
     * original sound.
     * <li>"Scientific" transformations that have no musical or artistic purpose (such as
     * spectral invertion), or that target non-audio signals.
     * </ul>
     * <li><b>{@link PerformanceType#NOT_SPECIFIED}</b> if for any reason the quality is not known (for instance
     * when bridging existing plugins based on a different plugin interface)
     * </ul>
     */
    public PerformanceType getQualityPerf();

    /**
     * Specifies how low the latency is. <b>Warning:</b> "good" means <i>low</i> latency and
     * "bad" means <i>high</i> latency. Obviously this does not take into account the latency compensation
     * that is performed by the host, and is hence mostly relevant when the effect is used in a "live"
     * situation where latency compensation is not possible.
     * <ul>
     * <li><b>{@link PerformanceType#ZERO}</b> Strictly zero latency even after an inifinte number
     * of applications. Note that IIR filters have a very small latency and should be classified
     * <code>VERY_GOOD</code> instead.
     * <li><b>{@link PerformanceType#VERY_GOOD}</b> Avergage latency less than 1ms of latency. This
     * includes most IIR filters.
     * <li><b>{@link PerformanceType#GOOD}</b> Less than 12ms (suitable for live performance if used alone)
     * <li><b>{@link PerformanceType#MEDIUM}</b> Less than 60ms (suitable for gesture to sound)
     * <li><b>{@link PerformanceType#BAD}</b> Less than 500ms (bad but still not too annoying when changing
     * sweepable parameters). Suitable for a communication channel.
     * <li><b>{@link PerformanceType#VERY_BAD}</b> More than 500ms
     * <li><b>{@link PerformanceType#NOT_APPLICABLE}</b> should not be used, except for an effect whose sole
     * purpose is to deliberately report a selectable amount of latency. If the latency is unknown,
     * {@link PerformanceType#NOT_SPECIFIED} should be used instead.
     * </ul>
     */
    public PerformanceType getLatencyPerf();

}
