package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.schema.v2.AccessPoint;

/**
 * Processing access points. Implementation is not thread-safe.
 */
public class AccessPointProcessor extends FragmentsProcessor {

    public AccessPointProcessor(ImportContext context) {
        super(context);
    }

    @Override
    public void process(Object item) {
        AccessPoint ap = (AccessPoint) item;
        proccessAp(ap);
        apInfo.onProcessed();
    }

    protected ApPart createPart(RulPartType type, AccessPointInfo apInfo) {
        ApPart entity = new ApPart();
        entity.setPartType(type);
        entity.setCreateChange(context.getCreateChange());
        entity.setState(ApStateEnum.OK);
        return entity;
    }
}
