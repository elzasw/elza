package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Číselník typů rejstříků.
 */
@Entity(name = "ap_type")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
public class ApType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer apTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean readOnly;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "parentApTypeId")
    private ApType parentApType;

    @Column(insertable = false, updatable = false)
    private Integer parentApTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId")
    private ParPartyType partyType;

    @Column(insertable = false, updatable = false, nullable = true)
    private Integer partyTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRuleSystem.class)
    @JoinColumn(name = "ruleSystemId")
    private ApRuleSystem ruleSystem;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public ApType() {

    }

    /**
     * Copy constructor
     * 
     * @param src
     */
    protected ApType(ApType src) {
        // source maybe lazy hibernate obj -> have to read data with methods 
        this.apTypeId = src.getApTypeId();
        this.code = src.getCode();
        this.name = src.getName();
        this.readOnly = src.isReadOnly();
        this.parentApType = src.getParentApType();
        this.parentApTypeId = src.getParentApTypeId();
        this.partyType = src.getPartyType();
        this.partyTypeId = src.getPartyTypeId();
        this.ruleSystem = src.getRuleSystem();
        this.rulPackage = src.getRulPackage();
    }

    /**
     * Vlastní ID.
     *
     * @return id
     */
    public Integer getApTypeId() {
        return apTypeId;
    }

    /**
     * Vlastní ID.
     *
     * @param apTypeId
     *            id
     */
    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
    }

    /**
     * Kód typu.
     *
     * @return kód typu
     */
    public String getCode() {
        return code;
    }

    /**
     * Kód typu.
     *
     * @param code
     *            kód typu
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Název typu.
     *
     * @return název typu
     */
    public String getName() {
        return name;
    }

    /**
     * Název typu.
     *
     * @param name
     *            název typu
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o
     * "nadtyp".
     *
     * @return Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná
     *         jen o "nadtyp".
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o
     * "nadtyp".
     *
     * @param addRecord
     */
    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    public ApType getParentApType() {
        return parentApType;
    }

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     *
     * @return Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    public void setParentApType(final ApType parentApType) {
        this.parentApType = parentApType;
        this.parentApTypeId = parentApType != null ? parentApType.getApTypeId() : null;
    }

    public Integer getParentApTypeId() {
        return parentApTypeId;
    }

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého
     * typu.
     *
     * @return Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem
     *         a jakého typu.
     */
    public ParPartyType getPartyType() {
        return partyType;
    }

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého
     * typu.
     *
     * @param partyType
     */
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
        this.partyTypeId = partyType != null ? partyType.getPartyTypeId() : null;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public ApRuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public void setRuleSystem(final ApRuleSystem ruleSystem) {
        this.ruleSystem = ruleSystem;
    }
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public String toString() {
        return "ApType pk=" + apTypeId;
    }

    public static ApType makeCopy(ApType src) {
        return new ApType(src);
    }
}
