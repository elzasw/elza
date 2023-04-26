package cz.tacr.elza.domain;

import java.io.Serializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.ArrFundGetter;
import cz.tacr.elza.domain.interfaces.Versionable;


/**
 * Vytvářená nebo již schválená verze archivní pomůcky. Základem archivní pomůcky je hierarchický archivní
 * popis. Každá pomůcka je vytvářena podle určitých pravidel tvorby. Pravidla tvorby mohou definovat
 * různé typy finální pomůcky (například manipulační seznam, inventární seznam, katalog v případě
 * ZP)
 *
 * @since 22.7.15
 */
@Entity(name = "arr_fund_version")
@Cache(region = "fund", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrFundVersion extends AbstractVersionableEntity implements Versionable, Serializable, ArrFundGetter {

    public static final String TABLE_NAME = "arr_fund_version";

    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_LOCK_CHANGE_ID = "lockChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer fundVersionId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_LOCK_CHANGE_ID, nullable = true)
    private ArrChange lockChange;

    @Column(nullable = true, insertable = false, updatable = false)
    private Integer lockChangeId;

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
        this.lockChangeId = lockChange != null ? lockChange.getChangeId() : null;
    }

    public Integer getLockChangeId() {
        return lockChangeId;
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
        this.fundId = fund != null ? fund.getFundId() : null;
    }

    public Integer getFundId() {
        return fundId;
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
