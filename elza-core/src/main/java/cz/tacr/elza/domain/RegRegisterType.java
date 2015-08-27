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
 * Číselník typů rejstříků.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_register_type")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRegisterType implements IdObject<Integer>, cz.tacr.elza.api.RegRegisterType {

    @Id
    @GeneratedValue
    private Integer registerTypeId;

    @Column(length = 20, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    /* Konstanty pro vazby a fieldy. */
    public static final String ID = "registerTypeId";


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
    @JsonIgnore
    public Integer getId() {
        return registerTypeId;
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

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
