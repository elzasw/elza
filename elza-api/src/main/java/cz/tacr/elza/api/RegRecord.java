package cz.tacr.elza.api;

import java.io.Serializable;
import java.util.List;

/**
 * Záznamy v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecord<RT extends RegRegisterType, ES extends RegExternalSource, VR extends RegVariantRecord>
    extends Versionable, Serializable {

    Integer getRecordId();

    void setRecordId(Integer recordId);

    RT getRegisterType();

    void setRegisterType(RT registerType);

    ES getExternalSource();

    void setExternalSource(ES externalSource);

    /**
     * @return rejstříkové heslo.
     */
    String getRecord();

    /**
     * @param record rejstříkové heslo
     */
    void setRecord(String record);

    /**
     * @return podrobná charakteristika rejstříkového hesla.
     */
    String getCharacteristics();

    /**
     * @param characteristics podrobná charakteristika rejstříkového hesla.
     */
    void setCharacteristics(String characteristics);

    /**
     * @return poznámka k heslu v rejstříku,
     */
    String getComment();

    /**
     * @param comment poznámka k heslu v rejstříku,
     */
    void setComment(String comment);

    /**
     * @return příznak, zda se jedná o lokální nebo globální rejstříkové heslo. lokální heslo je
     *         přiřazené pouze konkrétnímu archivnímu popisu/pomůcce.
     */
    Boolean getLocal();

    /**
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. lokální heslo je přiřazené
     * pouze konkrétnímu archivnímu popisu/pomůcce.
     *
     * @param local příznak, zda se jedná o lokální nebo globální rejstříkové heslo.
     */
    void setLocal(Boolean local);

    /**
     * @return externí identifikátor rejstříkového hesla v externím zdroji záznamů, například
     *         interpi.
     */
    String getExternalId();

    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     *
     * @param externalId externí identifikátor rejstříkového hesla.
     */
    void setExternalId(String externalId);

    /**
     * Vazba na variantní záznamy.
     *
     * @param variantRecordList množina záznamů.
     */
    void setVariantRecordList(List<VR> variantRecordList);

    /**
     * Vazba na variantní záznamy.
     *
     * @return množina, může být prázdná.
     */
    List<VR> getVariantRecordList();
}
