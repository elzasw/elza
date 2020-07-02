package cz.tacr.elza.service.arrangement;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;

/**
 * Interface for providing description items for the level
 * 
 * Used when creating new level
 */
public interface DesctItemProvider {

    void provide(ArrLevel level, ArrChange change, ArrFundVersion fundVersion, MultiplItemChangeContext changeContext);

}
