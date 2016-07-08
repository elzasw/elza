package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrItemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Repository pro {@link ArrItemSettings}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Repository
public interface ItemSettingsRepository extends JpaRepository<ArrItemSettings, Integer> {

}
