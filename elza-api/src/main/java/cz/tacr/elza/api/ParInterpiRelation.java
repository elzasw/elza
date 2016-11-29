package cz.tacr.elza.api;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public interface ParInterpiRelation<RT extends ParRelationType> {

    Integer getInterpiRelationId();

    void setInterpiRelationId(Integer interpiRelationId);

    RT getRelationType();

    void setRelationType(RT relationType);

    InterpiClass getCls();

    void setCls(InterpiClass cls);

    String getName();

    void setName(String name);
}
