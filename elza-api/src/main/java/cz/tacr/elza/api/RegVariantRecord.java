package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Variantní rejstříková hesla.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegVariantRecord<RR extends RegRecord> extends Versionable, Serializable {

    Integer getVariantRecordId();

    void setVariantRecordId(Integer variantRecordId);

    RR getRegRecord();

    void setRegRecord(RR regRecord);

    String getRecord();

    void setRecord(String record);

//    void setRegRecordPar(RR regRecordPar);
//
//    RR getRegRecordPar();
}
