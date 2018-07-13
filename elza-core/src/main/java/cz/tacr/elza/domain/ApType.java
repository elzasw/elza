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
    @JoinColumn(name = "parentApTypeId", nullable = true)
    private ApType parentApType;

    @Column(insertable = false, updatable = false)
    private Integer parentApTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = true)
    private ParPartyType partyType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

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
}
