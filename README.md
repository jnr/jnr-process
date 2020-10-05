jnr-process
===========

The jnr-process library provides a drop-in replacement for the JDK
ProcessBuilder API, but instead of a thread-pumped shim it is a direct
abstraction around the `posix_spawn` C API and provides selectable in, out, and
err channels.

Usage
-----

Basic usage with selectable input (out from child) is shown below:

```java
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
```

jnr-process provides `getIn`, `getOut`, and `getErr` to get a SelectableChannel
for the child's output, input, and error streams respectively.

The `killProcessGroup` methods are provided to kill an entire subtree of child
processes. All `kill` forms also accept an optional integer signal to send.