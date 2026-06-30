package cz.tacr.elza.repository;

import java.util.Optional;

import cz.tacr.elza.domain.ArrExportType;
import jakarta.validation.constraints.NotNull;

public interface ExportTypeRepository extends ElzaJpaRepository<ArrExportType, Integer> {

	Optional<ArrExportType> findByCode(@NotNull String code);

}
