package jnr.process;

import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.SpawnAttribute;
import jnr.posix.SpawnFileAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.io.File;

/**
 * Created by headius on 1/19/15.
 */
public class ProcessBuilder {
    private List<String> command;
    private static final POSIX posix = POSIXFactory.getPOSIX();

    public ProcessBuilder(List<String> command) {
        this.command = new ArrayList<String>(command);
    }

    public ProcessBuilder(String... command) {
        this.command = Arrays.asList(command);
    }

    public List<String> command() {
        return new ArrayList<String>(command);
    }

    public ProcessBuilder command(List<String> command) {
        this.command = new ArrayList<String>(command);
        return this;
    }

    public ProcessBuilder command(String... command) {
        this.command = Arrays.asList(command);
        return this;
    }

    public File directory() {
        return new File(posix.getcwd());
    }

    public ProcessBuilder directory(String dir) {
        posix.chdir(dir);
        return this;
    }

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
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
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
