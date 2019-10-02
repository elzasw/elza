/**
 *
 */
package cz.tacr.elza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SqlScript {

    /**
     * Return content of SQL script file
     * 
     * @param fileName
     *            Name of SQL script
     * @return
     */
    public static String getScript(final String fileName) {

        File fileSql = findSqlScript(fileName);
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

    private static File findSqlScript(String fileName) {
        if (!fileName.endsWith(".sql")) {
            fileName = fileName + ".sql";
        }
        System.out.println("hledam skript " + fileName);

        File dir = new File("scripts");
        if (dir.isDirectory()) {
            for (File soubor : dir.listFiles()) {
                if (soubor.isFile() && soubor.getName().equalsIgnoreCase(fileName)) {
                    System.out.println("nalezen sql soubor " + soubor.getName());
                    return soubor;
                }
            }
        }

        return null;
    }
}
