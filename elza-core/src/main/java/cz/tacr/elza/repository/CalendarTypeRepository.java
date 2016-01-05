package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrCalendarType;


/**
 * @author Martin Šlapa
 * @since 20.10.2015
 */
@Repository
public interface CalendarTypeRepository extends JpaRepository<ArrCalendarType, Integer> {

    ArrCalendarType findByCode(String calendarTypeCode);

}
