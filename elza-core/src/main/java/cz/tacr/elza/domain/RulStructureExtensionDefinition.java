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
 * Definice pro popis strukturovaného typu, které definuje dané rozšíření.
 *
 * @since 27.10.2017
 */
@Entity(name = "rul_structure_extension_definition")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulStructureExtensionDefinition {

    @Id
    @GeneratedValue
    private Integer structureExtensionDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructureExtension.class)
    @JoinColumn(name = "structureExtensionId", nullable = false)
    private RulStructureExtension structureExtension;

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
    public Integer getStructureExtensionDefinitionId() {
        return structureExtensionDefinitionId;
    }

    /**
     * @param structureExtensionDefinitionId identifikátor entity
     */
    public void setStructureExtensionDefinitionId(final Integer structureExtensionDefinitionId) {
        this.structureExtensionDefinitionId = structureExtensionDefinitionId;
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
    public RulStructureExtension getStructureExtension() {
        return structureExtension;
    }

    /**
     * @param structureExtension typ datového typu
     */
    public void setStructureExtension(final RulStructureExtension structureExtension) {
        this.structureExtension = structureExtension;
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