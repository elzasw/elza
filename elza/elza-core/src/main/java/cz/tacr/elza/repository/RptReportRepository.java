package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RptReport;

@Repository
public interface RptReportRepository extends JpaRepository<RptReport, Integer> {

}