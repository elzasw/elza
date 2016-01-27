package cz.tacr.elza.api;

import java.io.Serializable;
import java.util.List;

/**
 * Záznam v rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecord<RT extends RegRegisterType, ES extends RegExternalSource, VR extends RegVariantRecord,
        RR extends RegRecord, RS extends RegScope>
    extends Versionable, Serializable {

    /**
     * ID hesla.
     * @return  id hesla
     */
    Integer getRecordId();

    /**
     * ID hesla.
     * @param recordId  id hesla
     */
    void setRecordId(Integer recordId);

    /**
     * Typ rejstříku.
     * @return  typ rejstříku
     */
    RT getRegisterType();

    /**
     * Typ rejstříku.
     * @param registerType typ rejstříku
     */
    void setRegisterType(RT registerType);

    /**
     * Nadřazený záznam rejstříku.
     * @return  nadřazený záznam rejstříku
     */
    RR getParentRecord();

    /**
     * Nadřazený záznam rejstříku.
     * @param parentRecord nadřazený záznam rejstříku
     */
    void setParentRecord(RR parentRecord);

    /**
     * Externí zdroj hesel.
     * @return  externí zdroj hesel
     */
    ES getExternalSource();

    /**
     * Externí zdroj hesel.
     * @param externalSource externí zdroj hesel
     */
    void setExternalSource(ES externalSource);

    /**
     * Rejstříkové heslo.
     * @return rejstříkové heslo
     */
    String getRecord();

    /**
     * Rejstříkové heslo.
     * @param record rejstříkové heslo
     */
    void setRecord(String record);

    /**
     * Podrobná charakteristika rejstříkového hesla.
     * @return podrobná charakteristika rejstříkového hesla
     */
    String getCharacteristics();

    /**
     * Podrobná charakteristika rejstříkového hesla.
     * @param characteristics podrobná charakteristika rejstříkového hesla.
     */
    void setCharacteristics(String characteristics);

    /**
     * Poznámka k heslu v rejstříku.
     * @return poznámka k heslu v rejstříku
     */
    String getNote();

    /**
     * Poznámka k heslu v rejstříku.
     * @param note poznámka k heslu v rejstříku
     */
    void setNote(String note);

    /**
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. Lokální heslo je přiřazené pouze konkrétnímu
     * archivnímu popisu/pomůcce.
     *
     * @return příznak, zda se jedná o lokální nebo globální rejstříkové heslo
     */
    Boolean getLocal();

    /**
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. Lokální heslo je přiřazené pouze konkrétnímu
     * archivnímu popisu/pomůcce.
     *
     * @param local příznak, zda se jedná o lokální nebo globální rejstříkové heslo
     */
    void setLocal(Boolean local);

    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     * @return externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi
     */
    String getExternalId();

    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     *
     * @param externalId externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi
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

    /**
     * @return třída rejstříku
     */
    RS getScope();


    /**
     * @param scope třída rejstříku
     */
    void setScope(RS scope);
}
