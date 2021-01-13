package cz.tacr.elza.dataexchange.input.storage;

import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.repository.ApItemRepository;
import org.hibernate.Session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ApItemStorage extends EntityStorage<ItemWrapper> {

    private final ApItemRepository itemRepository;

    public ApItemStorage(Session session, StoredEntityCallback persistentEntityListener, ImportInitHelper initHelper) {
        super(session, persistentEntityListener);
        this.itemRepository = initHelper.getApItemRepository();
    }

    @Override
    protected void mergeEntities(Collection<ItemWrapper> pws) {
        prepareCurrentEntities(pws);
        super.mergeEntities(pws);
    }

    private void prepareCurrentEntities(Collection<ItemWrapper> iws) {
        //TODO : gotzy - možná domimplementovat s vazbou ApPart
        Map<Integer, ItemWrapper> apIdMap = new HashMap<>(iws.size());
        // init apId -> party map
       /* for (ItemWrapper iw : iws) {
            Integer partId = iw.getPartIdHolder().getEntityId();
            Validate.notNull(partId);
            apIdMap.put(apId, pw);
        }
        // find current parts by apIds

        if(apIdMap.size() > 0) {
            List<ApPart> currParts = itemRepository.findPartsByAccessPointIdIn(apIdMap.keySet());
            if (currParts.size() != apIdMap.size()) {
                throw new IllegalStateException(
                        "Not all AP parts found, apIds=" + StringUtils.join(apIdMap.keySet(), ','));
            }
            // update wrapped entity by existing part
            for (ApPart part : currParts) {
                PartWrapper wrapper = apIdMap.get(part.getApAccessPointId());
                ApPart entity = wrapper.getEntity();
                entity.setPartId(part.getPartId());
            }*/
        }
}
