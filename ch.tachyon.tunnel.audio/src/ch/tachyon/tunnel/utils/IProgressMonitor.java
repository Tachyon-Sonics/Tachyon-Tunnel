package ch.tachyon.tunnel.utils;

/**
 * Interface for reporting progress of a long operation. Also used by the GUI to stop or
 * cancel the operation.
 * <p>
 * Most of the time you only need to invoke the {@link #setProgress(double, double)} method.
 */
public interface IProgressMonitor {

    /**
     * Set the current progress
     * @param current a number between 0 and <code>total</code> corresponding to the current progress,
     * or {@link Double#NaN} to display an undeterminate progress (in that case <code>total</code> is
     * ignored).
     * @throws OperationAbortedException if the operation should be aborted (for instance from a GUI).
     */
    public void setProgress(double current, double total) throws OperationAbortedException;


}
