package cz.tacr.elza.service.arrangement;

import java.util.List;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;

/**
 * Interface for providing description items for the level
 * 
 * Used when creating new level
 */
public interface DesctItemProvider {

    /**
     * 
     * @param level
     * @param change
     * @param fundVersion
     * @param changeContext
     * @return List of created items
     */
    List<ArrDescItem> provide(ArrLevel level, ArrChange change, ArrFundVersion fundVersion,
                           MultipleItemChangeContext changeContext);

}
