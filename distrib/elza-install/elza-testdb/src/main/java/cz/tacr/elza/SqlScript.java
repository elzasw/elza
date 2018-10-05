/**
 *
 */
package cz.tacr.elza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SqlScript {

    public static String getScript(final String file) {
        String fileName = file;
        if (!fileName.endsWith(".sql")) {
            fileName += ".sql";
        }
        System.out.println("hledam skript " + fileName);

        File dir = new File("scripts");
        File fileSql = null;
        if (dir.isDirectory()) {
            for (File soubor : dir.listFiles()) {
                if (soubor.isFile() && soubor.getName().equalsIgnoreCase(fileName)) {
                    System.out.println("nalezen sql soubor " + soubor.getName());
                    fileSql = soubor;
                }
            }
        }

        if (fileSql == null) {
            return null;
        }

        String result = null;
        try {
            try(BufferedReader br = new BufferedReader(new FileReader(fileSql));)
            {
                String prikaz = null;
                while ((prikaz = br.readLine()) != null) {
                    if (prikaz.trim().length() == 0) {
                        continue;
                    }
                    if (result == null) {
                        result = prikaz + "\n";
                    } else {
                        result += prikaz + "\n";
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("nelze nacist sql script:" + e.getLocalizedMessage());
        }
        return result;
    }
}
