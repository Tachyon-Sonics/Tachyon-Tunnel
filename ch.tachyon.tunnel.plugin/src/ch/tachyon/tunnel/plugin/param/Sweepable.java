package ch.tachyon.tunnel.plugin.param;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.*;

/**
 * Indicate that the given parameter is sweepable. It basically means that the host is allowed
 * to change the value of the parameter while processing audio data. Note that "while processing" means
 * between the processing of two consecutive blocks of audio data (for example between two calls
 * to {@link IMultiChanEffect#process(float[][])}).
 * <p>
 * When a parameter is not sweepable, the host can only change its value before the processing starts.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sweepable {

}
