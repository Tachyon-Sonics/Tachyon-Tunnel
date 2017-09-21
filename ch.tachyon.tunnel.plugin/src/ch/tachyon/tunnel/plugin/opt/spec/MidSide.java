package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

/**
 * Indicate that, when processing stereo signal, it is preferrable to process
 * middle channel and side channel rather than left channel and right channel.
 * <p>
 * This can be used as a simplified way of handling potential phase differences
 * when the two channels are processed independently and the effect does not preserve the phase.
 * <p>
 * This annotation is meaningless if the plugin already implement a multi-channel processing
 * interface.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MidSide {

}
