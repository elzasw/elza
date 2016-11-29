package cz.tacr.elza.api;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public interface ParInterpiRoleType<IR extends ParInterpiRelation, RTT extends ParRelationRoleType> {

    Integer getInterpiRoleTypeId();

    void setInterpiRoleTypeId(Integer interpiRoleTypeId);

    IR getInterpiRelation();

    void setInterpiRelation(IR interpiRelation);

    RTT getRelationRoleType();

    void setRelationRoleType(RTT relationRoleType);

    String getName();

    void setName(String name);
}
