package cz.tacr.elza.domain;

import cz.tacr.elza.api.StorageSystemType;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Externí systémy úložišť.
 */
@Entity(name = "storage_external_system")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class StorageExternalSystem extends SysExternalSystem {

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private StorageSystemType type;

    public StorageSystemType getType() {
        return type;
    }

    public void setType(StorageSystemType type) {
        this.type = type;
    }

}
