package cz.tacr.elza.api;

import java.util.List;

/**
 * Rejstřík - pro externí použití.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecordExt<RT extends RegRegisterType, ES extends RegExternalSource, VR extends RegVariantRecord>
        extends RegRecord<RT, ES, VR> {

    void setVariantRecordsExt(List<VR> variantRecordList);

    List<VR> getVariantRecordsExt();

}
