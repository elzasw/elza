package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Externí systémy pro rejstříky/osoby.
 */
@Entity(name = "ap_external_system")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ApExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ApExternalSystemType type;

    public ApExternalSystemType getType() {
        return type;
    }

    public void setType(ApExternalSystemType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ApExternalSystem pk=" + getExternalSystemId();
    }
}
