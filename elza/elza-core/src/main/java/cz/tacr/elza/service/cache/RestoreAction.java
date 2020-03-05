package cz.tacr.elza.service.cache;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

public class RestoreAction {

    final StaticDataProvider sdp;

    private Map<Integer, List<ArrDataStructureRef>> restoreStructData;
    private Map<Integer, List<ArrDataPartyRef>> restorePartyRef;
    private Map<Integer, List<ArrDataRecordRef>> restoreAPRef;
    private Map<Integer, List<ArrDataFileRef>> restoreFileRef;
    private Map<Integer, List<ArrDaoLink>> restoreDaoLinks;

    final private StructuredObjectRepository structureDataRepository;

    final private PartyRepository partyRepository;

    /*final private PartyNameComplementRepository partyNameComplementRepository;

    final private PartyNameRepository partyNameRepository;*/

    final private ApAccessPointRepository accessPointRepository;

    final private FundFileRepository fundFileRepository;

    final private DaoRepository daoRepository;

    final private DataUriRefRepository dataUriRefRepository;

    final private NodeRepository nodeRepository;

    final private EntityManager em;

    public RestoreAction(final StaticDataProvider sdp,
                         final EntityManager em,
                         final StructuredObjectRepository structureDataRepository,
                         final PartyRepository partyRepository,
            /*final PartyNameComplementRepository partyNameComplementRepository,
            final PartyNameRepository partyNameRepository,*/
                         final ApAccessPointRepository accessPointRepository,
                         final FundFileRepository fundFileRepository,
                         final DaoRepository daoRepository,
                         final NodeRepository nodeRepository,
                         final DataUriRefRepository dataUriRefRepository) {
        this.sdp = sdp;
        this.em = em;
        this.structureDataRepository = structureDataRepository;
        this.partyRepository = partyRepository;
        /*this.partyNameComplementRepository = partyNameComplementRepository;
        this.partyNameRepository = partyNameRepository;*/
        this.accessPointRepository = accessPointRepository;
        this.fundFileRepository = fundFileRepository;
        this.daoRepository = daoRepository;
        this.nodeRepository = nodeRepository;
        this.dataUriRefRepository = dataUriRefRepository;
    }

    public void restore(Collection<RestoredNode> cachedNodes) {
        for (RestoredNode restoredNode : cachedNodes) {
            restore(restoredNode);
        }

        prepareStructureData();
        preparePartyRefs();
        prepareAPRefs();
        prepareFileRefs();
        preapareDaoLinks();
    }

    /**
     * Load description item type from rule system provider
     *
     * @param descItem
     */
    private void restoreDescItem(ArrDescItem descItem) {
        // restore createChange
        Validate.notNull(descItem.getCreateChangeId());
        ArrChange createChange = em.getReference(ArrChange.class, descItem.getCreateChangeId());
        descItem.setCreateChange(createChange, descItem.getCreateChangeId());

        // Deleted items cannot be stored in cache
        if (descItem.getDeleteChangeId() != null) {
            throw new SystemException("Item is marked as deleted and placed in cache", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("itemId", descItem.getItemId());
        }

        // restore item type
        Validate.notNull(descItem.getItemTypeId());
        ItemType itemType = sdp.getItemTypeById(descItem.getItemTypeId());
        Validate.notNull(itemType);

        descItem.setItemType(itemType.getEntity());

        Integer itemSpecId = descItem.getItemSpecId();
        // check if specification should be set
        if (itemType.hasSpecifications()) {
            Validate.notNull(itemSpecId);
        } else {
            Validate.isTrue(itemSpecId == null);
        }
        // prepare specification
        if (itemSpecId != null) {
            RulItemSpec itemSpec = itemType.getItemSpecById(itemSpecId);
            Validate.notNull(itemSpec);
            descItem.setItemSpec(itemSpec);
        }

        // Restore dataType
        ArrData data = descItem.getData();
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

    /**
     * Load unit date fields
     *
     * Method sets proper calendar type.
     *
     * @param data
     */
    private void loadUnitdate(ArrDataUnitdate data) {
        CalendarType calendarType = CalendarType.fromId(data.getCalendarTypeId());
        Validate.notNull(calendarType);
        data.setCalendarType(calendarType.getEntity());
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
                ArrData data = descItem.getData();
                if (data != null) {
                    if (data instanceof ArrDataStructureRef) {
                        addDataStructRef((ArrDataStructureRef) data);
                    } else if (data instanceof ArrDataPartyRef) {
                        addDataPartyRef((ArrDataPartyRef) data);
                    } else if (data instanceof ArrDataRecordRef) {
                        addDataAPRef((ArrDataRecordRef) data);
                    } else if (data instanceof ArrDataFileRef) {
                        addDataFileRef((ArrDataFileRef) data);
                    } else if (data instanceof ArrDataUnitdate) {
                        loadUnitdate((ArrDataUnitdate) data);
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
    }

    private void addDataStructRef(ArrDataStructureRef data) {
        Validate.notNull(data.getStructuredObjectId());

        if (restoreStructData == null) {
            restoreStructData = new HashMap<>();
        }
        List<ArrDataStructureRef> dataList = restoreStructData.computeIfAbsent(data.getStructuredObjectId(),
                                                                               k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataPartyRef(ArrDataPartyRef data) {
        Validate.notNull(data.getPartyId());

        if (restorePartyRef == null) {
            restorePartyRef = new HashMap<>();
        }
        List<ArrDataPartyRef> dataList = restorePartyRef.computeIfAbsent(data.getPartyId(), k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataAPRef(ArrDataRecordRef data) {
        Validate.notNull(data.getRecordId());

        if (restoreAPRef == null) {
            restoreAPRef = new HashMap<>();
        }
        List<ArrDataRecordRef> dataList = restoreAPRef.computeIfAbsent(data.getRecordId(), k -> new ArrayList<>());
        dataList.add(data);
    }

    private void addDataFileRef(ArrDataFileRef data) {
        Validate.notNull(data.getFileId());

        if (restoreFileRef == null) {
            restoreFileRef = new HashMap<>();
        }
        List<ArrDataFileRef> dataList = restoreFileRef.computeIfAbsent(data.getFileId(), k -> new ArrayList<>());
        dataList.add(data);
    }

    private void restoreDaoLink(ArrDaoLink daoLink) {
        Validate.notNull(daoLink.getCreateChangeId());
        ArrChange createChange = em.getReference(ArrChange.class, daoLink.getCreateChangeId());
        daoLink.setCreateChange(createChange, daoLink.getCreateChangeId());

        Validate.notNull(daoLink.getDaoId());

        if (restoreDaoLinks == null) {
            restoreDaoLinks = new HashMap<>();
        }
        List<ArrDaoLink> dataList = restoreDaoLinks.computeIfAbsent(daoLink.getDaoId(), k -> new ArrayList<>());
        dataList.add(daoLink);
    }

    private void restoreNodeExt(ArrNodeExtension nodeExt) {
        Validate.notNull(nodeExt.getArrangementExtensionId());
        Validate.notNull(nodeExt.getCreateChangeId());

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
        List<ArrStructuredObject> structureDataList = structureDataRepository.findAll(restoreStructData.keySet());
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
     * Vyplnění návazných entity {@link ParParty}.
     *
     */
    private void preparePartyRefs() {
        if (restorePartyRef == null) {
            return;
        }

        // Load parties
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(restorePartyRef.keySet());
        int partiesFound = 0;
        //List<ParPartyName> partyNames = new ArrayList<>();
        while (iterator.hasNext()) {
            List<Integer> next = iterator.next();
            List<ParParty> parties = partyRepository.findAllFetch(next);
            //partyNames.addAll(partyNameRepository.findByPartyIds(next));

            partiesFound += parties.size();
            for (ParParty party : parties) {


                List<ArrDataPartyRef> dataList = restorePartyRef.get(party.getPartyId());
                for (ArrDataPartyRef partyRef : dataList) {
                    partyRef.setParty(party);
                }
            }
        }

        // check if all parties processed
        Validate.isTrue(partiesFound == restorePartyRef.size(),
                        "Not all parties were found, foundParties = %d, expectedParties = %d", partiesFound,
                        restorePartyRef.size());
        restorePartyRef = null;

        // read complements for prefered name?
        // not used
        /*
        Map<Integer, List<ParPartyName>> partyNamesMap = partyNames.stream().collect(Collectors.groupingBy(s -> s
                .getParty().getPartyId()));

        for (ParParty party : partiesMapFound.values()) {
            CollectionUtils.addIgnoreNull(preferredNameIds, party.getPreferredNameId());
        }

        iterator = new ObjectListIterator<>(preferredNameIds);
        Map<Integer, List<ParPartyNameComplement>> preferredNameIdComplementsMap = new HashMap<>();
        while (iterator.hasNext()) {
            List<ParPartyNameComplement> partyNameComplements = partyNameComplementRepository.findByPartyNameIds(
                                                                                                                 iterator.next());
            Map<Integer, List<ParPartyNameComplement>> map = partyNameComplements.stream().collect(Collectors
                    .groupingBy(s -> s.getPartyName().getPartyNameId()));
            map.forEach((k, v) -> preferredNameIdComplementsMap.merge(k, v, (v1, v2) -> {
                Set<ParPartyNameComplement> set = new TreeSet<>(v1);
                set.addAll(v2);
                return new ArrayList<>(set);
            }));
        }

        for (ParParty party : partiesMapFound.values()) {
            if (party.getPreferredNameId() != null) {
                List<ParPartyNameComplement> partyNameComplements = preferredNameIdComplementsMap.get(party
                        .getPreferredNameId());
                if (partyNameComplements != null) {
                    party.getPreferredName().setPartyNameComplements(partyNameComplements);
                } else {
                    party.getPreferredName().setPartyNameComplements(Collections.emptyList());
                }
            }
            List<ParPartyName> parPartyNames = partyNamesMap.get(party.getPartyId());
            if (parPartyNames != null) {
                party.setPartyNames(parPartyNames);
            } else {
                party.setPartyNames(Collections.emptyList());
            }
        }*/
    }

    /**
     * Vyplnění návazných entity {@link ApAccessPoint}.
     *
     */
    private void prepareAPRefs() {
        if (restoreAPRef == null) {
            return;
        }
        List<ApAccessPoint> records = accessPointRepository.findAll(restoreAPRef.keySet());
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
        List<ArrFile> files = fundFileRepository.findAll(restoreFileRef.keySet());
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
     * Vyplnění návazných entity {@link ArrDao}.
     *
     *
     */
    private void preapareDaoLinks() {
        if (restoreDaoLinks == null) {
            return;
        }
        List<ArrDao> daos = daoRepository.findAll(restoreDaoLinks.keySet());
        Set<Integer> nodeIds = restoreDaoLinks.values().stream()
                .flatMap(Collection::stream)
                .map(ArrDaoLink::getNodeId)
                .collect(Collectors.toSet());
        Map<Integer, ArrNode> nodesMap = nodeRepository.findAll(nodeIds).stream()
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
