package ch.tachyon.tunnel.demo.plugin.resample.impl;

/**
 * Interface for {@link IAudioSource} that have an internal state that can be
 * reset
 */
public interface IResetable {
    
    public void reset();

}
