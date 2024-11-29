package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.RptRequiredView;

public interface RptRequiredViewRepository extends JpaRepository<RptRequiredView, Integer> {

	@Query("SELECT v FROM rpt_required_view v WHERE v.report.code = ?1")
	List<RptRequiredView> findByReportCode(String code);
}
