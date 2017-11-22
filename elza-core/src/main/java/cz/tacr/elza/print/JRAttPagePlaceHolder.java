package cz.tacr.elza.print;

/**
 * VO zajišťující placeholder stránky pro přílohy
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 * Date: 10.11.17
 */
public class JRAttPagePlaceHolder {
    public static final String INCL_PATTERN = "#include_attacements"; // značka pro vložení příloh

    public String getAttPage() {
        return INCL_PATTERN;
    }
}
