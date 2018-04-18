package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Externí systémy pro rejstříky/osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
@Entity(name = "ap_external_system")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table
public class ApExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column
    private cz.tacr.elza.api.ApExternalSystem type;

    public cz.tacr.elza.api.ApExternalSystem getType() {
        return type;
    }

    public void setType(final cz.tacr.elza.api.ApExternalSystem type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ApExternalSystem pk=" + getExternalSystemId();
    }
}
