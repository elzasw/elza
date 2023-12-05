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
 * Typ části, který je vždy třeba určit.
 *
 * @since 20.04.2020
 */
@Entity(name = "rul_part_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulPartType {

    public static final String CODE = "code";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "child_part_id")
    private RulPartType childPart;

    @Column(nullable = false)
    private Boolean repeatable;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getPartTypeId() {
        return partTypeId;
    }

    public void setPartTypeId(final Integer partTypeId) {
        this.partTypeId = partTypeId;
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

    public RulPartType getChildPart() {
        return childPart;
    }

    public void setChildPart(RulPartType childPart) {
        this.childPart = childPart;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

}
