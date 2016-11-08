package cz.tacr.elza.api;

/**
 * Třída typu vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 24. 10. 2016
 */
public interface ParRelationClassType {

    Integer getRelationClassTypeId();

    void setRelationClassTypeId(Integer relationClassTypeId);

    String getName();

    void setName(String name);

    String getCode();

    void setCode(String code);

    ParRelationClassTypeRepeatabilityEnum getRepeatability();

    void setRepeatability(ParRelationClassTypeRepeatabilityEnum repeatibility);
}
