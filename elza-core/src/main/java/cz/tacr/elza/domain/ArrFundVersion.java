package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Vytvářená nebo již schválená verze archivní pomůcky. Základem archivní pomůcky je hierarchický archivní
 * popis. Každá pomůcka je vytvářena podle určitých pravidel tvorby. Pravidla tvorby mohou definovat
 * různé typy finální pomůcky (například manipulační seznam, inventární seznam, katalog v případě
 * ZP)
 *
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "arr_fund_version")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFundVersion extends AbstractVersionableEntity implements
        cz.tacr.elza.api.ArrFundVersion<ArrFund, ArrChange, ArrNode, RulArrangementType, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer fundVersionId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lockChangeId", nullable = true)
    private ArrChange lockChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "rootNodeId", nullable = false)
    private ArrNode rootNode;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulArrangementType.class)
    @JoinColumn(name = "arrangementTypeId", nullable = false)
    private RulArrangementType arrangementType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "lastChangeId", nullable = false)
    private ArrChange lastChange;

    @Override
    public Integer getFundVersionId() {
        return fundVersionId;
    }

    @Override
    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getLockChange() {
        return lockChange;
    }

    @Override
    public void setLockChange(final ArrChange lockChange) {
        this.lockChange = lockChange;
    }

    @Override
    public ArrNode getRootNode() {
        return rootNode;
    }

    @Override
    public void setRootNode(final ArrNode rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public RulArrangementType getArrangementType() {
        return arrangementType;
    }

    @Override
    public void setArrangementType(final RulArrangementType arrangementType) {
        this.arrangementType = arrangementType;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public ArrChange getLastChange() {
        return lastChange;
    }

    @Override
    public void setLastChange(final ArrChange lastChange) {
        this.lastChange = lastChange;
    }

    @Override
    public String toString() {
        return "ArrFundVersion pk=" + fundVersionId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        cz.tacr.elza.domain.ArrFundVersion version = (cz.tacr.elza.domain.ArrFundVersion) obj;

        if (fundVersionId != null ? !fundVersionId.equals(version.fundVersionId) : version.fundVersionId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return fundVersionId != null ? fundVersionId.hashCode() : 0;
    }
}
