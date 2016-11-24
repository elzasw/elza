package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.UseUnitdateEnum;


/**
 * Seznam typů vztahů.
 */
@Entity(name = "par_relation_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParRelationType implements cz.tacr.elza.api.ParRelationType<ParRelationClassType> {

    @Id
    @GeneratedValue
    private Integer relationTypeId;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UseUnitdateEnum useUnitdate;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParRelationClassType.class)
    @JoinColumn(name = "relationClassTypeId", nullable = false)
    private ParRelationClassType relationClassType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    @Override
    public void setRelationTypeId(final Integer relationTypeId) {
        this.relationTypeId = relationTypeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public ParRelationClassType getRelationClassType() {
        return relationClassType;
    }

    @Override
    public void setRelationClassType(final ParRelationClassType relationClassType) {
        this.relationClassType = relationClassType;
    }

    @Override
    public UseUnitdateEnum getUseUnitdate() {
        return useUnitdate;
    }

    @Override
    public void setUseUnitdate(final UseUnitdateEnum useUnitdate) {
        this.useUnitdate = useUnitdate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParRelationType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParRelationType other = (ParRelationType) obj;

        return new EqualsBuilder().append(relationTypeId, other.getRelationTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(relationTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParRelationType pk=" + relationTypeId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
