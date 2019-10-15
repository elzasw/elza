/**
 *
 */
package cz.tacr.elza;

import java.util.ArrayList;
import java.util.List;

public class TestDBKonzole {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Na vstupu musí být minimálně 3 parametry" +
                    "(url, uzivatel, heslo). Zadano " + args.length);
        }

        String url = args[0];
        String uzivatel = args[1];
        String heslo = args[2];

        List<String> params = new ArrayList<String>();

        boolean createDB = false;
        String script = null;
        if (args.length > 3) {  // dalsi parametry se tykaji zakladaciho skriptu DB
            createDB = true;
            script = SqlScript.getScript(args[3]);
            for (int i = 4; i < args.length; i++) {
                System.out.println("pridan parametr sql:" + args[i]);
                params.add(args[i]);
            }
        }

        JdbcConnection jc = new JdbcConnection();
        jc.setUrl(url.trim());
        jc.setHeslo(heslo.trim());
        jc.setUzivatel(uzivatel.trim());
        jc.setParams(params);
        jc.setPrikaz(script);

        KonzoleVystup konzoleVystup = new KonzoleVystup();
        try {
            konzoleVystup.initialize();
            jc.initializeConnection();
            if (!createDB) {
                System.out.println("Bude spusten test pripojeni");
                jc.testConnection();
            } else {
                System.out.println("Bude spusten zakladaci skript k databazi");
                jc.runSql();
            }
            konzoleVystup.print("ok");
        } catch (Exception e) {
            konzoleVystup.print("chyba:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
        konzoleVystup.close();
    }

}
