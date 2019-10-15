package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
