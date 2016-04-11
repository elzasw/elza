package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Implementace {@link cz.tacr.elza.api.UsrGroup}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Entity(name = "usr_group")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrGroup implements cz.tacr.elza.api.UsrGroup, Serializable {

    @Id
    @GeneratedValue
    private Integer groupId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 250, nullable = true)
    private String description;

    @Override
    public Integer getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(final Integer groupId) {
        this.groupId = groupId;
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
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }
}
