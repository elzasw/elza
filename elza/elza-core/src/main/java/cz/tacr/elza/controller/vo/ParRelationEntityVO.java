package cz.tacr.elza.controller.vo;

/**
 * Vazba mezi osobami.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 04.01.2016
 */
public class ParRelationEntityVO {

    private Integer id;

    private Integer relationId;

    /**
     * Rejstříkové heslo.
     */
    private ApAccessPointVO record;

    /**
     * Typ vztahu.
     */
    private ParRelationRoleTypeVO roleType;

    private String note;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getRelationId() {
        return relationId;
    }

    public void setRelationId(final Integer relationId) {
        this.relationId = relationId;
    }

    public ApAccessPointVO getRecord() {
        return record;
    }

    public void setRecord(final ApAccessPointVO record) {
        this.record = record;
    }

    public ParRelationRoleTypeVO getRoleType() {
        return roleType;
    }

    public void setRoleType(final ParRelationRoleTypeVO roleType) {
        this.roleType = roleType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }
}
