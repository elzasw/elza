package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Obdobně jako u základních pravidel popisu, je možné zapnout rozšíření. Rozšíření je ale určené vždy jen pro jeden typ strukrurované hodnoty..
 *
 * @since 30.10.2017
 */
@Entity(name = "rul_structure_extension")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulStructureExtension {

    @Id
    @GeneratedValue
    private Integer structureExtensionId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructureType.class)
    @JoinColumn(name = "structureTypeId", nullable = false)
    private RulStructureType structureType;

    public Integer getStructureExtensionId() {
        return structureExtensionId;
    }

    public void setStructureExtensionId(final Integer structureExtensionId) {
        this.structureExtensionId = structureExtensionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    public RulStructureType getStructureType() {
        return structureType;
    }

    public void setStructureType(final RulStructureType structureType) {
        this.structureType = structureType;
    }
}
