package cz.tacr.elza.controller.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Mapování entity ve vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 3. 1. 2017
 */
public class InterpiEntityMappingVO {

    /** Id záznamu v ELZA. */
    private Integer id;

    /** Typ vztahu ELZA. */
    private Integer relationRoleTypeId;

    /** Typ role INTERPI. */
    private String interpiRoleType;

    /** Název entity v roli. */
    private String interpiEntityName;

    /** Příznak zda se má entita importovat. */
    private boolean importEntity;

    /** Příznak zda se má mapování uložit. */
    private boolean save;

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

    public String getInterpiRoleType() {
        return interpiRoleType;
    }

    public void setInterpiRoleType(final String interpiRoleType) {
        this.interpiRoleType = interpiRoleType;
    }

    public String getInterpiEntityName() {
        return interpiEntityName;
    }

    public void setInterpiEntityName(final String interpiEntityName) {
        this.interpiEntityName = interpiEntityName;
    }
    public boolean getImportEntity() {
        return importEntity;
    }

    public void setImportEntity(final boolean importRelation) {
        this.importEntity = importRelation;
    }

    public boolean getSave() {
        return save;
    }

    public void setSave(final boolean save) {
        this.save = save;
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
