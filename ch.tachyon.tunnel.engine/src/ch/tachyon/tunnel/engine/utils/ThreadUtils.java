package ch.tachyon.tunnel.engine.utils;

import ch.tachyon.tunnel.utils.*;

/**
 * {@link InterruptedException}-free helper methods
 */
public class ThreadUtils {

    /**
     * Similar to {@link Thread#sleep(long)}, but any {@link InterruptedException} is catched
     * and passed to {@link Debug#warn(Throwable)}.
     */
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

    /**
     * Similar to {@link Object#wait()}, but any {@link InterruptedException} is catched
     * and passed to {@link Debug#warn(Throwable)}.
     */
    public static void wait(Object obj) {
        try {
            obj.wait();
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

    public static void wait(Object obj, long timeout) {
        try {
            obj.wait(timeout);
        } catch (InterruptedException ex) {
            Debug.warn(ex);
        }
    }

    /**
     * Similar to {@link Thread#join()}, but any {@link InterruptedException} is catched
     * and passed to {@link Debug#warn(Throwable)}.
     */
    public static void join(Thread thread, long millis) {
        try {
            thread.join(millis);
        } catch (InterruptedException ex) {
            Debug.error(ex);
        }
    }

}
