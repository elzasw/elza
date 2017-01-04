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

import cz.tacr.elza.api.RegExternalSystemType;

/**
 * Externí systémy pro rejstříky/osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
@Entity(name = "reg_external_system")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table
public class RegExternalSystem extends SysExternalSystem implements cz.tacr.elza.api.RegExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column
    private RegExternalSystemType type;

    @Override
    public RegExternalSystemType getType() {
        return type;
    }

    @Override
    public void setType(final RegExternalSystemType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RegExternalSystem pk=" + getExternalSystemId();
    }
}
