package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;


/**
 * Objekt AS spolu s otevřenou verzí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public class ArrFundOpenVersion {

    private ArrFund fund;
    private ArrFundVersion openVersion;

    public ArrFundOpenVersion(final ArrFund fund, final ArrFundVersion version) {
        this.fund = fund;
        this.openVersion = version;
    }

    public ArrFund getFund() {
        return fund;
    }

    public ArrFundVersion getOpenVersion() {
        return openVersion;
    }
}
