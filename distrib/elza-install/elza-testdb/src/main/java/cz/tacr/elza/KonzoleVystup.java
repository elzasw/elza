/**
 *
 */
package cz.tacr.elza;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class KonzoleVystup {

    private PrintWriter pw = null;

    public void initialize() {
        FileWriter fw = null;
        try {
            fw = new FileWriter("testLog.txt");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BufferedWriter bw = new BufferedWriter(fw);
        pw = new PrintWriter(bw);
    }

    public void print(String message) {
        System.out.println(message);
        if (pw == null) {
            initialize();
        }
        pw.println(message);
    }

    public void close() {
        if (pw != null) {
            pw.close();
        }
    }
}
