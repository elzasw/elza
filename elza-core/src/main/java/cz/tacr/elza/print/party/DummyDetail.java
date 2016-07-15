package cz.tacr.elza.print.party;

/**
 * Pomocné VO aby bylo možné rozumně designovat jasper šablony.
 * <p>
 * Workaround: Vytvoří se jedna instance a předá v listu s jedním řádkem jako data,
 * takže jasperreport vytvoří jeden řádek detailu,
 * přestože se předaná hodnota nikde nepoužije.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 14.7.16
 */
public class DummyDetail {
    private String dummy = "DUMMY";

    public String getDummy() {
        return dummy;
    }
}
