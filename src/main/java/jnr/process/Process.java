package jnr.process;

import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.posix.POSIX;
import jnr.constants.platform.Signal;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by headius on 1/19/15.
 */
public class Process {
    private final long pid;
    private final POSIX posix;
    private final NativeDeviceChannel out; // stdin of child
    private final NativeDeviceChannel in; // stdout  of child
    private final NativeDeviceChannel err; // stderr of child
    long exitValue = -1;

    public Process(POSIX posix, long pid, int out, int in, int err) {
        this.posix = posix;
        this.pid = pid;
        this.out = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), out, SelectionKey.OP_WRITE);
        this.in = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), in, SelectionKey.OP_READ);
        this.err = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), err, SelectionKey.OP_READ);
    }

    public long getPid() {
        return pid;
    }

    public SelectableChannel getOut() {
        return out;
    }

    public OutputStream getOutputStream() {
        return Channels.newOutputStream(out);
    }

    public SelectableChannel getIn() {
        return in;
    }

    public InputStream getInputStream() {
        return Channels.newInputStream(in);
    }

    public SelectableChannel getErr() {
        return err;
    }

    public InputStream getErrorStream() {
        return Channels.newInputStream(err);
    }

    public long waitFor() {
        // TODO: This needs to handle multiple callers. Error? Block? waitpid can only be called once!
        // TODO: This needs macro handling to parse out various exit result values
        if (exitValue != -1) {
            return exitValue;
        }

        int[] status = new int[1];
        int ret = posix.waitpid(pid, status, 0);

        exitValue = status[0];

        return exitValue;
    }

    public int kill() {
        return kill(Signal.SIGKILL);
    }

    public int kill(Signal sig) {
        return posix.kill(pid, sig.intValue());
    }

    public int killProcessGroup() {
        return killProcessGroup(Signal.SIGKILL);
    }

    public int killProcessGroup(Signal sig) {
        return posix.kill(-pid, sig.intValue());
    }

    public long exitValue() {
        if (exitValue == -1) {
            throw new IllegalThreadStateException("subprocess has not yet completed");
        }
        return exitValue;
    }
}
