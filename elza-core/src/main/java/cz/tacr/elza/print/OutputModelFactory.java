package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.RuleSystemProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTree;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.core.tree.TreeNode;
import cz.tacr.elza.core.tree.visitors.TopLevelNodesVistor;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemFileRef;
import cz.tacr.elza.print.item.ItemInteger;
import cz.tacr.elza.print.item.ItemJsonTable;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.print.item.ItemPartyRef;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemText;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUnitId;
import cz.tacr.elza.print.item.ItemUnitdate;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.utils.HibernateUtils;

/**
 * Factory for output model initialization.
 */
@Service
public class OutputModelFactory {

    private final StaticDataService staticDataService;

    private final OutputService outputService;

    private final FundTreeProvider fundTreeProvider;

    @Autowired
    public OutputModelFactory(StaticDataService staticDataService,
                              OutputService outputService,
                              FundTreeProvider fundTreeProvider) {
        this.staticDataService = staticDataService;
        this.outputService = outputService;
        this.fundTreeProvider = fundTreeProvider;
    }

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    /**
     * Initializes output model.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutputModel createOutputModel(Integer outputId, ArrFundVersion fundVersion) {
        Validate.notNull(outputId);

        ArrOutputDefinition outputDefinition = outputDefinitionRepository.findOneFetchTypeAndFund(outputId);
        if (outputDefinition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputId", outputId);
        }

        ArrFund fund = outputDefinition.getFund();
        if (fundVersion != null) {
            Validate.isTrue(fundVersion.getFundId().equals(fund.getFundId()));
        }

        return createOutputModel(outputId, fundVersion, outputDefinition);
    }

    private OutputModel createOutputModel(Integer outputId, ArrFundVersion fundVersion, ArrOutputDefinition outputDefinition) {
        // init common
        OutputModel model = new OutputModel(outputId);
        model.setName(outputDefinition.getName());
        model.setInternal_code(outputDefinition.getInternalCode());
        model.setTypeCode(outputDefinition.getOutputType().getCode());
        model.setType(outputDefinition.getOutputType().getName());

        // init node tree (without data)
        readNodeTree(model, outputDefinition, fundVersion);

        // init fund
        ArrFund arrFund = outputDefinition.getFund();
        Fund fund = new Fund(rootNodeId, fundVersion);
        fund.setName(arrFund.getName());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(fundVersion.getDateRange());
        fund.setInternalCode(arrFund.getInternalCode());
        model.setFund(fund);

        // init institution
        ParInstitution parInstitution = arrFund.getInstitution();
        Institution institution = createInstitution(model, parInstitution);
        fund.setInstitution(institution);

        // init output items
        readOutputItems(model, fundVersion, outputDefinition);

        return model;
    }

    private void readNodeTree(OutputModel model, ArrOutputDefinition outputDefinition, ArrFundVersion fundVersion) {
        List<ArrNodeOutput> outputNodes = outputService.getOutputNodes(outputDefinition, fundVersion.getLockChange());
        FundTree fundTree = fundTreeProvider.getFundTree(fundVersion.getFundVersionId());

        Set<Integer> outputNodeIds = new HashSet<>(outputNodes.size());
        outputNodes.forEach(on -> outputNodeIds.add(on.getNodeId()));
        TopLevelNodesVistor outputNodesLocator = new TopLevelNodesVistor(outputNodeIds);

        fundTree.getRoot().traversalDF(outputNodesLocator);

        List<TreeNode> rootNodes = outputNodesLocator.getFoundNodes();
        Set<Integer> rootParentNodeIds = new HashSet<>();
        for (TreeNode rootNode : rootNodes) {
            // add parent nodes
            for (TreeNode parent : rootNode.getParentPath()) {
                // skip duplicate parents
                if (rootParentNodeIds.add(parent.getNodeId())) {
                    readNode(parent, model);
                }
            }
            // add subtree nodes
            rootNode.traversalDF(node -> {
                readNode(node, model);
                return true;
            });
        }
    }

    private void readNode(TreeNode treeNode, OutputModel model) {
        NodeId nodeId = new NodeId(arrNodeId, parentNodeId, position);
        nodeId.

        Integer arrNodeId = level.getNodeId();
        int position = level.getPosition();

        // check if exists
        NodeId nodeId = model.getNodeId(arrNodeId);
        if (nodeId != null) {
            return nodeId;
        }

        if (arrParentNodeId == null) {
            // create root node
            nodeId = new NodeId(arrNodeId, null, position);
        } else {
            // find parent for inner or leaf node
            NodeId parentNodeId = model.getNodeId(arrParentNodeId);
            Validate.notNull(parentNodeId);

            nodeId = new NodeId(arrNodeId, parentNodeId, position);

            // update parent children
            parentNodeId.getChildren().add(nodeId);
        }

        return model.addNodeId(nodeId);
    }

    private Institution createInstitution(OutputModel model, ParInstitution parInstitution) {
        Institution institution = new Institution();
        institution.setTypeCode(parInstitution.getInstitutionType().getCode());
        institution.setType(parInstitution.getInstitutionType().getName());
        institution.setCode(parInstitution.getInternalCode());

        ParParty parParty = parInstitution.getParty();
        Party party = model.getParty(parParty);
        institution.setPartyGroup((PartyGroup) party);

        return institution;
    }

    private void readOutputItems(OutputModel model, ArrFundVersion fundVersion, ArrOutputDefinition outputDefinition) {
        List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(fundVersion, outputDefinition);
        for (ArrOutputItem outputItem : outputItems) {
            if (outputItem.isUndefined()) {
                continue; // skip items without data
            }
            AbstractItem item = createItem(model, outputItem);
            model.addItem(item);
        }
    }

    private AbstractItem createItem(OutputModel model, ArrItem arrItem) {
        RuleSystemProvider ruleSystems = staticDataService.getData().getRuleSystems();

        RuleSystemItemType rsItemType = ruleSystems.getItemType(arrItem.getItemTypeId());
        ItemType itemType = model.getItemType(rsItemType.getEntity());

        AbstractItem item = convertItemData(model, arrItem.getData(), itemType);

        item.setType(Validate.notNull(itemType));
        item.setPosition(arrItem.getPosition());
        item.setUndefined(arrItem.isUndefined());

        if (arrItem.getItemSpecId() != null) {
            RulItemSpec rulItemSpec = rsItemType.getItemSpecById(arrItem.getItemSpecId());
            ItemSpec itemSpec = model.getItemSpec(rulItemSpec);
            item.setSpecification(Validate.notNull(itemSpec));
        }

        return item;
    }

    private AbstractItem convertItemData(OutputModel model, ArrData data, ItemType itemType) {
        Validate.isTrue(HibernateUtils.isInitialized(data));

        switch(itemType.getDataType()) {
            case UNITID:
                ArrDataUnitid unitid = (ArrDataUnitid) data;
                return new ItemUnitId(unitid.getValue());
            case UNITDATE:
                ArrDataUnitdate unitdate = (ArrDataUnitdate) data;
                return createItemUnitdate(unitdate);
            case TEXT:
                ArrDataText text = (ArrDataText) data;
                return new ItemText(text.getValue());
            case STRING:
                ArrDataString str = (ArrDataString) data;
                return new ItemString(str.getValue());
            case RECORD_REF:
                ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                return createItemAPRef(model, apRef);
            case PARTY_REF:
                ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                return createItemPartyRef(model, partyRef);
            case PACKET_REF:
                ArrDataPacketRef packetRef = (ArrDataPacketRef) data;
                return createItemPacketRef(packetRef);
            case JSON_TABLE:
                ArrDataJsonTable jsonTable = (ArrDataJsonTable) data;
                return new ItemJsonTable(itemType.getTableDefinition(), jsonTable.getValue());
            case INT:
                ArrDataInteger integer = (ArrDataInteger) data;
                return new ItemInteger(integer.getValue());
            case FORMATTED_TEXT:
                ArrDataText ftext = (ArrDataText) data;
                return new ItemText(ftext.getValue());
            case FILE_REF:
                ArrDataFileRef fileRef = (ArrDataFileRef) data;
                return createItemFile(fileRef);
            case ENUM:
                return ItemEnum.newInstance();
            case DECIMAL:
                ArrDataDecimal decimal = (ArrDataDecimal) data;
                return new ItemDecimal(decimal.getValue());
            case COORDINATES:
                ArrDataCoordinates coords = (ArrDataCoordinates) data;
                return new ItemCoordinates(coords.getValue());
            default:
                throw new SystemException("Uknown data type", BaseCode.INVALID_STATE).set("dataType", itemType.getDataType());
        }
    }

    private AbstractItem createItemUnitdate(ArrDataUnitdate data) {
        CalendarType ct = CalendarType.fromId(data.getCalendarTypeId());
        UnitDate unitdate = UnitDate.valueOf(data, ct.getEntity());
        return new ItemUnitdate(unitdate);
    }

    // TODO: vyresit fetch record
    private ItemRecordRef createItemAPRef(OutputModel model, ArrDataRecordRef data) {
        Record record = model.getRecordFromCache(itemData.getRecordId());
        if (record == null) {
            RegRecord regRecord = itemData.getRecord();
            record = model.getRecord(regRecord);
        }
        return new ItemRecordRef(record);
    }

    // TODO: vyresit fetch party
    private ItemPartyRef createItemPartyRef(OutputModel model, ArrDataPartyRef data) {
        Party party = model.getPartyFromCache(data.getPartyId());
        if(party==null) {
            final ParParty parParty = data.getParty();
            party = model.getParty(parParty);
        }
        return new ItemPartyRef(party);
    }

    // TODO: vyresit fetch packet
    private ItemPacketRef createItemPacketRef(final ArrDataPacketRef itemData) {
        Packet packet = packetMap.get(itemData.getPacketId());
        if (packet == null) {
            final ArrPacket arrPacket = itemData.getPacket();
            packet = new Packet();
            RulPacketType packetType = arrPacket.getPacketType();
            if (packetType != null) {
                packet.setType(packetType.getName());
                packet.setTypeCode(packetType.getCode());
                packet.setTypeShortcut(packetType.getShortcut());
            }
            packet.setStorageNumber(arrPacket.getStorageNumber());
            packet.setState(arrPacket.getState().name());
            packetMap.put(arrPacket.getPacketId(), packet);
        }
        return new ItemPacketRef(packet);
    }

    // TODO: vyresit fetch file
    private ItemFileRef createItemFile(final ArrDataFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFileRef itemFile = new ItemFileRef(arrFile);
        itemFile.setName(arrFile.getName());
        itemFile.setFileName(arrFile.getFileName());
        itemFile.setFileSize(arrFile.getFileSize());
        itemFile.setMimeType(arrFile.getMimeType());
        itemFile.setPagesCount(arrFile.getPagesCount());

        return itemFile;
    }
}
