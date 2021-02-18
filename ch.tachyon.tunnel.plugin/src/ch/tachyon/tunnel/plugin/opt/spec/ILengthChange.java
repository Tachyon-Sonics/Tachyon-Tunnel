package ch.tachyon.tunnel.plugin.opt.spec;

/**
 * Interface for an effect that changes the length (resampling, time stretching, etc.)
 * <p>
 * Even if not implemented, length change can be deduced by comparing input and output
 * latencies. However using this interface is necessary for zero-latency effects, or
 * if a more accurate result is desired.
 */
public interface ILengthChange {
    
    /**
     * @return the exact length change, as output length divided by input length
     */
    public double getLengthChange();

}
