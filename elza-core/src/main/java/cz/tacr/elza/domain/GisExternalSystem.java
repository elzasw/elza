package cz.tacr.elza.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.GisSystemType;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Externí systémy pro mapové podklady.
 */
@Entity(name = "gis_external_system")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class GisExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private GisSystemType type;

    public GisSystemType getType() {
        return type;
    }

    public void setType(GisSystemType type) {
        this.type = type;
    }

}
