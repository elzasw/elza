package cz.tacr.elza.api;

/**
 * Mapování vztahů mezi INTERPI a ELZA.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 11. 2016
 */
public interface ParInterpiMapping<RTT extends ParRelationRoleType, RT extends ParRelationType> {

    Integer getInterpiMappingId();

    void setInterpiMappingId(Integer interpiRelationId);

    RTT getRelationRoleType();

    void setRelationRoleType(RTT relationRoleType);

    RT getRelationType();

    void setRelationType(RT relation);

    InterpiClass getInterpiClass();

    void setInterpiClass(InterpiClass interpiClass);

    String getInterpiRelationType();

    void setInterpiRelationType(String interpiRelationType);

    String getInterpiRoleType();

    void setInterpiRoleType(String interpiRoleType);
}
