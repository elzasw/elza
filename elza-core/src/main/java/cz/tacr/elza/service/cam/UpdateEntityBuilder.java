package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.service.GroovyService;
import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.DeletePartXml;
import cz.tacr.cam.schema.cam.EntityIdXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.PartTypeXml;
import cz.tacr.cam.schema.cam.SetRecordStateXml;
import cz.tacr.cam.schema.cam.UpdateEntityXml;
import cz.tacr.cam.schema.cam.UpdateItemsXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.service.ExternalSystemService;

public class UpdateEntityBuilder extends CamXmlBuilder {

    final private ApBindingState bindingState;
    final ApBindingItemRepository bindingItemRepository;
    final ApState apState;
    final private ExternalSystemService externalSystemService;
    final private ApBinding binding;

    List<ApBindingItem> bindingItems = new ArrayList<>();

    public UpdateEntityBuilder(ExternalSystemService externalSystemService,
                               ApBindingItemRepository bindingItemRepository,
                               StaticDataProvider sdp,
                               final ApState state,
                               ApBindingState bindingState,
                               GroovyService groovyService) {
        super(sdp, bindingState.getAccessPoint(), new BindingRecordRefHandler(bindingState.getBinding()), groovyService);
        this.bindingItemRepository = bindingItemRepository;
        this.bindingState = bindingState;
        this.apState = state;
        this.externalSystemService = externalSystemService;
        this.binding = bindingState.getBinding();
    }

    protected UpdateItemsXml createUpdateItems(ApBindingItem changedPart, List<ApBindingItem> changedItems, String externalSystemTypeCode) {
        UpdateItemsXml updateItemsXml = new UpdateItemsXml();
        updateItemsXml.setPid(new UuidXml(changedPart.getValue()));
        updateItemsXml.setT(PartTypeXml.fromValue(changedPart.getPart().getPartType().getCode()));

        for (ApBindingItem bindingItem : changedItems) {
            Object i = CamXmlFactory.createItem(sdp, bindingItem.getItem(), bindingItem.getValue(), entityRefHandler, groovyService, externalSystemTypeCode);
            if (i != null) {
                updateItemsXml.getItems().add(i);
            }
        }
        return updateItemsXml;
    }

    private List<Object> createUpdateEntityChanges(Collection<ApPart> partList,
                                                   Map<Integer, List<ApItem>> itemMap,
                                                   List<ApBindingItem> bindingParts,
                                                   String externalSystemTypeCode) {
        List<Object> changes = new ArrayList<>();
        List<ApBindingItem> deletedParts = new ArrayList<>();
        List<ApBindingItem> changedParts = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(bindingParts)) {
            for (ApBindingItem bindingPart : bindingParts) {
                if (bindingPart.getPart().getDeleteChange() != null) {
                    deletedParts.add(bindingPart);
                } else if (bindingPart.getPart().getCreateChange().getChangeId() > bindingState.getSyncChange()
                        .getChangeId()) {
                    changedParts.add(bindingPart);
                }
                partList.remove(bindingPart.getPart());
            }
        }

        changes.addAll(createPartList(partList, itemMap, externalSystemTypeCode));
        changes.addAll(createDeletePartList(deletedParts));
        changes.addAll(createChangedPartList(changedParts, itemMap, externalSystemTypeCode));

        return changes;
    }

    private List<Object> createChangedPartList(List<ApBindingItem> changedParts,
                                               Map<Integer, List<ApItem>> itemMap,
                                               String externalSystemTypeCode) {
        List<Object> changes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(changedParts)) {
            Map<Integer, List<ApBindingItem>> bindingItemMap = bindingItemRepository.findItemsByBinding(bindingState
                    .getBinding()).stream()
                    .collect(Collectors.groupingBy(i -> i.getItem().getPartId()));
            for (ApBindingItem changedPart : changedParts) {
                Collection<ApItem> itemList = itemMap.getOrDefault(changedPart.getPart().getPartId(), Collections
                        .emptyList());
                List<ApBindingItem> bindingItemList = bindingItemMap.getOrDefault(changedPart.getPart().getPartId(),
                                                                                  new ArrayList<>());
                changes.addAll(createChangedPartList(changedPart, itemList, bindingItemList, externalSystemTypeCode));
            }
        }
        return changes;
    }

    private List<Object> createChangedPartList(ApBindingItem changedPart,
                                               Collection<ApItem> itemList,
                                               List<ApBindingItem> bindingItemList,
                                               String externalSystemTypeCode) {
        List<Object> changes = new ArrayList<>();
        List<ApBindingItem> changedItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                if (bindingItem.getItem().getCreateChange().getChangeId() > bindingState.getSyncChange()
                        .getChangeId()) {
                    changedItems.add(bindingItem);
                    itemList.remove(bindingItem.getItem());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(itemList)) {
            changes.add(createNewItems(changedPart, itemList, externalSystemTypeCode));
        }
        if (CollectionUtils.isNotEmpty(changedItems)) {
            changes.add(createUpdateItems(changedPart, changedItems, externalSystemTypeCode));
        }

        return changes;
    }

    private List<DeletePartXml> createDeletePartList(List<ApBindingItem> deletedParts) {
        List<DeletePartXml> deletePartXmls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(deletedParts)) {
            for (ApBindingItem deletedPart : deletedParts) {
                deletePartXmls.add(new DeletePartXml(new UuidXml(deletedPart.getValue()),
                        // TODO: improve with sdp
                        PartTypeXml.fromValue(
                                              deletedPart.getPart().getPartType().getCode())));
            }
        }
        return deletePartXmls;
    }


    private BatchEntityRecordRevXml createBatchEntityRecordRef() {
        BatchEntityRecordRevXml batchEntityRecordRevXml = new BatchEntityRecordRevXml();
        batchEntityRecordRevXml.setEid(new EntityIdXml(Long.parseLong(bindingState.getBinding().getValue())));
        batchEntityRecordRevXml.setRev(new UuidXml(bindingState.getExtRevision()));
        batchEntityRecordRevXml.setLid("LID" + UUID.randomUUID().toString());
        return batchEntityRecordRevXml;
    }

    public void build(final List<Object> trgList,
                      final EntityXml entityXml, List<ApPart> partList,
                      Map<Integer, List<ApItem>> itemMap,
                      List<ApBindingItem> bindingParts,
                      String externalSystemTypeCode) {
        
        List<Object> changes = createUpdateEntityChanges(partList, itemMap, bindingParts, externalSystemTypeCode);

        if (CollectionUtils.isNotEmpty(changes)) {
            for (Object change : changes) {
                addChange(trgList, change);
            }
        }

        // TODO: this is broken, probably meant for aptype change
        if (!entityXml.getEnt().getValue().equals(apState.getApType().getCode())) {
            addChange(trgList, new CodeXml(apState.getApType().getCode()));
        }

        if (apState.getStateApproval() == StateApproval.APPROVED && entityXml
                .getEns() != EntityRecordStateXml.ERS_APPROVED) {
            addChange(trgList, new SetRecordStateXml(EntityRecordStateXml.ERS_APPROVED, null));
        }

        //TODO dodělat změnu preferovaného partu
        ApBindingItem preferPart = CamUtils.findBindingItemById(bindingParts,
                                                                accessPoint.getPreferredPart().getPartId());
        
    }

    private void addChange(List<Object> trgList, Object change) {
        UpdateEntityXml result = new UpdateEntityXml(createBatchEntityRecordRef(), change);
        trgList.add(result);

    }

    @Override
    protected void onItemCreated(ApItem item, String uuid) {
        externalSystemService.createApBindingItem(binding, uuid, null, item);
    }

    @Override
    protected void onPartCreated(ApPart apPart, String uuid) {
        externalSystemService.createApBindingItem(binding, uuid, apPart, null);
    }
}
