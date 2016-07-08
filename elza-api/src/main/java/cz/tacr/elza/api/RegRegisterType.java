package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Číselník typů rejstříkových hesel.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRegisterType<RRT extends RegRegisterType, PPT extends ParPartyType> extends Serializable {

    /**
     * Vlastní ID.
     * @return  id
     */
    Integer getRegisterTypeId();

    /**
     * Vlastní ID.
     * @param registerTypeId id
     */
    void setRegisterTypeId(Integer registerTypeId);

    /**
     * Kód typu.
     * @return kód typu
     */
    String getCode();

    /**
     * Kód typu.
     * @param code kód typu
     */
    void setCode(String code);

    /**
     * Název typu.
     * @return název typu
     */
    String getName();

    /**
     * Název typu.
     * @param name název typu
     */
    void setName(String name);

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     * @param partyType
     */
    void setPartyType(PPT partyType);

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     * @return Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     */
    PPT getPartyType();

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     * @param parentRegisterType
     */
    void setParentRegisterType(RRT parentRegisterType);

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     * @return Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    RRT getParentRegisterType();

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     * @param addRecord
     */
    void setAddRecord(Boolean addRecord);

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     * @return Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    Boolean getAddRecord();

    /**
     * Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     * @param hierarchical
     */
    void setHierarchical(Boolean hierarchical);

    /**
     * Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     * @return Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     */
    Boolean getHierarchical();
}
