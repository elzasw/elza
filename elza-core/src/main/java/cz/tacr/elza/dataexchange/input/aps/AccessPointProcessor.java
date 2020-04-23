package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parts.PartProcessor;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.dbchangelog.DbChangeSet20200331164200;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.schema.v2.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        ApPart entity;
        try {
            entity = new ApPart();
        } catch (Exception e) {
            throw new SystemException("Failed to intialized no arg constructor, entity: " + ApPart.class, e);
        }
        entity.setPartType(type);
        entity.setCreateChange(context.getCreateChange());
        entity.setState(ApStateEnum.OK);
        return entity;
    }
}
