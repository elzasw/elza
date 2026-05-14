package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrExport;

public interface ExportRepository extends ElzaJpaRepository<ArrExport, Integer> {

	boolean existsByExportTypeExportTypeId(Integer id);

}
