package jnr.process;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Created by headius on 1/19/15.
 */
public class TestProcessBuilder {
    @Test
    public void testBasicProcess() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo hello");

        Process p = pb.start();

        byte[] hello = new byte[5];
        p.getInputStream().read(hello);

        assertEquals(0, p.waitFor());
        
        assertArrayEquals("hello".getBytes(), hello);
    }
}
