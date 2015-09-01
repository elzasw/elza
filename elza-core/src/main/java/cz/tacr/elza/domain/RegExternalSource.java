package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.req.ax.IdObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;


/**
 * Číselník externích zdrojů rejstříkových hesel.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_external_source")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegExternalSource implements IdObject<Integer>, cz.tacr.elza.api.RegExternalSource {

    @Id
    @GeneratedValue
    private Integer externalSourceId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;


    @Override
    public Integer getExternalSourceId() {
        return externalSourceId;
    }

    @Override
    public void setExternalSourceId(final Integer externalSourceId) {
        this.externalSourceId = externalSourceId;
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
    @JsonIgnore
    public Integer getId() {
        return externalSourceId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RegExternalSource)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RegExternalSource other = (RegExternalSource) obj;

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
