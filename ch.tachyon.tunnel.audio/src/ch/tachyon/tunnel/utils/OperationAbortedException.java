package ch.tachyon.tunnel.utils;

/**
 * Indicate that an operation has been aborted (typically through a GUI "abort" button).
 * Thrown by {@link IProgressMonitor#setProgress(double, double)}.
 */
public class OperationAbortedException extends RuntimeException {

    private final boolean cancel;


    public OperationAbortedException(boolean cancel) {
        this.cancel = cancel;
    }

    /**
     * Whether the operation should be cancelled. When <code>true</code>, anything that has been
     * partially done should be undone. When <code>false</code>, anything that has been partially
     * done should be kept.
     */
    public boolean isCancel() {
        return cancel;
    }


}
