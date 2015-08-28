package cz.tacr.elza.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Záznam rejstříku - externí použití.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class RegRecordExt extends RegRecord implements cz.tacr.elza.api.RegRecordExt<RegRegisterType, RegExternalSource, RegVariantRecord> {

    /** Variantní záznamy pro ext použití. */
    private List<RegVariantRecord> variantRecordsExt = new ArrayList<>(0);


    /**
     * @param variantRecordsExt     variantní záznamy pro externí použití
     */
    @Override
    public void setVariantRecordsExt(final List<RegVariantRecord> variantRecordsExt) {
        this.variantRecordsExt = variantRecordsExt;
    }

    /**
     * @return  variantní záznamy pro externí použití
     */
    @Override
    public List<RegVariantRecord> getVariantRecordsExt() {
        return variantRecordsExt;
    }

}
