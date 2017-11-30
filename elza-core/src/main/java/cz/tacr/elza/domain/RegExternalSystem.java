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
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table
public class RegExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column
    private RegExternalSystemType type;

    public RegExternalSystemType getType() {
        return type;
    }

    public void setType(final RegExternalSystemType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "RegExternalSystem pk=" + getExternalSystemId();
    }
}
