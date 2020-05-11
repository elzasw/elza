package cz.tacr.elza.dataexchange.output.aps;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.dataexchange.output.writer.PartApInfo;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.RulPartType;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

public class PartDispatcher extends NestedLoadDispatcher<ApPart> {

    private final PartApInfo info;

    private final StaticDataProvider staticData;

    private final List<ApPart> parts = new ArrayList<>();

    public PartDispatcher(PartApInfo info, LoadDispatcher<?> apInfoDispatcher, StaticDataProvider staticData) {
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
        parts.add(result);
    }

    @Override
    protected void onCompleted() {
        info.setParts(parts);
    }
}
