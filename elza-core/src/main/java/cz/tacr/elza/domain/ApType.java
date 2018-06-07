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
@Entity(name = "ap_type")
@Table
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApType {

    /* Konstanty pro vazby a fieldy. */
    public static final String ID = "apTypeId";

    @Id
    @GeneratedValue
    private Integer apTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = true)
    private Boolean addRecord;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "parentApTypeId", nullable = true)
    private ApType parentApType;

    @Column(insertable = false, updatable = false)
    private Integer parentApTypeId;

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
    public Integer getApTypeId() {
        return apTypeId;
    }

    /**
     * Vlastní ID.
     * @param apTypeId id
     */
    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
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
     */
    public ApType getParentApType() {
        return parentApType;
    }

    /**
     * Odkaz na sebe sama (hierarchie typů rejstříků).
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
        if (!(obj instanceof ApType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ApType other = (ApType) obj;

        return new EqualsBuilder().append(apTypeId, other.getApTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(apTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApType pk=" + apTypeId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
