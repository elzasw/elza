package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Implementace třídy {@link cz.tacr.elza.api.RulOutputType}
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.6.2016
 */
@Entity(name = "rul_output_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulOutputType {

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

    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    public void setOutputTypeId(final Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
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

    public RulPackage getPackage() {
        return rulPackage;
    }

    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    public RulRule getRule() {
        return rule;
    }

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
