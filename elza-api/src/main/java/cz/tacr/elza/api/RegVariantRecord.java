package cz.tacr.elza.api;

import cz.tacr.elza.api.interfaces.IRegScope;

import java.io.Serializable;

/**
 * Variantní rejstříkové heslo.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegVariantRecord<RR extends RegRecord> extends Versionable, Serializable, IRegScope {

    /**
     * Vlastní ID.
     * @return  id var. hesla
     */
    Integer getVariantRecordId();

    /**
     * Vlastní ID.
     * @param variantRecordId id var. hesla
     */
    void setVariantRecordId(Integer variantRecordId);

    /**
     * Vazba na heslo rejstříku.
     * @return  objekt hesla
     */
    RR getRegRecord();

    /**
     * Vazba na heslo rejstříku.
     * @param regRecord objekt hesla
     */
    void setRegRecord(RR regRecord);

    /**
     * Obsah hesla.
     * @return obsah variantního hesla
     */
    String getRecord();

    /**
     * Obsah hesla.
     * @param record obsah variantního hesla
     */
    void setRecord(String record);
}
