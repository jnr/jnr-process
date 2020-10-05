package jnr.process;

import static org.junit.Assert.*;
import org.junit.Test;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

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

    @Test
    public void testSelectableIn() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo hello");

        Process p = pb.start();

        SelectableChannel in = p.getIn();

        in.configureBlocking(false);
        Selector selector = in.provider().openSelector();
        SelectionKey key = in.register(selector, SelectionKey.OP_READ);
        int selected = selector.select();

        assertEquals(1, selected);
        assertEquals(key, selector.keys().iterator().next());

        selector.close();
        in.configureBlocking(true);

        byte[] hello = new byte[5];
        p.getInputStream().read(hello);

        assertEquals(0, p.waitFor());

        assertArrayEquals("hello".getBytes(), hello);
    }

    @Test
    public void testSelectableErr() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "echo hello >&2");

        Process p = pb.start();

        SelectableChannel err = p.getErr();

        err.configureBlocking(false);
        Selector selector = err.provider().openSelector();
        SelectionKey key = err.register(selector, SelectionKey.OP_READ);
        int selected = selector.select();

        assertEquals(1, selected);
        assertEquals(key, selector.keys().iterator().next());

        selector.close();
        err.configureBlocking(true);

        byte[] hello = new byte[5];
        p.getErrorStream().read(hello);

        assertEquals(0, p.waitFor());

        assertArrayEquals("hello".getBytes(), hello);
    }

    @Test
    public void testSelectableOut() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "cat");

        Process p = pb.start();

        SelectableChannel out = p.getOut();

        out.configureBlocking(false);
        Selector selector = out.provider().openSelector();
        SelectionKey key = out.register(selector, SelectionKey.OP_WRITE);
        int selected = selector.select();

        assertEquals(1, selected);
        assertEquals(key, selector.keys().iterator().next());

        selector.close();
        out.configureBlocking(true);

        p.getOutputStream().write("hello".getBytes());

        byte[] hello = new byte[5];
        p.getInputStream().read(hello);

        p.kill();

        assertEquals(9, p.waitFor());

        assertArrayEquals("hello".getBytes(), hello);
    }
}
