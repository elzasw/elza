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
        FileWriter fw;
        BufferedWriter bw;
        try() {
            fw = new FileWriter("testLog.txt");
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
        } catch (IOException e) {
            e.printStackTrace();            
            if(bw!=null) {
                try {
                    bw.close();
                    bw=null;
                }
                catch (IOException e) {
                    // not checked
                }
            }
            if(fw!=null) {
                try {
                    fw.close();
                    fw=null;
                }
                catch (IOException e) {
                    // not checked
                }
            }
            return;
        }
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
