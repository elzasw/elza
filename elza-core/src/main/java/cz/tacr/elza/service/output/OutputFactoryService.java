package cz.tacr.elza.service.output;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrItemPartyRef;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.print.Fund;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.NodeLoader;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.RecordType;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.UnitDateText;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemFile;
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
import cz.tacr.elza.print.party.Dynasty;
import cz.tacr.elza.print.party.Event;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyName;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.utils.ProxyUtils;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.MappingDirection;

/**
 * Factory pro vytvoření struktury pro výstupy
 *
 * @author Martin Lebeda
 * @since 03.05.2016
 */

@Service
public class OutputFactoryService implements NodeLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private MapperFacade mapper;

    @Autowired
    private OutputGeneratorWorkerFactory outputGeneratorFactory;

    @Autowired
    private PartyGroupRepository partyGroupRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RegistryService registryService;

    public OutputFactoryService() {
        // inicializace mapperů
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        mapperFactory.classMap(ArrItemUnitdate.class, UnitDate.class)
                .customize(new CustomMapper<ArrItemUnitdate, UnitDate>() {
                    @Override
                    public void mapAtoB(final ArrItemUnitdate arrItemUnitdate, final UnitDate unitDate, final MappingContext context) {
                        super.mapAtoB(arrItemUnitdate, unitDate, context);
                        final ArrCalendarType calendarType = arrItemUnitdate.getCalendarType();
                        unitDate.setCalendarType(calendarType);
                        unitDate.setCalendar(calendarType.getName());
                        unitDate.setCalendarCode(calendarType.getCode());
                    }
                })
                .exclude("calendarType")
                .byDefault(MappingDirection.A_TO_B).register();

        mapperFactory.classMap(RegRegisterType.class, RecordType.class).byDefault(MappingDirection.A_TO_B).register();

        mapper = mapperFactory.getMapperFacade();
    }

    /**
     * Factory metoda pro vytvoření logické struktury Output struktury
     *
     * @param arrOutput databázová položka s definicí požadovaného výstupu
     * @return struktura pro použití v šablonách
     */
    public Output createOutput(final ArrOutput arrOutput) {
        // naplnit output
        final Output output = outputGeneratorFactory.getOutput(arrOutput);
        output.setName(arrOutput.getOutputDefinition().getName());
        output.setInternal_code(arrOutput.getOutputDefinition().getInternalCode());
        output.setTypeCode(arrOutput.getOutputDefinition().getOutputType().getCode());
        output.setType(arrOutput.getOutputDefinition().getOutputType().getName());

        // fund
        final ArrFund arrFund = arrOutput.getOutputDefinition().getFund();
        ArrFundVersion arrFundVersion = arrangementService.getOpenVersionByFundId(arrFund.getFundId());
        final Fund fund = createFund(null, arrFund, arrFundVersion);
        output.setFund(fund);

        // zařadit do výstupu rootNode fundu
        final ArrNode arrFundRootNode = arrFundVersion.getRootNode();
        ArrLevel arrRootLevel = levelRepository.findNodeInRootTreeByNodeId(arrFundRootNode, arrFundRootNode, arrFundVersion.getLockChange());
        NodeId rootNodeId = createNodeId(arrRootLevel, output, null);
        fund.setRootNodeId(rootNodeId);

        // plnit institution
        final ParInstitution arrFundInstitution = arrFund.getInstitution();
        final Institution institution = createInstitution(arrFundInstitution, output);
        fund.setInstitution(institution);

        // zařadit items přímo přiřazené na output
        addOutputItems(arrOutput, output, arrFundVersion);

        // zařadit strom nodes
        createNodeIdTree(arrOutput, output);

        return output;
    }

    private void createNodeIdTree(final ArrOutput arrOutput, final Output output) {
        List<ArrNode> nodes = outputService.getNodesForOutput(arrOutput);
        nodes.sort((n1, n2) -> n1.getNodeId().compareTo(n2.getNodeId()));

        ArrChange lockChange = output.getArrFundVersion().getLockChange();
        ArrNode rootNode = nodeRepository.findOne(output.getFund().getRootNodeId().getArrNodeId());
        for (ArrNode arrNode : nodes) {
            output.addDirectNodeIdentifier(arrNode.getNodeId());

            ArrLevel arrLevel = levelRepository.findNodeInRootTreeByNodeId(arrNode, rootNode, lockChange);
            addParentNodeByNode(arrLevel, output);
        }
    }

    /**
     * Naplní do výstupu hodnoty atributů přiřazené na definicivýstupu.
     */
    private void addOutputItems(final ArrOutput arrOutput, final Output output, final ArrFundVersion arrFundVersion) {
        final List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(arrFundVersion, arrOutput.getOutputDefinition());
        for (ArrOutputItem arrOutputItem : outputItems) {
            final AbstractItem item = getItem(arrOutputItem.getItemId(), output, null);
            output.getItems().add(item);
        };
    }

    private PartyGroup createPartyGroup(final Output output, final ParParty parParty) {
        final PartyGroup partyGroup = new PartyGroup();
        final ParPartyGroup parPartyGroup = partyGroupRepository.findOne(parParty.getPartyId());
        partyGroup.setScope(parPartyGroup.getScope());
        partyGroup.setFoundingNorm(parPartyGroup.getFoundingNorm());
        partyGroup.setOrganization(parPartyGroup.getOrganization());
        partyGroup.setScopeNorm(parPartyGroup.getScopeNorm());

        // partyGroup - odděděno od party
        partyGroup.setType(parParty.getPartyType().getName());
        partyGroup.setTypeCode(parParty.getPartyType().getCode());
        partyGroup.setHistory(parParty.getHistory());
        partyGroup.setSourceInformation(parParty.getSourceInformation());
        partyGroup.setCharacteristics(parParty.getCharacteristics());
        partyGroup.setPreferredName(createPartyName(parParty.getPreferredName()));

        List<ParPartyName> partyNames = parParty.getPartyNames();
        if (CollectionUtils.isNotEmpty(partyNames)) {
            List<ParPartyName> namesToCreate = new ArrayList<>(partyNames);
            namesToCreate.remove(parParty.getPreferredName());

            for (ParPartyName parPartyName : namesToCreate) {
                PartyName partyName = createPartyName(parPartyName);
                partyGroup.getNames().add(partyName);
            }
        }

        Record partyRecord = createRecord(output, parParty);
        partyGroup.setRecord(partyRecord);

        return partyGroup;
    }

    private Record createRecord(final Output output, final ParParty parParty) {
        final RegRecord parPartyRecord = parParty.getRecord();
        Record partyRecord = new Record(output);
        partyRecord.setRecord(parPartyRecord.getRecord());
        partyRecord.setCharacteristics(parPartyRecord.getCharacteristics());
        parPartyRecord.getVariantRecordList().stream().forEach(regVariantRecord -> partyRecord.getVariantRecords().add(regVariantRecord.getRecord()));

        partyRecord.setType(getRecordTypeByPartyRecord(output, parPartyRecord));
        return partyRecord;
    }

    private Institution createInstitution(final ParInstitution arrFundInstitution, final Output output) {
        final Institution institution = new Institution();
        institution.setTypeCode(arrFundInstitution.getInstitutionType().getCode());
        institution.setType(arrFundInstitution.getInstitutionType().getName());
        institution.setCode(arrFundInstitution.getInternalCode());

        // partyGroup k instituci
        final ParParty parParty = arrFundInstitution.getParty();

        // Check party type
        final ParPartyType partyType = parParty.getPartyType();
        if(!ParPartyType.PartyTypeEnum.GROUP_PARTY.toString().equals(partyType.getCode())) {
            throw new IllegalStateException("Party for institution is not GROUP_PARTY, partyId = "+parParty.getPartyId());
        }
        // create party group
        final PartyGroup partyGroup = createPartyGroup(output, parParty);
        institution.setPartyGroup(partyGroup);

        return institution;
    }

    private Fund createFund(final NodeId rootNodeId, final ArrFund arrFund, final ArrFundVersion arrFundVersion) {
        Fund fund = new Fund(rootNodeId, arrFundVersion);
        fund.setName(arrFund.getName());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(arrFundVersion.getDateRange());
        fund.setInternalCode(arrFund.getInternalCode());

        return fund;
    }

    /**
     * factory metoda konvertující objekty
     */
    private PartyName createPartyName(final ParPartyName parPartyPreferredName) {
        PartyName preferredName = new PartyName();
        preferredName.setMainPart(parPartyPreferredName.getMainPart());
        preferredName.setOtherPart(parPartyPreferredName.getOtherPart());
        preferredName.setNote(parPartyPreferredName.getNote());
        preferredName.setDegreeBefore(parPartyPreferredName.getDegreeBefore());
        preferredName.setDegreeAfter(parPartyPreferredName.getDegreeAfter());
        preferredName.setValidFrom(createUnitDateText(parPartyPreferredName.getValidFrom()));
        preferredName.setValidTo(createUnitDateText(parPartyPreferredName.getValidTo()));
        return preferredName;
    }

    /**
     * factory metoda konvertující objekty
     */
    private UnitDateText createUnitDateText(final ParUnitdate parUnitdate) {
        if (parUnitdate == null) {
            return null;
        }
        UnitDateText dateFrom = null;
        final String format = parUnitdate.getFormat();
        if (StringUtils.isNotBlank(format)) { // musí být nastaven formát
            dateFrom = new UnitDateText();
            dateFrom.setValueText(UnitDateConvertor.convertToString(parUnitdate));
        }
        return dateFrom;
    }

    /**
     * Metoda vytvoří strukturu nodů vč. nadřazených až k rootu a vč. stromu všech potomků
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrLevel node přímo přiřazený k outputu
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void addParentNodeByNode(final ArrLevel arrLevel, final Output output) {
        // získat seznam rodičů node a zařadit
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        ArrNode arrNode = arrLevel.getNode();
        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(arrNode, arrFundVersion);
        levelList.sort((l1, l2) -> l1.getNode().getNodeId().compareTo(l2.getNode().getNodeId()));

        for (ArrLevel arrParentLevel : levelList) {
            ArrNode parentNode = arrParentLevel.getNodeParent();
            createNodeId(arrParentLevel, output, parentNode);
        }

        if (levelList.size() > 0) {
            // získat node vč potomků a atributů
            ArrNode parentNode = levelList.get(levelList.size() - 1).getNode();
            getNodeIdWithChildren(arrLevel, output, parentNode);
        } else {
            getNodeIdWithChildren(arrLevel, output, null);
        }
    }

    /**
     * Vytvoří node vč. celého stromu potomků.
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrLevel zdrojový level
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void getNodeIdWithChildren(final ArrLevel arrLevel, final Output output, final ArrNode parentNode) {
        ArrNode arrNode = arrLevel.getNode();
        createNodeId(arrLevel, output, parentNode);

        // získat children
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();

        final List<ArrLevel> allChildrenByNode = levelRepository.findByParentNode(arrNode, arrChange);
        for (ArrLevel arrChildLevel : allChildrenByNode) {
            getNodeIdWithChildren(arrChildLevel, output, arrNode);
        }
    }

    /**
     * Vytvoří {@link NodeId}.
     *
     * @param arrLevel    zdrojová úroveň
     * @param output      výstup, ke kterému se budou nody zařazovat
     * @param arrParentNode  nadřazený uzel
     * @return node vč. items
     */
    private NodeId createNodeId(final ArrLevel arrLevel, final Output output, final ArrNode arrParentNode) {
        NodeId parent = null;
        Integer depth = 1;
        if (arrParentNode != null) {
            parent = output.getNodeId(arrParentNode.getNodeId());
            depth = parent.getDepth() + 1;
        }
        Integer parentNodeId = null;
        if (arrParentNode != null) {
            parentNodeId = arrParentNode.getNodeId();
        }
        Integer nodeIdentifier = arrLevel.getNode().getNodeId();
        Integer position = arrLevel.getPosition();
        NodeId nodeId = outputGeneratorFactory.getNodeId(output, nodeIdentifier, parentNodeId, position, depth);

        nodeId = output.addNodeId(nodeId);
        if (parent != null) {
            parent.getChildren().add(nodeId);
        }
        return nodeId;
    }

    /**
     * @param nodeId ID Požadovaného node (odpovídá ID arrNode)
     * @param output output pod který node patří
     * @return Node pro tisk
     */
    public Node getNode(final NodeId nodeId, final Output output) {
        return outputGeneratorFactory.getNode(nodeId, output);
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItemId zdrojový item
     * @param output  výstup, ke kterému se budou items zařazovat
     * @param nodeId    node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    @Transactional(readOnly = true)
    public AbstractItem getItem(final Integer arrItemId, final Output output, final NodeId nodeId) {
        final ArrItem arrItem = itemService.loadDataById(arrItemId);
        final AbstractItem item = getItemByType(output, nodeId, arrItem);

        // založit itemSpec
        final RulItemSpec rulItemSpec = arrItem.getItemSpec();
        if (rulItemSpec != null) {
            final ItemSpec itemSpec = createItemSpec(rulItemSpec);
            item.setSpecification(itemSpec);
        }

        // založit itemType
        final RulItemType rulItemType = arrItem.getItemType();
        final ItemType itemType = createItemType(rulItemType);
        item.setType(itemType);

        return item;
    }

    private ItemType createItemType(final RulItemType rulItemType) {
        final ItemType itemType = new ItemType();
        itemType.setName(rulItemType.getName());
        itemType.setDataType(rulItemType.getDataType().getCode());
        itemType.setShortcut(rulItemType.getShortcut());
        itemType.setDescription(rulItemType.getDescription());
        itemType.setCode(rulItemType.getCode());
        itemType.setViewOrder(rulItemType.getViewOrder());
        return itemType;
    }

    private ItemSpec createItemSpec(final RulItemSpec rulItemSpec) {
        final ItemSpec itemSpec = new ItemSpec();
        itemSpec.setName(rulItemSpec.getName());
        itemSpec.setShortcut(rulItemSpec.getShortcut());
        itemSpec.setDescription(rulItemSpec.getDescription());
        itemSpec.setCode(rulItemSpec.getCode());
        return itemSpec;
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItem zdrojová item
     * @param output   výstup, ke kterému se budou items zařazovat
     * @param nodeId     node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    private AbstractItem getItemByType(final Output output, final NodeId nodeId, final ArrItem arrItem) {
        final ArrItemData itemData = arrItem.getItem();

        AbstractItem item;
        if (itemData instanceof ArrItemUnitid) {
            item = getItemUnitid(nodeId, (ArrItemUnitid) itemData);
        } else if (itemData instanceof ArrItemUnitdate) {
            item = getItemUnitdate(nodeId, (ArrItemUnitdate) itemData);
        } else if (itemData instanceof ArrItemText) {
            item = getItemUnitText(nodeId, (ArrItemText) itemData);
        } else if (itemData instanceof ArrItemString) {
            item = getItemUnitString(nodeId, (ArrItemString) itemData);
        } else if (itemData instanceof ArrItemRecordRef) {
            item = getItemUnitRecordRef(output, nodeId, (ArrItemRecordRef) itemData);
        } else if (itemData instanceof ArrItemPartyRef) {
            item = getItemUnitPartyRef(output, nodeId, (ArrItemPartyRef) itemData);
        } else if (itemData instanceof ArrItemPacketRef) {
            item = getItemUnitPacketRef(nodeId, (ArrItemPacketRef) itemData);
        } else if (itemData instanceof ArrItemJsonTable) {
            item = getItemUnitJsonTable(nodeId, (ArrItemJsonTable) itemData);
        } else if (itemData instanceof ArrItemInt) {
            item = getItemUnitInteger(nodeId, (ArrItemInt) itemData);
        } else if (itemData instanceof ArrItemFormattedText) {
            item = getItemUnitFormatedText(nodeId, (ArrItemFormattedText) itemData);
        } else if (itemData instanceof ArrItemFileRef) {
            item = getItemFile(nodeId, (ArrItemFileRef) itemData);
        } else if (itemData instanceof ArrItemEnum) {
            item = getItemUnitEnum(nodeId, (ArrItemEnum) itemData);
        } else if (itemData instanceof ArrItemDecimal) {
            item = getItemUnitDecimal(nodeId, (ArrItemDecimal) itemData);
        } else if (itemData instanceof ArrItemCoordinates) {
            item = getItemUnitCoordinates(nodeId, (ArrItemCoordinates) itemData);
        } else {
            logger.warn("Neznámý datový typ hodnoty Item ({}) je zpracován jako string.", itemData.getClass().getName());
            item = new ItemString(nodeId, itemData.toString());
        }

        item.setPosition(arrItem.getPosition());

        return item;
    }

    private AbstractItem getItemFile(final NodeId nodeId, final ArrItemFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFile itemFile = new ItemFile(nodeId, arrFile);
        itemFile.setName(arrFile.getName());
        itemFile.setFileName(arrFile.getFileName());
        itemFile.setFileSize(arrFile.getFileSize());
        itemFile.setMimeType(arrFile.getMimeType());
        itemFile.setPagesCount(arrFile.getPagesCount());

        return itemFile;
    }

    private AbstractItem getItemUnitString(final NodeId nodeId, final ArrItemString itemData) {
        return new ItemString(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitRecordRef(final Output output, final NodeId nodeId, final ArrItemRecordRef itemData) {
        final Record record = getRecordByItem(output, itemData);
        return new ItemRecordRef(nodeId, record);
    }

    private Record getRecordByItem(final Output output, final ArrItemRecordRef itemData) {
        Record record = getRecord(output, itemData.getRecord());
        record.setType(getRecordTypeByItem(output, itemData));
        return record;
    }

    private Record createRecord(final Output output, final RegRecord regRecord) {
        Record record =  getRecord(output, regRecord);
        record.setType(getRecordTypeByNode(output, regRecord.getRegisterType()));
        return record;
    }

    private Record getRecordByParty(final Output output, final RegRecord partyRecord) {
        Record record = getRecord(output, partyRecord);
        record.setType(getRecordTypeByPartyRecord(output, partyRecord));
        return record;
    }

    private Record getRecord(@NotNull final Output output, @NotNull final RegRecord regRecord) {
        Record record = outputGeneratorFactory.getRecord(output);
        record.setRecord(regRecord.getRecord());
        record.setCharacteristics(regRecord.getCharacteristics());
        regRecord.getVariantRecordList().stream().forEach(regVariantRecord -> record.getVariantRecords().add(regVariantRecord.getRecord()));
        return record;
    }

    private RecordType getRecordTypeByItem(final Output output, final ArrItemRecordRef itemData) {
        final RegRegisterType registerType = itemData.getRecord().getRegisterType();
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
    }

    private RecordType getRecordTypeByNode(final Output output, final RegRegisterType registerType) {
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountDirectRecords(recordType.getCountDirectRecords() + 1);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
        }

    private RecordType getRecordTypeByPartyRecord(final Output output, final RegRecord parPartyRecord) {
        final RegRegisterType registerType = parPartyRecord.getRegisterType();
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
    }

    private RecordType getRecordType(final Output output, final RegRegisterType registerType) {
        RecordType recordType = output.getRecordTypes().get(registerType.getCode());
        if (recordType == null) {
            recordType = mapper.map(registerType, RecordType.class);
            output.getRecordTypes().put(registerType.getCode(), recordType);
        }
        return recordType;
    }

    private AbstractItem getItemUnitPartyRef(final Output output, final NodeId nodeId, final ArrItemPartyRef itemData) {
        final ParParty parParty = itemData.getParty();
        String partyTypeCode = parParty.getPartyType().getCode();
        PartyType partyType = PartyType.getByCode(partyTypeCode);

        Party party;
        switch (partyType) {
            case DYNASTY:
                ParDynasty parDynasty = ProxyUtils.deproxy(parParty);
                party = createDynasty(parDynasty, output);
                break;
            case EVENT:
                ParEvent parEvent = ProxyUtils.deproxy(parParty);
                party = createEvent(parEvent, output);
                break;
            case PARTY_GROUP:
                ParPartyGroup parPartyGroup = ProxyUtils.deproxy(parParty);
                party = createPartyGroup(parPartyGroup, output);
                break;
            case PERSON:
                ParPerson parPerson = ProxyUtils.deproxy(parParty);
                party = createPerson(parPerson, output);
                break;
            default :
                throw new IllegalStateException("Neznámý typ osoby " + partyType.getCode());
        }

        return new ItemPartyRef(nodeId, party);
    }

    private Person createPerson(final ParPerson parPerson, final Output output) {
        Person person = new Person();
        fillCommonPartyAttributes(person, parPerson, output);

        return person;
    }

    private PartyGroup createPartyGroup(final ParPartyGroup parPartyGroup, final Output output) {
        PartyGroup partyGroup = new PartyGroup();
        fillCommonPartyAttributes(partyGroup, parPartyGroup, output);

        partyGroup.setFoundingNorm(parPartyGroup.getFoundingNorm());
        partyGroup.setOrganization(parPartyGroup.getOrganization());
        partyGroup.setScope(parPartyGroup.getScope());
        partyGroup.setScopeNorm(parPartyGroup.getScopeNorm());

        return partyGroup;
    }

    private Event createEvent(final ParEvent parEvent, final Output output) {
        Event event = new Event();
        fillCommonPartyAttributes(event, parEvent, output);

        return event;
    }

    private Dynasty createDynasty(final ParDynasty parDynasty, final Output output) {
        Dynasty dynasty = new Dynasty();
        fillCommonPartyAttributes(dynasty, parDynasty, output);

        dynasty.setGenealogy(parDynasty.getGenealogy());

        return dynasty;
    }

    private void fillCommonPartyAttributes(final Party party, final ParParty parParty, final Output output) {
        party.setPreferredName(createPartyName(parParty.getPreferredName()));
        partyNameRepository.findByParty(parParty).stream()
                .filter(parPartyName -> !parPartyName.getPartyNameId().equals(parParty.getPreferredName().getPartyNameId())) // kromě preferovaného jména
                .forEach(parPartyName -> party.getNames().add(createPartyName(parPartyName)));
        party.setHistory(parParty.getHistory());
        party.setSourceInformation(parParty.getSourceInformation());
        party.setCharacteristics(parParty.getCharacteristics());
        party.setRecord(getRecordByParty(output, parParty.getRecord()));
        party.setType(parParty.getPartyType().getName());
        party.setTypeCode(parParty.getPartyType().getCode());
    }

    private AbstractItem getItemUnitPacketRef(final NodeId nodeId, final ArrItemPacketRef itemData) {
        final ArrPacket arrPacket = itemData.getPacket();
        Packet packet = new Packet();
        RulPacketType packetType = arrPacket.getPacketType();
        if (packetType != null) {
            packet.setType(packetType.getName());
            packet.setTypeCode(packetType.getCode());
            packet.setTypeShortcut(packetType.getShortcut());
        }
        packet.setStorageNumber(arrPacket.getStorageNumber());
        packet.setState(arrPacket.getState().name());
        return new ItemPacketRef(nodeId, packet);
    }

    private AbstractItem getItemUnitJsonTable(final NodeId nodeId, final ArrItemJsonTable itemData) {
        return new ItemJsonTable(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitFormatedText(final NodeId nodeId, final ArrItemFormattedText itemData) {
        return new ItemText(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitInteger(final NodeId nodeId, final ArrItemInt itemData) {
        return new ItemInteger(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitEnum(final NodeId nodeId, final ArrItemEnum itemData) {
        return new ItemEnum(nodeId, itemData.toString());
    }

    private AbstractItem getItemUnitDecimal(final NodeId nodeId, final ArrItemDecimal itemData) {
        return new ItemDecimal(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitCoordinates(final NodeId nodeId, final ArrItemCoordinates itemData) {
        return new ItemCoordinates(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitText(final NodeId nodeId, final ArrItemText itemData) {
        return new ItemText(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitdate(final NodeId nodeId, final ArrItemUnitdate itemData) {
        UnitDate data = mapper.map(itemData, UnitDate.class);
        return new ItemUnitdate(nodeId, data);
    }

    private AbstractItem getItemUnitid(final NodeId nodeId, final ArrItemUnitid itemData) {
        return new ItemUnitId(nodeId, itemData.getValue());
    }

    /**
     * Načtení požadovaných uzlů (JP) společně s daty.
     *
     * @param output  výstup
     * @param nodeIds seznam identifikátorů uzlů, které načítáme
     * @return mapa - klíč identifikátor uzlu, hodnota uzel
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Node> loadNodes(final Output output, final Collection<NodeId> nodeIds) {
        Map<Integer, Node> mapNodes = nodeIds.stream().map(nodeId -> getNode(nodeId, output)).collect(Collectors.toMap(Node::getArrNodeId, Function.identity()));

        fillItems(output, nodeIds, mapNodes);
        fillRecords(output, nodeIds, mapNodes);

        return mapNodes;
    }

    /**
     * Načtení rejstříkových hesel k jednotkám popisu.
     *
     * @param output    output pod který node patří
     * @param nodeIds   seznam identifikátorů uzlů, které načítáme
     * @param mapNodes  mapa uzlů, do kterých ukládáme
     */
    private void fillRecords(final Output output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
        Map<Integer, List<RegRecord>> recordsByNode = registryService.findByNodes(mapNodes.keySet());
        for (NodeId nodeId : nodeIds) {
            int arrNodeId = nodeId.getArrNodeId();
            List<RegRecord> regRecords = recordsByNode.get(arrNodeId);
            List<Record> records;
            if (regRecords == null) {
                records = Collections.<Record>emptyList();
            } else {
                records = regRecords.stream().map(regRecord -> createRecord(output, regRecord)).collect(Collectors.toList());
            }
            Node node = mapNodes.get(arrNodeId);
            node.setRecords(records);
        }
    }

    /**
     * Načtení hodnot atributu k jednotkám popisu.
     *
     * @param output   output pod který node patří
     * @param nodeIds  seznam identifikátorů uzlů, které načítáme
     * @param mapNodes mapa uzlů, do kterých ukládáme
     */
    private void fillItems(final Output output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
        Map<Integer, List<ArrDescItem>> descItemsByNode = arrangementService.findByNodes(mapNodes.keySet());

        List<ArrDescItem> allDescItems = new LinkedList<>();
        for (List<ArrDescItem> items : descItemsByNode.values()) {
            allDescItems.addAll(items);
        }

        ObjectListIterator<ArrDescItem> iterator = new ObjectListIterator<>(allDescItems);

        while (iterator.hasNext()) {
            List<ArrDescItem> descItems = iterator.next();
            itemService.loadData(descItems);
        }

        Map<Integer, ItemType> itemTypeMap = new HashMap<>();
        Map<Integer, ItemSpec> itemSpecMap = new HashMap<>();

        for (NodeId nodeId : nodeIds) {
            int arrNodeId = nodeId.getArrNodeId();
            List<ArrDescItem> descItems = descItemsByNode.get(arrNodeId);

            List<Item> items;
            if (descItems == null) {
                items = Collections.<Item>emptyList();
            } else {
                items = descItems.stream()
                        .map(arrDescItem -> {
                            AbstractItem itemByType = getItemByType(output, nodeId, arrDescItem);

                            RulItemSpec rulItemSpec = arrDescItem.getItemSpec();
                            if (rulItemSpec != null) {
                                Integer itemSpecId = rulItemSpec.getItemSpecId();
                                ItemSpec itemSpec = itemSpecMap.get(itemSpecId);
                                if (itemSpec == null) {
                                    itemSpec = createItemSpec(rulItemSpec);
                                    itemSpecMap.put(itemSpecId, itemSpec);
                                }
                                itemByType.setSpecification(itemSpec);
                            }

                            RulItemType rulItemType = arrDescItem.getItemType();
                            Integer itemTypeId = rulItemType.getItemTypeId();
                            ItemType itemType = itemTypeMap.get(itemTypeId);
                            if (itemType == null) {
                                itemType = createItemType(rulItemType);
                                itemTypeMap.put(itemTypeId, itemType);
                            }
                            itemByType.setType(itemType);

                            return itemByType;
                        }).collect(Collectors.toList());
            }
            items.sort((i1,i2) -> (i1.compareToItemViewOrderPosition(i2)));
            Node node = mapNodes.get(arrNodeId);
            node.setItems(items);
        }
    }


}
