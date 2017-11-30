package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
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

import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.domain.interfaces.Versionable;


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
@Cache(region = "fund", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFundVersion extends AbstractVersionableEntity implements Versionable, Serializable, IArrFund {

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
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer rootNodeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(name = "fundId", insertable = false, updatable = false)
    private Integer fundId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer ruleSetId;

    @Column(nullable = true)
    private String dateRange;

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public void setFundVersionId(final Integer fundVersionId) {
        this.fundVersionId = fundVersionId;
    }

    /**
     * @return číslo změny vytvoření pomůcky.
     */
    public ArrChange getCreateChange() {
        return createChange;
    }

    /**
     * @param createChange číslo změny vytvoření pomůcky.
     */
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    /**
     * @return číslo změny uzamčení pomůcky.
     */
    public ArrChange getLockChange() {
        return lockChange;
    }

    /**
     * @param lockChange číslo změny uzamčení pomůcky.
     */
    public void setLockChange(final ArrChange lockChange) {
        this.lockChange = lockChange;
    }

    /**
     * @return odkaz na root uzel struktury archivního popisu.
     */
    public ArrNode getRootNode() {
        return rootNode;
    }

    /**
     * @param rootNode odkaz na root uzel struktury archivního popisu .
     */
    public void setRootNode(final ArrNode rootNode) {
        this.rootNode = rootNode;
        this.rootNodeId = rootNode != null ? rootNode.getNodeId() : null;
    }

    public Integer getRootNodeId() {
        return rootNodeId;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    /**
     * @param fund identifikátor archívní pomůcky.
     */
    public void setFund(final ArrFund fund) {
        this.fund = fund;
        if (fund == null) {
        	this.fundId = null;
        } else {
        	this.fundId = fund.getFundId();
        }
    }

    /**
     * @return odkaz na pravidla tvorby.
     */
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * @param ruleSet odkaz na pravidla tvorby.
     */
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    /**
     * @return vysčítaná informace o časovém rozsahu fondu - sdruženo po typech kalendářů
     */
    public String getDateRange() {
        return dateRange;
    }

    /**
     * @param dateRange vysčítaná informace o časovém rozsahu fondu - sdruženo po typech kalendářů
     */
    public void setDateRange(final String dateRange) {
        this.dateRange = dateRange;
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

	public Integer getFundId() {
		return fundId;
	}
}
