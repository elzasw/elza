package cz.tacr.elza.dataexchange.output.writer;

import java.util.Collection;

import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;

public interface ApInfo extends BaseApInfo {

    Collection<ApName> getNames();

    ApDescription getDesc();
}
