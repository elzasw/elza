package cz.tacr.elza.controller.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.api.InterpiClass;

/**
 * Jedna položka pro INTERPI mapování.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 12. 2016
 */
public class InterpiMappingItemVO {

    private Integer id;

    /** Typ vztahu ELZA. */
    private Integer relationRoleTypeId;

    /** Typ role entity ELZA. */
    private Integer relationTypeId;

    private InterpiClass interpiClass;

    /** Typ vztahu INTERPI. */
    private String interpiRelationType;

    /** Typ role INTERPI. */
    private String interpiRoleType;

    /** Příznak zda se má vztak importovat. */
    private boolean importRelation;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getRelationRoleTypeId() {
        return relationRoleTypeId;
    }

    public void setRelationRoleTypeId(final Integer relationRoleTypeId) {
        this.relationRoleTypeId = relationRoleTypeId;
    }

    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    public void setRelationTypeId(final Integer relationTypeId) {
        this.relationTypeId = relationTypeId;
    }

    public InterpiClass getInterpiClass() {
        return interpiClass;
    }

    public void setInterpiClass(final InterpiClass interpiClass) {
        this.interpiClass = interpiClass;
    }

    public String getInterpiRelationType() {
        return interpiRelationType;
    }

    public void setInterpiRelationType(final String interpiRelationType) {
        this.interpiRelationType = interpiRelationType;
    }

    public String getInterpiRoleType() {
        return interpiRoleType;
    }

    public void setInterpiRoleType(final String interpiRoleType) {
        this.interpiRoleType = interpiRoleType;
    }

    public boolean getImportRelation() {
        return importRelation;
    }

    public void setImportRelation(final boolean importRelation) {
        this.importRelation = importRelation;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
