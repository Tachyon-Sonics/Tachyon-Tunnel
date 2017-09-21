package ch.tachyon.tunnel.plugin.host;

import ch.tachyon.tunnel.plugin.*;

/**
 * Provides read and write accesses to audio data for plugin implementing the {@link IRandomAccessEffect}
 * interface.
 * <p>
 * Read and write accesses are implemented as if two copies of the audio are used, one for reading, and one
 * for writing. In other words, reading always reads the original, unmodified audio, even if writes have
 * been performed at the location being read.
 * <p>
 * All accesses are relative to the beginning of the audio region to process. Read access are allowed to be
 * performed outside of this region; the host may return actual samples or zeroes. Write accesses on the other
 * hand are not allowed outside of the region, but the size of the region can be modified using Inserts and
 * Deletes.
 */
public interface IRandomAccessor {

    /**
     * Get information about the audio being processed. The relevent properties of the result
     * are {@link IProcessingInfo#getLength()} (original length, does not reflect insertions and deletions
     * performed so far), {@link IProcessingInfo#getNumberOfChannels()}
     * and {@link IProcessingInfo#getSampleRate()}. Other properties are undefined and should not be used.
     * <p>
     * There is no way to get the current length of the result. If needed, this information must be
     * managed by the plugin. The length of the result determines the allowed maximum positions for
     * insertions and deletions.
     */
    public IProcessingInfo getProcessingInfo();

    /**
     * Read audio data at the specified position. The position is relative to begin of the audio region to
     * process.
     * <p>
     * It is legal for a plugin to read before or after the region to process using a position less than zero or
     * greater than {@link IProcessingInfo#getLength()}. In such a case, the host may either supply actual
     * samples if they exist, or choose to "hide" samples before and after the region and supply zeroes
     * instead. If no sample exist before or after the region, zeroes will be supplied.
     * <p>
     * This method always reads the old unprocessed samples. It never reads samples written by
     * {@link #write(long, float[][], int)}.
     * @param position the position of the first sample frame to read, relative to the begin of the audio to
     * process
     * @param target the target array to fill with the samples to read
     * @param length the number of sample frames to read
     */
    public void read(long position, float[][] target, int length);

    /**
     * Write audio data at the specified position. The position is relative to begin of the audio region to
     * process.
     * <p>
     * Writing data before or after the region to process is allowed but the host may ignore it.
     */
    public void write(long position, float[][] samples, int length);

    /**
     * Insert audio data at the specified position. The position is relative to begin of the audio region to
     * process.
     * <p>
     * Inserting data before or after the region to process is not allowed.
     */
    public void insert(long position, float[][] samples, int length);

    /**
     * Delete audio data at the specified position. The position is relative to begin of the audio region to
     * process.
     * <p>
     * Deleting data before or after the region to process is not allowed.
     */
    public void delete(long position, int length);

    /**
     * Set the current processing progress as a number between 0 and 1. A plugin implementation is not required
     * to invoke this method, but using it will allow the host to display an accurate progress indicator.
     * <p>
     * If a plugin uses this method, it is recommanded that it invokes it with <code>0</code> before the
     * first I/O operation to inform the host that is does not need to try to deduce the current progress
     * itself.
     * <p>
     * Host should expect this method to be invoked at an arbitrary frequency or not at all. If this method
     * is never invoked, the host may infer the current progress based on the number of samples read or
     * written, or may not display any progress at all.
     * @param progress the current progress, between 0 and 1.
     */
    public void setProgress(double progress);

}
