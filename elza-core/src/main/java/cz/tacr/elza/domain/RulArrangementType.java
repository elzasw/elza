package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typ výstupu podle zvolených pravidel tvorby. V případě základních pravidel se jedná o manipulační
 * seznam, inventář, katalog. Typ výstupu se používá pro kontrolu struktury archivního popisu. Je
 * realizována pouze entita obalující, nikoli další objekty, které realizují kontroly.
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_arrangement_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulArrangementType implements cz.tacr.elza.api.RulArrangementType<RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer arrangementTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Override
    public Integer getArrangementTypeId() {
        return arrangementTypeId;
    }

    @Override
    public void setArrangementTypeId(final Integer arrangementTypeId) {
        this.arrangementTypeId = arrangementTypeId;
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
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public String toString() {
        return "RulArrangementType pk=" + arrangementTypeId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulArrangementType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulArrangementType other = (RulArrangementType) obj;

        return new EqualsBuilder().append(arrangementTypeId, other.getArrangementTypeId()).isEquals();
    }
}
