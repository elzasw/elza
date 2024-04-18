package cz.tacr.elza.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Externí systémy digitálního archivu.
 */
@Entity(name = "digital_archive_external_system")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DigitalArchiveExternalSystem extends SysExternalSystem {

}
