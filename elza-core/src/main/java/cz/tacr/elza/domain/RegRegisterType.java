package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Číselník typů rejstříků.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_register_type")
@Table
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRegisterType {

    /* Konstanty pro vazby a fieldy. */
    public static final String ID = "registerTypeId";

    @Id
    @GeneratedValue
    private Integer registerTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = true)
    private Boolean hierarchical;

    @Column(nullable = true)
    private Boolean addRecord;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "parentRegisterTypeId", nullable = true)
    private RegRegisterType parentRegisterType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = true)
    private ParPartyType partyType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    /**
     * Vlastní ID.
     * @return  id
     */
    public Integer getRegisterTypeId() {
        return registerTypeId;
    }

    /**
     * Vlastní ID.
     * @param registerTypeId id
     */
    public void setRegisterTypeId(final Integer registerTypeId) {
        this.registerTypeId = registerTypeId;
    }

    /**
     * Kód typu.
     * @return kód typu
     */
    public String getCode() {
        return code;
    }

    /**
     * Kód typu.
     * @param code kód typu
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Název typu.
     * @return název typu
     */
    public String getName() {
        return name;
    }

    /**
     * Název typu.
     * @param name název typu
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     * @param hierarchical
     */
    public Boolean getHierarchical() {
        return hierarchical;
    }

    /**
     * Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     * @return Příznak, zda rejstříková hesla tohoto typu rejstříku tvoří hierarchii.
     */
    public void setHierarchical(final Boolean hierarchical) {
            this.hierarchical = hierarchical;
    }

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     * @return Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     */
    public Boolean getAddRecord() {
        return addRecord;
    }

    /**
     * Příznak, zda může daný typ rejstříku obsahovat hesla nebo se jedná jen o "nadtyp".
     * @param addRecord
     */
    public void setAddRecord(final Boolean addRecord) {
        this.addRecord = addRecord;
    }

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     * @param parentRegisterType
     */
    public RegRegisterType getParentRegisterType() {
        return parentRegisterType;
    }

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
     * @return Odkaz na sebe sama (hierarchie typů rejstříků).
     */
    public void setParentRegisterType(final RegRegisterType parentRegisterType) {
        this.parentRegisterType = parentRegisterType;
    }

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     * @return Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     */
    public ParPartyType getPartyType() {
        return partyType;
    }

    /**
     * Určení, zda hesla daného typu mohou být "abstraktní" osobou/původcem a jakého typu.
     * @param partyType
     */
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RegRegisterType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RegRegisterType other = (RegRegisterType) obj;

        return new EqualsBuilder().append(registerTypeId, other.getRegisterTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(registerTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "RegRegisterType pk=" + registerTypeId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
