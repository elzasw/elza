package cz.tacr.elza.domain.vo;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;


/**
 * Objekt AS spolu s otevřenou verzí.
 */
// TODO: This object is probably useless, fund can be 
//       fetched together with version
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
