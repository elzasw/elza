package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.tacr.elza.domain.interfaces.Versionable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Abstraktní předek pro prvky s verzí.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
@MappedSuperclass
public class AbstractVersionableEntity implements Versionable {

    @Version
    @JsonIgnore
    private Integer version;

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }
}
