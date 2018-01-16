package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Základní definice pro popis strukturovaného typu a serializaci hodnoty strukturovaného typu.
 *
 * @since 27.10.2017
 */
@Entity(name = "rul_structure_definition")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulStructureDefinition {

    @Id
    @GeneratedValue
    private Integer structureDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructureType.class)
    @JoinColumn(name = "structureTypeId", nullable = false)
    private RulStructureType structureType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId", nullable = false)
    private RulComponent component;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DefType defType;

    @Column(nullable = false)
    private Integer priority;

    /**
     * @return identifikátor entity
     */
    public Integer getStructureDefinitionId() {
        return structureDefinitionId;
    }

    /**
     * @param structureDefinitionId identifikátor entity
     */
    public void setStructureDefinitionId(final Integer structureDefinitionId) {
        this.structureDefinitionId = structureDefinitionId;
    }

    /**
     * @return balíček
     */
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    /**
     * @return komponenta / soubor
     */
    public RulComponent getComponent() {
        return component;
    }

    /**
     * @param component komponenta / soubor
     */
    public void setComponent(final RulComponent component) {
        this.component = component;
    }

    /**
     * @return typ datového typu
     */
    public RulStructureType getStructureType() {
        return structureType;
    }

    /**
     * @param structureType typ datového typu
     */
    public void setStructureType(final RulStructureType structureType) {
        this.structureType = structureType;
    }

    /**
     * @return typ pravidel
     */
    public DefType getDefType() {
        return defType;
    }

    /**
     * @param defType typ pravidel
     */
    public void setDefType(final DefType defType) {
        this.defType = defType;
    }

    /**
     * @return priorita vykonávání
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * @param priority priorita vykonávání
     */
    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    /**
     * Typ definice.
     */
    public enum DefType {

        /**
         * Popis struktury.
         */
        ATTRIBUTE_TYPES,

        /**
         * Serializace hodnoty.
         */
        SERIALIZED_VALUE
    }
}