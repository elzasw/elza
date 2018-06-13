package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.RulItemType;


/**
 * Repository pro {@link ArrItemSettings}
 *
 * @since 27.06.2016
 */
public interface ItemSettingsRepository extends JpaRepository<ArrItemSettings, Integer> {

    List<ArrItemSettings> findByOutputDefinition(ArrOutputDefinition outputDefinition);

    ArrItemSettings findOneByOutputDefinitionAndItemType(ArrOutputDefinition outputDefinition, RulItemType itemType);

    // void deleteByOutputDefinition(ArrOutputDefinition outputDefinition);

    void deleteByOutputDefinitionFund(ArrFund fund);
}
