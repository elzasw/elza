package cz.tacr.elza.dataexchange.input.storage;

import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.repository.ApPartRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApPartStorage extends EntityStorage<PartWrapper> {

    private final ApPartRepository partRepository;

    public ApPartStorage(Session session, StoredEntityCallback persistEntityListener, ImportInitHelper initHelper) {
        super(session, persistEntityListener);
        this.partRepository = initHelper.getApPartRepository();
    }

    @Override
    protected void mergeEntities(Collection<PartWrapper> pws) {
        prepareCurrentEntities(pws);
        super.mergeEntities(pws);
    }

    private void prepareCurrentEntities(Collection<PartWrapper> pws) {
        Map<Integer, PartWrapper> apIdMap = new HashMap<>(pws.size());
        // init apId -> party map
        for (PartWrapper pw : pws) {
            Integer apId = pw.getPartInfo().getApInfo().getEntityId();
            Validate.notNull(apId);
            apIdMap.put(apId, pw);
        }
        // find current parts by apIds

        if(apIdMap.size() > 0) {
            List<ApPart> currParts = partRepository.findPartsByAccessPointIdIn(apIdMap.keySet());
            if (currParts.size() != apIdMap.size()) {
                throw new IllegalStateException(
                        "Not all AP parts found, apIds=" + StringUtils.join(apIdMap.keySet(), ','));
            }
            // update wrapped entity by existing part
            for (ApPart part : currParts) {
                PartWrapper wrapper = apIdMap.get(part.getAccessPointId());
                ApPart entity = wrapper.getEntity();
                entity.setPartId(part.getPartId());
            }
        }
        // delete all sub entities - items
        // deleteSubEntities(pws);
    }






}
