/**
 *
 */
package cz.tacr.elza.data;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "nastaveni")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "nastaveni")
public class Nastaveni {
    @XmlElement(name = "konektorList")
    private List<JdbcKonektor> konektorList = new ArrayList<JdbcKonektor>();
    @XmlElement(name = "sqlDotaz", required = false)
    private String sqlDotaz;

    public String getSqlDotaz() {
        return sqlDotaz;
    }

    public void setSqlDotaz(String sqlDotaz) {
        this.sqlDotaz = sqlDotaz;
    }

    public List<JdbcKonektor> getKonektorList() {
        return konektorList;
    }

    public void setKonektorList(List<JdbcKonektor> konektorList) {
        this.konektorList = konektorList;
    }

    public static Nastaveni getDefaultInstance() {
        Nastaveni nastaveni = new Nastaveni();
        JdbcKonektor konektor = new JdbcKonektor();
        konektor.setNazev("Oracle driver");
        konektor.setAdresa("oracle.jdbc.driver.OracleDriver");
        nastaveni.getKonektorList().add(konektor);
        konektor = new JdbcKonektor();
        konektor.setNazev("Ms sql driver");
        konektor.setAdresa("net.sourceforge.jtds.jdbc.Driver");
        nastaveni.getKonektorList().add(konektor);
        return nastaveni;
    }

}
