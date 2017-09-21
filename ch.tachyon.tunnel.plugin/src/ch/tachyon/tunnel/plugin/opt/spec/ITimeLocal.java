package ch.tachyon.tunnel.plugin.opt.spec;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.opt.thread.*;
import ch.tachyon.tunnel.plugin.param.*;


/**
 * Indicate that a processed chunk of audio not only depends on the corresponding input audio chunk,
 * but also on a given number of sample frames or the previous audio chunk(s).
 * <p>
 * A typical example is an echo plugin: produced samples are a mix of the current samples and echoes
 * of a number of previous samples.
 * <p>
 * This interface is meant as a declarative way of allowing parallelization on multi-core hardware, and
 * is ignored on single-core hardware. This is only an optimization-related interface. It is <i>not</i>
 * a way for the plugin to request samples before or after the audio to process. The {@link IRequiredBeforeAfter}
 * interface can be used for that purpose.
 * <p>
 * This interface can be used together with {@link MultiThreading} to allow parallel processing of a
 * plugin that is not entirely time-local. The engine will extends every audio chunk to process with
 * additional sample frames before it, as given by {@link #getRequiredFramesBefore(IProcessingInfo)}.
 * These added samples are then dropped from the resulting audio chunk to construct the final output.
 * <p>
 * There is no way to specify that samples <i>after</i> the current input audio chunk are required.
 * However, in general such a plugin do not need these and reports a non-zero latency instead,
 * using {@link ILatency}.
 * <p>
 * The engine may choose not to parallelize the computation if the returned values are too large. Plugins
 * are recommended to always return the actual values, even if they are large, and to let the engine decide
 * if parallelization is worth.
 * <p>
 * In case of an IIR filter, {@link #getRequiredFramesBefore(IProcessingInfo)} can return the number of sample
 * frames it takes in the worst case for the impulse response to fall below the quantization floor as defined
 * by {@link IProcessingInfo#getQuantization()}. The returned value can depend on the actual value of the
 * plugin's non {@link Sweepable} parameters, but must use the worst-case for any {@link Sweepable} parameter.
 * <p>
 * Using this interface will waste some CPU time processing the added sample frames. On the other hand, it
 * scales extremely well on architectures with dozens of CPU cores.
 * As an alternative, the {@link IHasSerialSections} interface can be used to manually serialize the copy of
 * previous audio chunk sample frames for the next one.
 */
public interface ITimeLocal {

    public int getRequiredFramesBefore(IProcessingInfo info);

}
