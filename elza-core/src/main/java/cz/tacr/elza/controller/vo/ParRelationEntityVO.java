package cz.tacr.elza.controller.vo;

/**
 * Vazba mezi osobami.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
public class ParRelationEntityVO {

    private Integer relationEntityId;

    private Integer relationId;

    /**
     * Rejstříkové heslo.
     */
    private RegRecordVO record;

    /**
     * Typ vztahu.
     */
    private ParRelationRoleTypeVO roleType;

    public Integer getRelationEntityId() {
        return relationEntityId;
    }

    public void setRelationEntityId(final Integer relationEntityId) {
        this.relationEntityId = relationEntityId;
    }

    public Integer getRelationId() {
        return relationId;
    }

    public void setRelationId(final Integer relationId) {
        this.relationId = relationId;
    }

    public RegRecordVO getRecord() {
        return record;
    }

    public void setRecord(final RegRecordVO record) {
        this.record = record;
    }

    public ParRelationRoleTypeVO getRoleType() {
        return roleType;
    }

    public void setRoleType(final ParRelationRoleTypeVO roleType) {
        this.roleType = roleType;
    }
}
