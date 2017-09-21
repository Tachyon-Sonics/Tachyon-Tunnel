package ch.tachyon.tunnel.common;

/**
 * Indicate whether input samples or output samples are considered.
 */
public enum IoDirection {
    /**
     * Input samples (before processing)
     */
    INPUT,
    /**
     * Output samples (after processing)
     */
    OUTPUT;
}
