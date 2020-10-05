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
 * A {@link java.lang.Process} look-alike based on the Java Native Runtime's native FFI binding.
 *
 * Note this adds the {@link #getIn()}, {@link #getOut()}, and {@link #getErr()} methods for accessing selectable
 * channels from the child process, as well as {@link #killProcessGroup()} to kill the child and descendants.
 */
public class Process {
    private final long pid;
    private final POSIX posix;
    private final NativeDeviceChannel out; // stdin of child
    private final NativeDeviceChannel in; // stdout  of child
    private final NativeDeviceChannel err; // stderr of child
    long exitValue = -1;

    /**
     * Construct a new Process instance that wraps the given pid and native IO streams.
     *
     * @param posix the POSIX instance from jnr-posix
     * @param pid the subprocesses's pid
     * @param out the parent's output stream (subprocess's input stream)
     * @param in the parent's input stream (subprocess's output stream)
     * @param err the parent's error input stream (subprocess's error output stream)
     */
    Process(POSIX posix, long pid, int out, int in, int err) {
        this.posix = posix;
        this.pid = pid;
        this.out = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), out, SelectionKey.OP_WRITE, false);
        this.in = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), in, SelectionKey.OP_READ, false);
        this.err = new NativeDeviceChannel(NativeSelectorProvider.getInstance(), err, SelectionKey.OP_READ, false);
    }

    /**
     * Get the pid of the child process.
     *
     * @return the pid of the child process
     */
    public long getPid() {
        return pid;
    }

    /**
     * Get the selectable channel for the parent's output, which is the child's input.
     *
     * @return the parent's output channel for the child's input
     */
    public SelectableChannel getOut() {
        return out;
    }

    /**
     * Get the stream for the parent's output, which is the child's input.
     *
     * @return the parent's output stream for the child's input
     */
    public OutputStream getOutputStream() {
        return Channels.newOutputStream(out);
    }

    /**
     * Get the selectable channel for the parent's input, which is the child's output.
     *
     * @return the parent's input channel for the child's output
     */
    public SelectableChannel getIn() {
        return in;
    }

    /**
     * Get the stream for the parent's input, which is the child's output.
     *
     * @return the parent's input stream for the child's output
     */
    public InputStream getInputStream() {
        return Channels.newInputStream(in);
    }

    /**
     * Get the selectable channel for the error stream (input for parent, error output for child).
     *
     * @return the parent's error input channel for the child's error output
     */
    public SelectableChannel getErr() {
        return err;
    }

    /**
     * Get the stream for the error stream (input for parent, error output for child).
     *
     * @return the parent's error input stream for the child's error output
     */
    public InputStream getErrorStream() {
        return Channels.newInputStream(err);
    }

    /**
     * Wait for the subprocess to terminate and return its exit code.
     *
     * @return the exit code from the child process, after it has terminated
     */
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

    /**
     * Kill the child process with a KILL signal.
     *
     * @return the return code from the native kill function
     */
    public int kill() {
        return kill(Signal.SIGKILL);
    }

    /**
     * Kill the child process with the specified signal.
     *
     * @param sig the signal to send to the child process
     * @return the return code from the native kill function
     */
    public int kill(Signal sig) {
        return posix.kill((int)pid, sig.intValue());
    }

    /**
     * Kill the child process and all its descendants with a KILL signal.
     *
     * @return the return code from the native kill function
     */
    public int killProcessGroup() {
        return killProcessGroup(Signal.SIGKILL);
    }

    /**
     * Kill the child process and all its descendants with the specified signal.
     *
     * @param sig the signal to send to the child and its descendants
     * @return the return code from the native kill function
     */
    public int killProcessGroup(Signal sig) {
        return posix.kill(-(int)pid, sig.intValue());
    }

    /**
     * Get the exit code from the child process, or raise {@link IllegalThreadStateException} if it has not yet
     * terminated.
     *
     * @return the exit value from the terminated child process
     */
    public long exitValue() {
        if (exitValue == -1) {
            throw new IllegalThreadStateException("subprocess has not yet completed");
        }
        return exitValue;
    }
}
