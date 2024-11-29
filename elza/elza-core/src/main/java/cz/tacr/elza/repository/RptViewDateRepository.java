package cz.tacr.elza.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.RptViewDate;

public interface RptViewDateRepository extends JpaRepository<RptViewDate, LocalDate> {

	@Query("SELECT v FROM rpt_view_date v WHERE v.dateId = ?1")
	Optional<RptViewDate> findById(LocalDate dateId);

	RptViewDate findTop1ByOrderByDateIdDesc();
}
