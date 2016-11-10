package cz.tacr.elza.api;

/**
 * Nastavení zobrazení formuláře pro osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 25. 10. 2016
 */
public interface UIPartyGroup<PT extends ParPartyType> {

    Integer getPartyGroupId();

    void setPartyGroupId(Integer partyGroupId);

    PT getPartyType();

    void setPartyType(PT partyType);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    Integer getViewOrder();

    void setViewOrder(Integer viewOrder);

    UIPartyGroupTypeEnum getType();

    void setType(UIPartyGroupTypeEnum type);

    String getContentDefinition();

    void setContentDefinition(String contentDefinition);
}
