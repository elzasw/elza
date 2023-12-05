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
 * Typ datového typu, který je vždy třeba určit, pokud je nějaký atribut popisu strukturovaný.
 *
 * @since 27.10.2017
 */
@Entity(name = "rul_structured_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulStructuredType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer structuredTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean anonymous;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getStructuredTypeId() {
        return structuredTypeId;
    }

    public void setStructuredTypeId(final Integer structuredTypeId) {
        this.structuredTypeId = structuredTypeId;
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

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(final Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

}
