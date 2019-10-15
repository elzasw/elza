package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.util.Assert;

import cz.tacr.elza.api.enums.InterpiClass;

/**
 * Mapování entity ve vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 12. 2016
 */
public class InterpiRelationMappingVO {

    /** Id záznamu v ELZA. */
    private Integer id;

    /** Typ role entity ELZA. */
    private Integer relationTypeId;

    private InterpiClass interpiClass;

    /** Typ vztahu INTERPI. */
    private String interpiRelationType;

    /** Příznak zda se má vztah importovat. */
    private boolean importRelation;

    /** Seznam entit. */
    private List<InterpiEntityMappingVO> entities;

    /** Příznak zda se má mapování uložit. */
    private boolean save;

    public void addEntityMapping(final InterpiEntityMappingVO entityMappingVO) {
        Assert.notNull(entityMappingVO, "Mapovací entita musí být vyplněna");

        if (entities == null) {
            entities = new LinkedList<>();
        }
        entities.add(entityMappingVO);
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
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

    public boolean getImportRelation() {
        return importRelation;
    }

    public void setImportRelation(final boolean importRelation) {
        this.importRelation = importRelation;
    }

    public List<InterpiEntityMappingVO> getEntities() {
        return entities;
    }

    public void setEntities(final List<InterpiEntityMappingVO> entities) {
        this.entities = entities;
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
