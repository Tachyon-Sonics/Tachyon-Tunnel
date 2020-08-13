package ch.tachyon.tunnel.test;

import org.junit.*;

public class WaitForPooledThreadsTest {
    
    @Test
    public void waitForPooledThreads() {
        ThreadPooledSuite.waitAllThreads();
    }

}
