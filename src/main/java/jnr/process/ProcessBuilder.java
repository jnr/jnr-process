package jnr.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.SpawnFileAction;

/**
 * ProcessBuilder mimics the API of the JDK {@link java.lang.ProcessBuilder} class, providing methods to build up and
 * launch a child process.
 */
public class ProcessBuilder {
    private List<String> command;
    private Map<String, String> env;
    
    private static final POSIX posix = POSIXFactory.getPOSIX();

    /**
     * Create a new ProcessBuilder instance with the given command line.
     *
     * @param command the command line as a list of string arguments
     */
    public ProcessBuilder(List<String> command) {
        this.command = new ArrayList<String>(command);
        this.env = new HashMap<String, String>(System.getenv());
    }

    /**
     * Create a new ProcessBuilder instance with the given command line.
     *
     * @param command the command line as a varargs array of string arguments
     */
    public ProcessBuilder(String... command) {
    	this(Arrays.asList(command));
    }

    /**
     * Get a copy of the list of command line arguments that will be used to launch the child process.
     *
     * @return a copied list of command line arguments
     */
    public List<String> command() {
        return new ArrayList<String>(command);
    }

    /**
     * Replace the command line with the given list of command line arguments.
     *
     * @param command the new list of command line arguments
     * @return this ProcessBuilder instance
     */
    public ProcessBuilder command(List<String> command) {
        this.command = new ArrayList<String>(command);
        return this;
    }

    /**
     * Replace the command line with the given varargs array of command line arguments.
     *
     * @param command the new array of command line arguments
     * @return this ProcessBuilder instance
     */
    public ProcessBuilder command(String... command) {
        this.command = Arrays.asList(command);
        return this;
    }
    
    /**
     * Returns a string map view of this process builder's environment.  Whenever a process builder is created, the environment
     * is initialized to a copy of the current process environment (See {@link System#getenv()}).  Subprocesses subsequently
     * started by this object's {@link #start()} method will use this map as their environment.
     * <p>
     * The returned object may be modified using ordinary {@link Map} operators.
     * 
     * @return
     * 		The process builder's environment.
     */
    public Map<String, String> environment() {
    	return this.env;
    }

    /**
     * Launch the subprocess and return a new {@link Process} instance.
     *
     * @return a new Process wrapping the child process's pid and IO streams
     */
    public Process start() {
        int[] stdin = new int[2];
        int[] stdout = new int[2];
        int[] stderr = new int[2];

        // prepare all pipes
        posix.pipe(stdin);
        posix.pipe(stdout);
        posix.pipe(stderr);

        // build env list
        ArrayList<String> envp = new ArrayList<String>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            envp.add(entry.getKey() + "=" + entry.getValue());
        }

        // spawn process, closing parent's descriptors in child
        long pid = posix.posix_spawnp(
                command.get(0),
                Arrays.asList(
                        SpawnFileAction.dup(stdin[0], 0),
                        SpawnFileAction.dup(stdout[1], 1),
                        SpawnFileAction.dup(stderr[1], 2),
                        SpawnFileAction.close(stdin[1]),
                        SpawnFileAction.close(stdout[0]),
                        SpawnFileAction.close(stderr[0])),
                command,
                envp);

        // close child's descriptors in parent
        posix.close(stdin[0]);
        posix.close(stdout[1]);
        posix.close(stderr[1]);

        // construct a Process
        return new Process(posix, pid, stdin[1], stdout[0], stderr[0]);
    }
}
