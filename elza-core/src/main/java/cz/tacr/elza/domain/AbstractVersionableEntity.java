package cz.tacr.elza.domain;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import cz.tacr.elza.api.Versionable;

/**
 * Abstraktní předek pro prvky s verzí.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
@MappedSuperclass
public class AbstractVersionableEntity implements Versionable {

    @Version
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
