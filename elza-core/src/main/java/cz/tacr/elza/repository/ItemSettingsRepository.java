package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItemSettings;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulItemType;


/**
 * Repository pro {@link ArrItemSettings}
 *
 * @since 27.06.2016
 */
public interface ItemSettingsRepository extends JpaRepository<ArrItemSettings, Integer> {

    List<ArrItemSettings> findByOutput(ArrOutput output);

    ArrItemSettings findOneByOutputAndItemType(ArrOutput output, RulItemType itemType);

    // void deleteByOutput(ArrOutput output);

    void deleteByOutputFund(ArrFund fund);
}
