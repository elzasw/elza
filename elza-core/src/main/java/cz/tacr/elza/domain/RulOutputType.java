package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Implementace třídy {@link cz.tacr.elza.api.RulOutputType}
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.6.2016
 */
@Entity(name = "rul_output_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulOutputType implements cz.tacr.elza.api.RulOutputType<RulPackage, RulRule> {

    @Id
    @GeneratedValue
    private Integer outputTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRule.class)
    @JoinColumn(name = "ruleId", nullable = true)
    private RulRule rule;

    @Override
    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    @Override
    public void setOutputTypeId(Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public RulRule getRule() {
        return rule;
    }

    @Override
    public void setRule(final RulRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.RulOutputType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.RulOutputType other = (cz.tacr.elza.domain.RulOutputType) obj;

        return new EqualsBuilder().append(outputTypeId, other.getOutputTypeId()).isEquals();
    }
}
