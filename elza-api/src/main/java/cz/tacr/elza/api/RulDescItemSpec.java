package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Evidence možných specifikací typů atributů archivního popisu. Evidence je společná pro všechny
 * archivní pomůcky. Vazba výčtu specifikací na různá pravidla bude řešeno později. Podtyp atributu
 * (Role entit - Malíř, Role entit - Sochař, Role entit - Spisovatel).
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface RulDescItemSpec<RIT extends RulDescItemType, P extends RulPackage>
        extends
        Serializable {

    enum Type {
        /**
         * Povinný
         */
        REQUIRED,

        /**
         * Doporučený
         */
        RECOMMENDED,

        /**
         * Možný
         */
        POSSIBLE,

        /**
         * Nemožný
         */
        IMPOSSIBLE
    }

    Integer getDescItemSpecId();


    void setDescItemSpecId(final Integer descItemSpecId);


    RIT getDescItemType();


    void setDescItemType(final RIT descItemType);


    String getCode();


    void setCode(final String code);


    String getName();


    void setName(final String name);


    String getShortcut();


    void setShortcut(final String shortcut);


    String getDescription();


    void setDescription(final String description);


    /**
     * @return pořadí zobrazení.
     */
    Integer getViewOrder();


    /**
     * @param viewOrder pořadí zobrazení.
     */
    void setViewOrder(final Integer viewOrder);


    /**
     * @return typ udává, zda je povinné/doporučené/... vyplnit hodnotu atributu.
     */
    Type getType();


    /**
     * Typ udává, zda je povinné/doporučené/... vyplnit hodnotu atributu.
     *
     * @param type typ
     */
    void setType(Type type);


    /**
     * @return příznak udává, zda je atribut opakovatelný
     */
    Boolean getRepeatable();


    /**
     * Příznak udává, zda je atribut opakovatelný.
     *
     * @param repeatable opakovatelnost
     */
    void setRepeatable(Boolean repeatable);

    /**
     * @return typ kód typu kontroly
     */
    String getPolicyTypeCode();

    /**
     * @param policyTypeCode kód typu kontroly
     */
    void setPolicyTypeCode(String policyTypeCode);

    /**
     * @return balíček
     */
    P getPackage();


    /**
     * @param rulPackage balíček
     */
    void setPackage(P rulPackage);

}
