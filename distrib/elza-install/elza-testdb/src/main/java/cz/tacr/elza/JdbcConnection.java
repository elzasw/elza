/**
 *
 */
package cz.tacr.elza;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcConnection {
    private String adresa = null;
    private String url = null;
    private String uzivatel = null;
    private String heslo = null;
    private String prikaz = null;
    private List<String> params = new ArrayList<String>();

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUzivatel() {
        return uzivatel;
    }

    public void setUzivatel(String uzivatel) {
        this.uzivatel = uzivatel;
    }

    public String getHeslo() {
        return heslo;
    }

    public void setHeslo(String heslo) {
        this.heslo = heslo;
    }

    public String getPrikaz() {
        return prikaz;
    }

    public void setPrikaz(String prikaz) {
        this.prikaz = prikaz;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public void initializeConnection() throws SQLException, ClassNotFoundException
            , InstantiationException, IllegalAccessException {
        if (url == null || url.isEmpty()) {
            throw new NullPointerException("Není vyplněno URL databáze");
        }
        if (uzivatel == null || uzivatel.isEmpty()) {
            throw new NullPointerException("Není vyplněn uživatel");
        }
        if (heslo == null || heslo.isEmpty()) {
            throw new NullPointerException("Není vyplněno heslo k databázi");
        }

        registerDriver("net.sourceforge.jtds.jdbc.Driver");
        registerDriver("org.postgresql.Driver");
    }

    public void testConnection() throws SQLException {
        try (Connection con = DriverManager.getConnection(url, uzivatel, heslo);) {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("pripojeno k databazi " + meta.getDatabaseProductName()
                    + " " + meta.getDatabaseProductVersion());
        }
    }

    public void runSql() throws SQLException {
        try (Connection con = DriverManager.getConnection(url, uzivatel, heslo);) {
            if (prikaz != null && !prikaz.trim().isEmpty()) {
                prikaz = updateParametr(prikaz, params);
                String[] subPrikazList = prikaz.split(";");
                for (String subPrikaz : subPrikazList) {
                    if (subPrikaz == null || subPrikaz.trim().length() == 0) {
                        continue;
                    }
                    try (PreparedStatement ps = con.prepareStatement(subPrikaz);) {
                        ps.execute();
                    }
                }
            }
        }
    }

    private String updateParametr(String input, List<String> params) {
        Map<Integer, String> paramMap = new HashMap<Integer, String>();
        int index = 1;
        for (String param : params) {
            paramMap.put(index++, param);
        }
        int startIndex = 0;
        int endIndex = 0;

        String result = "";
        do {
            startIndex = input.indexOf("<", startIndex);
            if (startIndex >= 0) {
                result += input.substring(endIndex, startIndex);
                endIndex = input.indexOf(">", startIndex);
                if (endIndex < startIndex) {
                    throw new RuntimeException("Nenalezen ukoncovaci znak > za pozici " + startIndex);
                }
                String param = input.substring(startIndex + 1, endIndex);
                Integer paramIndex = Integer.valueOf(param);
                param = paramMap.get(paramIndex);
                if (param == null) {
                    throw new RuntimeException("parameter pro index " + paramIndex);
                }
                result += param;
                startIndex = endIndex;
                endIndex++;
            }
        } while (startIndex >= 0);

        result += input.substring(endIndex, input.length());
        return result;
    }

    private void registerDriver(final String adresa) throws SQLException, InstantiationException
            , IllegalAccessException, ClassNotFoundException {
        Class clazz = Class.forName(adresa);
        Driver driver;
        driver = (Driver) clazz.newInstance();
        DriverManager.registerDriver(driver);
    }

}
