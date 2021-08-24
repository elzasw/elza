package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.DeleteItemsXml;
import cz.tacr.cam.schema.cam.DeletePartXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ItemRefXml;
import cz.tacr.cam.schema.cam.NewItemsXml;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.SetRecordStateXml;
import cz.tacr.cam.schema.cam.UpdateEntityXml;
import cz.tacr.cam.schema.cam.UpdateItemsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.service.GroovyService;

public class UpdateEntityBuilder extends CamXmlBuilder {

    final private static Logger logger = LoggerFactory.getLogger(UpdateEntityBuilder.class);

    final private ApBindingState bindingState;
    final ApBindingItemRepository bindingItemRepository;
    final ApState apState;

    private List<ApBindingItem> bindingItems;

    private List<ApBindingItem> bindingParts;

    private List<Object> trgList = new ArrayList<>();

    static class BindingPartInfo {
        final ApBindingItem bindingPart;

        List<ApBindingItem> deletedItems = new ArrayList<>();
        List<ApBindingItem> activeItems = new ArrayList<>();

        Map<Integer, ApBindingItem> bindingItemMap = new HashMap<>();

        public BindingPartInfo(final ApBindingItem bindingPart) {
            this.bindingPart = bindingPart;
        }

        public void addItem(ApBindingItem bindingItem) {
            if (bindingItem.getDeleteChangeId() != null) {
                deletedItems.add(bindingItem);
            } else {
                activeItems.add(bindingItem);
            }
            bindingItemMap.put(bindingItem.getItemId(), bindingItem);
        }

        public List<ApBindingItem> getActiveBindedItems() {
            return activeItems;
        }

        public List<ApBindingItem> getDeletedBindedItems() {
            return deletedItems;
        }

        public boolean isBinded(ApItem i) {
            ApBindingItem bindedItem = bindingItemMap.get(i.getItemId());
            return bindedItem != null;
        }
    };

    /**
     * Map between partId and binded items
     */
    private Map<Integer, BindingPartInfo> bindingMap = new HashMap<>();

    public UpdateEntityBuilder(ApBindingItemRepository bindingItemRepository,
                               StaticDataProvider sdp,
                               final ApState state,
                               ApBindingState bindingState,
                               GroovyService groovyService,
                               AccessPointDataService apDataService,
                               ApScope scope,
                               ApExternalSystem externalSystem) {
        super(sdp, bindingState.getAccessPoint(), new BindingRecordRefHandler(bindingState.getBinding()), groovyService,
                apDataService, scope, externalSystem.getType());
        this.bindingItemRepository = bindingItemRepository;
        this.bindingState = bindingState;
        this.apState = state;
    }

    protected UpdateItemsXml createUpdateItems(ApBindingItem changedPart, List<ApBindingItem> changedItems) {
        UpdateItemsXml updateItemsXml = new UpdateItemsXml();
        updateItemsXml.setPid(new UuidXml(changedPart.getValue()));
        updateItemsXml.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        for (ApBindingItem bindingItem : changedItems) {
            Object i = CamXmlFactory.createItem(sdp, bindingItem.getItem(), bindingItem.getValue(), entityRefHandler,
                                                groovyService, apDataService, externalSystemType.toString(), scope);
            if (i != null) {
                updateItemsXml.getItems().add(i);
            }
        }
        return updateItemsXml;
    }

    /**
     * Prepare item changes
     * 
     * Prerequisity: all items are read
     * 
     * @param partList
     * @param itemMap
     * @param externalSystemTypeCode
     * @return
     */
    private void createUpdateEntityChanges(Collection<ApPart> partList,
                                           Map<Integer, List<ApItem>> itemMap) {
        List<ApBindingItem> deletedParts = new ArrayList<>();
        List<ApBindingItem> partsWithPossibleChange = new ArrayList<>();
        List<ApPart> newParts = new ArrayList<>();

        for (ApBindingItem bindingPart : bindingParts) {
            ApPart part = bindingPart.getPart();
            if (part.getDeleteChangeId() != null) {
                deletedParts.add(bindingPart);
            } else {
                partsWithPossibleChange.add(bindingPart);
            }
        }
        // detect new parts
        for (ApPart part : partList) {
            BindingPartInfo partBinding = bindingMap.get(part.getPartId());
            if (partBinding == null) {
                newParts.add(part);
            }
        }

        List<PartXml> parts = createNewParts(newParts, itemMap);
        for (PartXml part : parts) {
            addUpdate(part);
        }

        createDeletePartList(deletedParts);

        createChangedPartList(partsWithPossibleChange, itemMap);
    }

    /**
     * Create list of modified parts and items in these parts
     * 
     * @param changedParts
     * @param itemMap
     * @param externalSystemTypeCode
     * @return
     */
    private void createChangedPartList(List<ApBindingItem> changedParts,
                                       Map<Integer, List<ApItem>> itemMap) {
        if (CollectionUtils.isEmpty(changedParts)) {
            return;
        }

        // sort items as created, update, deleted
        for (ApBindingItem changedPart : changedParts) {
            Integer partId =  changedPart.getPartId();
            BindingPartInfo bi = this.bindingMap.get(partId);
            if(bi==null) {
                logger.error("Failed to get binging info, partId: {}", partId);
                throw new BusinessException("Failed to get binging info", 
                                            BaseCode.DB_INTEGRITY_PROBLEM);
            }
            List<ApItem> itemList = itemMap.getOrDefault(partId, Collections.emptyList());

            createChangedPartList(changedPart, itemList, bi);
        }
    }

    private void createChangedPartList(ApBindingItem changedPart,
                                               List<ApItem> itemList,
                                       BindingPartInfo bi) {
        /*
        List<Object> changes = new ArrayList<>();
        List<ApBindingItem> changedItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bi)) {
            for (ApBindingItem bindingItem : bi) {
                if (bindingItem.getItem().getCreateChangeId() > bindingState.getSyncChangeId()) {
                    changedItems.add(bindingItem);
                    itemList.remove(bindingItem.getItem());
                }
            }
        }
        */

        itemList = filterOutItemsWithoutExtSysMapping(itemList);
        if (CollectionUtils.isNotEmpty(itemList)) {
            // filter bindined items
            List<ApItem> filteredList = itemList.stream().filter(i -> !bi.isBinded(i))
                    .collect(Collectors.toList());
            if (filteredList.size() > 0) {
                NewItemsXml newItems = createNewItems(changedPart, filteredList);
                // some new items does not have to be created
                if (newItems != null) {
                    addUpdate(newItems);
                }
            }
        }

        List<ApBindingItem> deletedItems = bi.getDeletedBindedItems();
        if (CollectionUtils.isNotEmpty(deletedItems)) {
            DeleteItemsXml deleteItems = createDeleteItems(changedPart, deletedItems);
            if (deleteItems != null) {
                addUpdate(deleteItems);
            }
        }

        List<ApBindingItem> activeItems = bi.getActiveBindedItems();
        if (CollectionUtils.isNotEmpty(activeItems)) {
            // filter bindined items
            List<ApBindingItem> filteredList = activeItems.stream().filter(i -> i.getItem()
                    .getCreateChangeId() > bindingState.getSyncChangeId())
                    .collect(Collectors.toList());
            if (filteredList.size() > 0) {
                UpdateItemsXml updateItems = createUpdateItems(changedPart, activeItems);
                if (updateItems != null) {
                    addUpdate(updateItems);
                }
            }
        }
    }

    private DeleteItemsXml createDeleteItems(ApBindingItem changedPart,
                                             List<ApBindingItem> deletedItems) {
        if (CollectionUtils.isEmpty(deletedItems)) {
            return null;
        }
        DeleteItemsXml ret = new DeleteItemsXml();
        ret.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));
        ret.setPid(new UuidXml(changedPart.getValue()));
        List<ItemRefXml> itemRefs = ret.getList();

        for (ApBindingItem di : deletedItems) {
            ItemRefXml itemRf = new ItemRefXml();
            // TODO: improve with sdp
            itemRf.setT(new CodeXml(di.getItem().getItemType().getCode()));
            itemRf.setUuid(new UuidXml(di.getValue()));
            // TODO: set type and spec
            itemRefs.add(itemRf);
        }
        return ret;
    }

    private void createDeletePartList(List<ApBindingItem> deletedParts) {
        if (CollectionUtils.isEmpty(deletedParts)) {
            return;
        }

        List<DeletePartXml> deletePartXmls = new ArrayList<>(deletedParts.size());
        for (ApBindingItem deletedPart : deletedParts) {
            DeletePartXml dpx = new DeletePartXml(new UuidXml(deletedPart.getValue()),
                    // TODO: improve with sdp
                    PartTypeXml.fromValue(
                                          deletedPart.getPart().getPartType().getCode()));
            addUpdate(dpx);
        }
    }

    private BatchEntityRecordRevXml createBatchEntityRecordRef() {
        BatchEntityRecordRevXml batchEntityRecordRevXml = new BatchEntityRecordRevXml();
        batchEntityRecordRevXml.setEid(new EntityIdXml(Long.parseLong(bindingState.getBinding().getValue())));
        batchEntityRecordRevXml.setRev(new UuidXml(bindingState.getExtRevision()));
        batchEntityRecordRevXml.setLid("LID" + UUID.randomUUID().toString());
        return batchEntityRecordRevXml;
    }

    public List<Object> build(final EntityXml entityXml,
                      List<ApPart> partList,
                              Map<Integer, List<ApItem>> itemMap) {
        // check that list is empty
        Validate.isTrue(trgList.size() == 0);

        // read current bindings
        bindingParts = bindingItemRepository.findPartsForSync(bindingState.getBinding(),
                                                              bindingState.getSyncChangeId());
        for (ApBindingItem bindingPart : bindingParts) {
            BindingPartInfo bi = new BindingPartInfo(bindingPart);
            bindingMap.put(bindingPart.getPartId(), bi);
        }
        bindingItems = bindingItemRepository.findItemsForSync(bindingState.getBinding(),
                                                              bindingState.getSyncChangeId());
        for (ApBindingItem bindingItem : bindingItems) {
            ApItem item = bindingItem.getItem();
            BindingPartInfo bi = bindingMap.get(item.getPartId());
            if (bi == null) {
                logger.error("Inconsistent date, bindingItemId: {}", bindingItem.getBindingItemId());
            }
            bi.addItem(bindingItem);
        }

        createUpdateEntityChanges(partList, itemMap);

        // TODO: this is broken, probably meant for aptype change
        /*if (!entityXml.getEnt().getValue().equals(apState.getApType().getCode())) {
            addChange(trgList, new CodeXml(apState.getApType().getCode()));
        }*/

        if (apState.getStateApproval() == StateApproval.APPROVED &&
                entityXml.getEns() != EntityRecordStateXml.ERS_APPROVED) {
            addUpdate(new SetRecordStateXml(EntityRecordStateXml.ERS_APPROVED, null));
        }

        // změna preferovaného partu
        PartXml prefNameXml = CamUtils.getPrefName(entityXml);
        ApBindingItem preferPart = CamUtils.findBindingItemById(bindingParts,
                                                                accessPoint.getPreferredPartId());
        String prefPartUuid;
        if (preferPart != null) {
            prefPartUuid = preferPart.getValue();
        } else {
            // preferred part is new
            prefPartUuid = partUuids.get(accessPoint.getPreferredPartId());
            Validate.notNull(prefPartUuid);
        }
        if (!Objects.equals(prefPartUuid, prefNameXml.getPid().getValue())) {
            setPrefName(new UuidXml(prefPartUuid));
        }
        
        return trgList;
    }

    public List<Object> getResult() {
        return trgList;
    }

    private void addChangeInternal(Object change) {
        UpdateEntityXml result = new UpdateEntityXml(createBatchEntityRecordRef(), change);
        trgList.add(result);

    }

    private void addUpdate(NewItemsXml change) {
        addChangeInternal(change);
    }

    private void addUpdate(PartXml change) {
        addChangeInternal(change);
    }

    private void addUpdate(DeletePartXml change) {
        addChangeInternal(change);
    }

    private void addUpdate(SetRecordStateXml change) {
        addChangeInternal(change);
    }

    private void addUpdate(DeleteItemsXml change) {
        addChangeInternal(change);
    }

    private void addUpdate(UpdateItemsXml change) {
        addChangeInternal(change);
    }

    private void setPrefName(UuidXml prefName) {
        addChangeInternal(prefName);
    }
}
