package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Extended {@link IResampler} that supports changing the resampling ratio
 * on the fly.
 * @author Nicolas
 *
 */
public interface ISweepableResampler extends IResampler {
    
    /**
     * Change the resampling ratio.
     * <p>
     * This method must be invoked between two calls to {@link #resample(float[])}.
     * It is not safe to invoke it from another thread.
     */
    public void setRatio(double ratio);

}
