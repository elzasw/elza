package cz.tacr.elza.service.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.ArrRefTemplate;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ArrRefTemplateRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;

public class RestoreAction {

    private static final Logger log = LoggerFactory.getLogger(RestoreAction.class);

    final StaticDataProvider sdp;

    private Map<Integer, List<ArrDataStructureRef>> restoreStructData;
    private Map<Integer, List<ArrDataRecordRef>> restoreAPRef;
    private Map<Integer, List<ArrDataFileRef>> restoreFileRef;
    private Map<Integer, List<ArrDataUriRef>> restoreUriRefTemplate;
    private Map<Integer, List<ArrDataUriRef>> restoreUriRefNode;
    private Map<Integer, List<ArrDaoLink>> restoreDaoLinks;

    final private StructuredObjectRepository structureDataRepository;

    final private ApAccessPointRepository accessPointRepository;

    final private FundFileRepository fundFileRepository;

    final private DaoRepository daoRepository;

    final private NodeRepository nodeRepository;

    final private ArrRefTemplateRepository refTemplateRepository;

    final private EntityManager em;

    public RestoreAction(final StaticDataProvider sdp,
                         final EntityManager em,
                         final StructuredObjectRepository structureDataRepository,
                         final ApAccessPointRepository accessPointRepository,
                         final FundFileRepository fundFileRepository,
                         final DaoRepository daoRepository,
                         final NodeRepository nodeRepository,
                         final ArrRefTemplateRepository refTemplateRepository) {
        this.sdp = sdp;
        this.em = em;
        this.structureDataRepository = structureDataRepository;
        this.accessPointRepository = accessPointRepository;
        this.fundFileRepository = fundFileRepository;
        this.daoRepository = daoRepository;
        this.nodeRepository = nodeRepository;
        this.refTemplateRepository = refTemplateRepository;
    }

    public void restore(Collection<RestoredNode> cachedNodes) {
        for (RestoredNode restoredNode : cachedNodes) {
            restore(restoredNode);
        }

        prepareStructureData();
        prepareAPRefs();
        prepareFileRefs();
        prepareUriRefs();
        preapareDaoLinks();
    }

    /**
     * Load description item type from rule system provider
     *
     * @param descItem
     */
    private void restoreDescItem(ArrDescItem descItem) {
        // restore createChange
        Objects.requireNonNull(descItem.getCreateChangeId());
        ArrChange createChange = em.getReference(ArrChange.class, descItem.getCreateChangeId());
        descItem.setCreateChange(createChange, descItem.getCreateChangeId());

        // Deleted items cannot be stored in cache
        if (descItem.getDeleteChangeId() != null) {
            throw new SystemException("Item is marked as deleted and placed in cache", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("itemId", descItem.getItemId());
        }

        // restore item type
        Objects.requireNonNull(descItem.getItemTypeId());
        ItemType itemType = sdp.getItemTypeById(descItem.getItemTypeId());
        Objects.requireNonNull(itemType);

        descItem.setItemType(itemType.getEntity());

        Integer itemSpecId = descItem.getItemSpecId();

        // check if specification should be set
        if (itemType.hasSpecifications()) {
            // exception for element without itemSpecId
            if (descItem.getData() == null ^ itemSpecId == null) {
                Validate.notNull(itemSpecId,
                                 "Item without specification or data was found. nodeId=%s, itemTypeId=%s, itemId=%s",
                                 descItem.getNodeId(),
                                 descItem.getItemTypeId(),
                                 descItem.getItemId());
            }
        } else {
            Validate.isTrue(itemSpecId == null, "Item with unexpected specifiaction. nodeId=%s, itemTypeId=%s",
                            descItem.getNodeId(),
                            descItem.getItemTypeId());
        }
        // prepare specification
        if (itemSpecId != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(itemSpecId);
            if (itemSpec == null) {
                log.error("Missing specification itemSpecId: {}, itemId: {}", itemSpecId, descItem.getItemId());
                throw new SystemException("Item is using unknown specification", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("itemId", descItem.getItemId())
                        .set("itemSpecId", itemSpecId);
            }
            descItem.setItemSpec(itemSpec);
        }

        // Restore dataType
        ArrData data = HibernateUtils.unproxy(descItem.getData());
        if (data != null) {
            loadDataType(data, itemType);
        }
    }

    /**
     * Load data type and set it
     *
     * @param data
     * @param itemType
     */
    private void loadDataType(ArrData data, ItemType itemType) {
        DataType dataType = itemType.getDataType();
        // check that item type match

        // check data type id
        if (dataType.getId() != data.getDataTypeId()) {
            throw new BusinessException(
                    "Data inconsistency, dataId = " + data.getDataId(),
                    BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("dataId", data.getDataId())
                            .set("dataTypeId", data.getDataTypeId())
                            .set("itemTypeId", itemType.getItemTypeId())
                            .set("itemTypeCode", itemType.getCode())
                            .set("itemTypeDataTypeId", dataType.getId());
        }
        // check class
        if (!dataType.isValidClass(data.getClass())) {
            throw new BusinessException(
                    "Data inconsistency, dataId = " + data.getDataId(),
                    BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("dataId", data.getDataId())
                            .set("dataTypeId", data.getDataTypeId())
                            .set("dataClass", data.getClass().getName())
                            .set("itemTypeId", itemType.getItemTypeId())
                            .set("itemTypeCode", itemType.getCode())
                            .set("itemTypeDataTypeId", dataType.getId());
        }

        data.setDataType(dataType.getEntity());
    }

    private void restore(RestoredNode restoredNode) {
        ArrNode node = restoredNode.getNode();
        if (CollectionUtils.isNotEmpty(restoredNode.getDescItems())) {
            for (ArrDescItem descItem : restoredNode.getDescItems()) {
                // set node
                descItem.setNode(node);
                // set dataType, itemType, itemSpec
                restoreDescItem(descItem);

                // restore links
                ArrData data = HibernateUtils.unproxy(descItem.getData());
                if (data != null) {
                    if (data instanceof ArrDataStructureRef) {
                        addDataStructRef((ArrDataStructureRef) data);
                    } else if (data instanceof ArrDataRecordRef) {
                        addDataAPRef((ArrDataRecordRef) data);
                    } else if (data instanceof ArrDataFileRef) {
                        addDataFileRef((ArrDataFileRef) data);
                    } else if (data instanceof ArrDataUriRef) {
                        addDataUriRef((ArrDataUriRef) data);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(restoredNode.getDaoLinks())) {
            for (ArrDaoLink daoLink : restoredNode.getDaoLinks()) {
                restoreDaoLink(daoLink);
            }
        }
        if (CollectionUtils.isNotEmpty(restoredNode.getNodeExtensions())) {
            for (ArrNodeExtension nodeExt : restoredNode.getNodeExtensions()) {
                restoreNodeExt(nodeExt);
            }
        }
        if (CollectionUtils.isEmpty(restoredNode.getInhibitedItems())) {
        	restoredNode.setInhibitedItems(Collections.emptyList());
        }
    }

    private void addDataStructRef(ArrDataStructureRef data) {
    	Objects.requireNonNull(data.getStructuredObjectId());

        if (restoreStructData == null) {
            restoreStructData = new HashMap<>();
        }
        List<ArrDataStructureRef> dataList = restoreStructData.computeIfAbsent(data.getStructuredObjectId(),
                                                                               k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataAPRef(ArrDataRecordRef data) {
    	Objects.requireNonNull(data.getRecordId());

        if (restoreAPRef == null) {
            restoreAPRef = new HashMap<>();
        }
        List<ArrDataRecordRef> dataList = restoreAPRef.computeIfAbsent(data.getRecordId(), k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataFileRef(ArrDataFileRef data) {
    	Objects.requireNonNull(data.getFileId());

        if (restoreFileRef == null) {
            restoreFileRef = new HashMap<>();
        }
        List<ArrDataFileRef> dataList = restoreFileRef.computeIfAbsent(data.getFileId(), k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataUriRef(final ArrDataUriRef data) {
        Integer refTemplateId = data.getRefTemplateId();
        if (refTemplateId != null) {
            if (restoreUriRefTemplate == null) {
                restoreUriRefTemplate = new HashMap<>();
            }
            List<ArrDataUriRef> dataList = restoreUriRefTemplate.computeIfAbsent(refTemplateId,
                    k -> new ArrayList<>());
            dataList.add(data);
        }
        Integer nodeId = data.getNodeId();
        if (nodeId != null) {
            if (restoreUriRefNode == null) {
                restoreUriRefNode = new HashMap<>();
            }
            List<ArrDataUriRef> dataList = restoreUriRefNode.computeIfAbsent(nodeId,
                    k -> new ArrayList<>());
            dataList.add(data);
        }
    }

    private void restoreDaoLink(ArrDaoLink daoLink) {
    	Objects.requireNonNull(daoLink.getCreateChangeId());
        ArrChange createChange = em.getReference(ArrChange.class, daoLink.getCreateChangeId());
        daoLink.setCreateChange(createChange, daoLink.getCreateChangeId());

        Objects.requireNonNull(daoLink.getDaoId());

        if (restoreDaoLinks == null) {
            restoreDaoLinks = new HashMap<>();
        }
        List<ArrDaoLink> dataList = restoreDaoLinks.computeIfAbsent(daoLink.getDaoId(), k -> new ArrayList<>());
        dataList.add(daoLink);
    }

    private void restoreNodeExt(ArrNodeExtension nodeExt) {
    	Objects.requireNonNull(nodeExt.getArrangementExtensionId());
    	Objects.requireNonNull(nodeExt.getCreateChangeId());

        RulArrangementExtension arExt = em.getReference(RulArrangementExtension.class, nodeExt
                .getArrangementExtensionId());
        nodeExt.setArrangementExtension(arExt, nodeExt.getArrangementExtensionId());

        ArrChange createChange = em.getReference(ArrChange.class, nodeExt.getCreateChangeId());
        nodeExt.setCreateChange(createChange, nodeExt.getCreateChangeId());
    }

    /**
     * Vyplnění návazných entity {@link ArrStructuredObject}.
     *
     */
    private void prepareStructureData() {
        if (restoreStructData == null) {
            return;
        }
        List<ArrStructuredObject> structureDataList = structureDataRepository.findAllById(restoreStructData.keySet());
        for (ArrStructuredObject structObj : structureDataList) {
            List<ArrDataStructureRef> dataList = restoreStructData.remove(structObj.getStructuredObjectId());
            for (ArrDataStructureRef data : dataList) {
                data.setStructuredObject(structObj);
            }
        }
        // all structured items were restored
        Validate.isTrue(restoreStructData.isEmpty());
        restoreStructData = null;
    }

    /**
     * Vyplnění návazných entity {@link ApAccessPoint}.
     *
     */
    private void prepareAPRefs() {
        if (restoreAPRef == null) {
            return;
        }
        List<ApAccessPoint> records = accessPointRepository.findAllById(restoreAPRef.keySet());
        //Map<Integer, ApAccessPoint> recordsMapFound = new HashMap<>();
        for (ApAccessPoint record : records) {
            List<ArrDataRecordRef> dataList = restoreAPRef.remove(record.getAccessPointId());
            for (ArrDataRecordRef data : dataList) {
                data.setRecord(record);
            }
        }
        // all structured items were restored
        Validate.isTrue(restoreAPRef.isEmpty());
        restoreAPRef = null;
    }

    /**
     * Vyplnění návazných entity {@link ArrFile}.
     *
     */
    private void prepareFileRefs() {
        if (restoreFileRef == null) {
            return;
        }
        List<ArrFile> files = fundFileRepository.findAllById(restoreFileRef.keySet());
        for (ArrFile file : files) {
            List<ArrDataFileRef> dataList = restoreFileRef.remove(file.getFileId());
            for (ArrDataFileRef data : dataList) {
                data.setFile(file);
            }
        }

        Validate.isTrue(restoreFileRef.isEmpty());
        restoreFileRef = null;
    }
    /**
     * Vyplnění návazných entity {@link ArrRefTemplate} a {@link ArrNode}.
     *
     */
    private void prepareUriRefs() {
        if (restoreUriRefNode == null && restoreUriRefTemplate == null) {
            return;
        }

        if (restoreUriRefNode != null) {
            List<ArrNode> nodes = nodeRepository.findAllById(restoreUriRefNode.keySet());
            for (ArrNode node : nodes) {
                List<ArrDataUriRef> dataList = restoreUriRefNode.remove(node.getNodeId());
                for (ArrDataUriRef data : dataList) {
                    data.setArrNode(node);
                }
            }

            Validate.isTrue(restoreUriRefNode.isEmpty());
            restoreUriRefNode = null;
        }

        if (restoreUriRefTemplate != null) {
            List<ArrRefTemplate> templates = refTemplateRepository.findAllById(restoreUriRefTemplate.keySet());
            for (ArrRefTemplate template : templates) {
                List<ArrDataUriRef> dataList = restoreUriRefTemplate.remove(template.getRefTemplateId());
                for (ArrDataUriRef data : dataList) {
                    data.setRefTemplate(template);
                }
            }

            Validate.isTrue(restoreUriRefTemplate.isEmpty());
            restoreUriRefTemplate = null;
        }

    }

    /**
     * Vyplnění návazných entity {@link ArrDao}.
     *
     *
     */
    private void preapareDaoLinks() {
        if (restoreDaoLinks == null) {
            return;
        }
        List<ArrDao> daos = daoRepository.findAllById(restoreDaoLinks.keySet());
        Set<Integer> nodeIds = restoreDaoLinks.values().stream()
                .flatMap(Collection::stream)
                .map(ArrDaoLink::getNodeId)
                .collect(Collectors.toSet());
        Map<Integer, ArrNode> nodesMap = nodeRepository.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(ArrNode::getNodeId, Function.identity()));

        for (ArrDao dao : daos) {
            List<ArrDaoLink> dataList = restoreDaoLinks.remove(dao.getDaoId());

            for (ArrDaoLink daoLink : dataList) {
                daoLink.setDao(dao);
                daoLink.setNode(nodesMap.get(daoLink.getNodeId()));
            }
        }

        Validate.isTrue(restoreDaoLinks.isEmpty());
        restoreDaoLinks = null;
    }
}
