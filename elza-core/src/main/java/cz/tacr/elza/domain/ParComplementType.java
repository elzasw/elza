package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


/**
 * Číselník typů doplňků jmen osob.
 */
@Entity(name = "par_complement_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParComplementType implements cz.tacr.elza.api.ParComplementType {

    @Id
    @GeneratedValue
    private Integer complementTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer viewOrder;


    @Override
    public Integer getComplementTypeId() {
        return complementTypeId;
    }

    @Override
    public void setComplementTypeId(Integer complementTypeId) {
        this.complementTypeId = complementTypeId;
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
    public Integer getViewOrder() {
        return viewOrder;
    }

    @Override
    public void setViewOrder(Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParComplementType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParComplementType other = (cz.tacr.elza.api.ParComplementType) obj;

        return new EqualsBuilder().append(complementTypeId, other.getComplementTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(complementTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParComplementType pk=" + complementTypeId;
    }

}
