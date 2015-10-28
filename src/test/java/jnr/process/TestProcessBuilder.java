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
    
    @Test
    public void testEnvironmentVariables() throws Exception {
    	String value = "environment variable";
    	
    	ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo \"$envVar\"");
    	pb.environment().put("envVar", value);
    	
    	Process p = pb.start();
    	
    	byte[] message = new byte[value.getBytes().length];
    	p.getInputStream().read(message);
    	
    	assertEquals(0, p.waitFor());
    	assertArrayEquals(value.getBytes(), message);
    }
}
