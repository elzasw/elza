package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Obdobně jako u základních pravidel popisu, je možné zapnout rozšíření. Rozšíření je ale určené vždy jen pro jeden typ strukrurované hodnoty..
 *
 * @since 30.10.2017
 */
@Entity(name = "rul_structured_type_extension")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulStructuredTypeExtension {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer structuredTypeExtensionId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructuredType.class)
    @JoinColumn(name = "structuredTypeId", nullable = false)
    private RulStructuredType structuredType;

    public Integer getStructuredTypeExtensionId() {
        return structuredTypeExtensionId;
    }

    public void setStructuredTypeExtensionId(final Integer structuredTypeExtensionId) {
        this.structuredTypeExtensionId = structuredTypeExtensionId;
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

    public RulStructuredType getStructuredType() {
        return structuredType;
    }

    public void setStructuredType(final RulStructuredType structuredType) {
        this.structuredType = structuredType;
    }
}
