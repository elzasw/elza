package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.PartsApInfo;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.RulPartType;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class PartDispatcher extends NestedLoadDispatcher<ApPart> {

    private final PartsApInfo info;

    private final StaticDataProvider staticData;

    public PartDispatcher(PartsApInfo info, LoadDispatcher<?> apInfoDispatcher, StaticDataProvider staticData) {
        super(apInfoDispatcher);
        this.staticData = staticData;
        this.info = info;
    }

    @Override
    public void onLoad(ApPart result) {
        //init Part Type
        RulPartType type = staticData.getPartTypeById(result.getPartTypeId());
        Validate.notNull(type);
        result.setPartType(type);
        info.addPart(result);
    }

    @Override
    protected void onCompleted() {
        // nop
    }
}
