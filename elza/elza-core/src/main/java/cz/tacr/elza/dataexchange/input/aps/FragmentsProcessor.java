package cz.tacr.elza.dataexchange.input.aps;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FragmentsProcessor extends AccessPointEntryProcessor {

    public FragmentsProcessor(ImportContext context) {
        super(context, false);
    }

    @Override
    public void process(Object item) {
        proccessFragments((Fragments) item);
    }

    protected void proccessFragments(Fragments fragments) {
        Collection<AccessPointInfo> apInfoCol = context.getAllAccessPointInfo();
        List<ApPart> parts = createParts(fragments);
       // info = context.addAccessPoint(entity.accessPoint, entry.getId(), entity.state, eids, parts);
    }

    private List<ApPart> createParts(Fragments fragments) {
        if(fragments.getFrg().isEmpty()) {
            return null;
        }

        List<ApPart> entities = new ArrayList<>(fragments.getFrg().size());
        for(Fragment fragment : fragments.getFrg()) {
            if (StringUtils.isEmpty(fragment.getT())) {
                throw new DEImportException("Fragment id type is not set, fragmentId=" + fragment.getFid());
            }
            RulPartType partType = context.getRulPartType(fragment.getT());
            if(partType == null) {
                throw new DEImportException("Part type not found, fragmentId=" + fragment.getFid() + ", " + fragment.getT());
            }
            //create Ap Part
            ApPart entity = new ApPart();
            entity.setCreateChange(context.getCreateChange());
            entity.setPartType(partType);
            entity.setState(ApStateEnum.OK);
            //TODO : gotzy - vyřešit parent part
            entities.add(entity);
        }
        return entities;
    }
}

