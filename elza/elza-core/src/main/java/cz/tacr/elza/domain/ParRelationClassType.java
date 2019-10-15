package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.enums.ParRelationClassTypeRepeatabilityEnum;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Třída typu vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 24. 10. 2016
 */
@Entity(name = "par_relation_class_type")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
public class ParRelationClassType {

    public static final String CREATION_CODE = "B";
    public static final String DESTRUCTION_CODE = "E";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer relationClassTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_10)
    private ParRelationClassTypeRepeatabilityEnum repeatability;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getRelationClassTypeId() {
        return relationClassTypeId;
    }

    public void setRelationClassTypeId(final Integer relationClassTypeId) {
        this.relationClassTypeId = relationClassTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public ParRelationClassTypeRepeatabilityEnum getRepeatability() {
        return repeatability;
    }

    public void setRepeatability(final ParRelationClassTypeRepeatabilityEnum repeatability) {
        this.repeatability = repeatability;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
