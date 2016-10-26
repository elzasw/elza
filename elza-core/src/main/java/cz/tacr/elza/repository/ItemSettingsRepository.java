package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Repository pro {@link ArrItemSettings}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
public interface ItemSettingsRepository extends JpaRepository<ArrItemSettings, Integer> {

    List<ArrItemSettings> findByOutputDefinition(ArrOutputDefinition outputDefinition);

    ArrItemSettings findOneByOutputDefinitionAndItemType(ArrOutputDefinition outputDefinition, RulItemType itemType);

    void deleteByOutputDefinition(ArrOutputDefinition outputDefinition);
}
