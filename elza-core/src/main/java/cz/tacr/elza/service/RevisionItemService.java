package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApRevItem;
import cz.tacr.elza.domain.ApRevPart;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ApRevItemRepository;
import cz.tacr.elza.repository.DataRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RevisionItemService {

    private final static Logger logger = LoggerFactory.getLogger(RevisionItemService.class);

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ApRevItemRepository revItemRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private EntityManager em;


    public List<ApRevItem> findByParts(List<ApRevPart> parts) {
        return revItemRepository.findByParts(parts);
    }

    public void deleteRevisionItems(List<ApRevItem> items, ApChange change) {
        if (CollectionUtils.isNotEmpty(items)) {
            for (ApRevItem item : items) {
                item.setDeleteChange(change);
            }
            revItemRepository.saveAll(items);
        }
    }

    public List<ApRevItem> createItems(final ApRevPart part,
                                    final List<ApItemVO> createItems,
                                    final ApChange change) {
        if (createItems.isEmpty()) {
            return Collections.emptyList();
        }

        // Map for position counting
        Map<Integer, List<ApRevItem>> typeIdItemsMap = new HashMap<>();

        StaticDataProvider sdp = staticDataService.getData();
        List<ArrData> dataToSave = new ArrayList<>(createItems.size());
        List<ApRevItem> itemsCreated = new ArrayList<>();

        for (ApItemVO createItem : createItems) {
            ItemType itemType = sdp.getItemTypeById(createItem.getTypeId());
            RulItemSpec itemSpec = accessPointItemService.getItemSpecification(itemType, createItem);
            List<ApRevItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

            //todo check if position works
            Integer positionWant = createItem.getPosition();
            int position = nextPosition(existsItems);

            if (positionWant != null) {
                Validate.isTrue(positionWant > 0);
                if (position > positionWant) {

                    List<ApRevItem> itemsToShift = findItemsGE(existsItems, positionWant);
                    List<ApRevItem> newItems = shiftItems(itemsToShift, 1, change);
                    existsItems.addAll(newItems);

                    position = positionWant;
                }
            }

            ArrData data = createItem.createDataEntity(em);
            dataToSave.add(data);

            itemService.checkItemLengthLimit(itemType.getEntity(), data);

            //todo nextItemObjectId???
            ApRevItem itemCreated = createItem(part, data, itemType.getEntity(), itemSpec, change, accessPointItemService.nextItemObjectId(), position);
            itemsCreated.add(itemCreated);

            existsItems.add(itemCreated);

        }

        dataRepository.saveAll(dataToSave);
        revItemRepository.saveAll(itemsCreated);
        logger.debug("Items created, ItemIds: {}", itemsCreated.stream().map(ApRevItem::getItemId).collect(Collectors.toList()));
        return itemsCreated;
    }

    private ApRevItem createItem(final ApRevPart part,
                                final ArrData data,
                                final RulItemType it, final RulItemSpec is, final ApChange c,
                                final int objectId, final int position) {
        ApRevItem item = new ApRevItem();
        item.setData(data);
        item.setItemType(it);
        item.setItemSpec(is);
        item.setCreateChange(c);
        item.setObjectId(objectId);
        item.setPosition(position);
        item.setPart(part);
        return item;
    }

    private int nextPosition(final List<ApRevItem> existsItems) {
        if (existsItems.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ApRevItem existsItem : existsItems) {
            if (existsItem.getDeleteChange() == null) {
                if (existsItem.getPosition() >= position) {
                    position = existsItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    private List<ApRevItem> findItemsGE(final List<ApRevItem> items, final int position) {
        List<ApRevItem> result = new ArrayList<>();
        for (ApRevItem item : items) {
            if (item.getDeleteChange() == null) {
                if (item.getPosition() >= position) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<ApRevItem> shiftItems(final List<ApRevItem> items, final int diff, final ApChange change) {
        List<ApRevItem> newItems = new ArrayList<>();
        for (ApRevItem item : items) {
            if (item.getItemId() == null) {
                item.setPosition(item.getPosition() + diff);
            } else {
                ApRevItem newItem = item.copy();
                newItem.setCreateChange(change);
                newItem.setPosition(item.getPosition() + diff);
                newItems.add(newItem);

                item.setDeleteChange(change);
            }
        }
        return newItems;
    }
}
