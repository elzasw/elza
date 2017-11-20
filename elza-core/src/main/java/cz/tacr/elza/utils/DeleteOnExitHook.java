package cz.tacr.elza.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Hook pro mazání adresářů při ukončení JVM.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 14.11.2017
 */
class DeleteOnExitHook {
    private static LinkedHashSet<String> files = new LinkedHashSet<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> runHooks()));
    }

    private DeleteOnExitHook() {
    }

    static synchronized void add(String file) {
        if (files == null) {
            // DeleteOnExitHook is running. Too late to add a file
            throw new IllegalStateException("Shutdown in progress");
        }

        files.add(file);
    }

    static synchronized void delete(String file) {
        if (files == null) {
            // DeleteOnExitHook is running. Too late to delete a file
            throw new IllegalStateException("Shutdown in progress");
        }

        files.remove(file);
    }

    static void runHooks() {
        LinkedHashSet<String> theFiles;

        synchronized (DeleteOnExitHook.class) {
            theFiles = files;
            files = null;
        }

        ArrayList<String> toBeDeleted = new ArrayList<>(theFiles);

        for (String path : toBeDeleted) {
//            System.out.println("Delete directory: " + path);
            try {
                TempDirectory.delete(path);
            } catch (Exception ex) {
                // nechceme řešit
            }
        }
    }
}
