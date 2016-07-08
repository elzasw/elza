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
 * Typ formy jména.
 */
@Entity(name = "par_party_name_form_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParPartyNameFormType implements cz.tacr.elza.api.ParPartyNameFormType {

    @Id
    @GeneratedValue
    private Integer nameFormTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250)
    private String name;


    @Override
    public Integer getNameFormTypeId() {
        return nameFormTypeId;
    }

    @Override
    public void setNameFormTypeId(final Integer nameFormTypeId) {
        this.nameFormTypeId = nameFormTypeId;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParPartyNameFormType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParPartyNameFormType other = (cz.tacr.elza.api.ParPartyNameFormType) obj;

        return new EqualsBuilder().append(nameFormTypeId, other.getNameFormTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nameFormTypeId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParPartyNameFormType pk=" + nameFormTypeId;
    }

}
