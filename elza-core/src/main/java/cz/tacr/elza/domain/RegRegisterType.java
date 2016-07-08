package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

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


/**
 * Číselník typů rejstříků.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_register_type")
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRegisterType implements  cz.tacr.elza.api.RegRegisterType<RegRegisterType, ParPartyType> {

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

    @Override
    public Integer getRegisterTypeId() {
        return registerTypeId;
    }

    @Override
    public void setRegisterTypeId(final Integer registerTypeId) {
        this.registerTypeId = registerTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
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
    public Boolean getHierarchical() {
        return hierarchical;
    }

    @Override
    public void setHierarchical(Boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

    @Override
    public Boolean getAddRecord() {
        return addRecord;
    }

    @Override
    public void setAddRecord(Boolean addRecord) {
        this.addRecord = addRecord;
    }

    @Override
    public RegRegisterType getParentRegisterType() {
        return parentRegisterType;
    }

    @Override
    public void setParentRegisterType(RegRegisterType parentRegisterType) {
        this.parentRegisterType = parentRegisterType;
    }

    @Override
    public ParPartyType getPartyType() {
        return partyType;
    }

    @Override
    public void setPartyType(ParPartyType partyType) {
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

        cz.tacr.elza.api.RegRegisterType other = (cz.tacr.elza.api.RegRegisterType) obj;

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
}
