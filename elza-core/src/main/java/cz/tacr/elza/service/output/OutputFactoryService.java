package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
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
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.print.Fund;
import cz.tacr.elza.print.ItemSpec;
import cz.tacr.elza.print.ItemType;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.RecordType;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.UnitDateText;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.item.ItemInteger;
import cz.tacr.elza.print.item.ItemJsonTable;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.print.item.ItemPartyRef;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemText;
import cz.tacr.elza.print.item.ItemUnitId;
import cz.tacr.elza.print.item.ItemUnitdate;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyName;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.OutputService;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.MappingDirection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory pro vytvoření struktury pro výstupy
 *
 * @author Martin Lebeda
 * @since 03.05.2016
 */

@Service
public class OutputFactoryService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private MapperFactory mapperFactory;
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

//    @Autowired
//    private ItemRepository itemRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    public OutputFactoryService() {
        // inicializace mapperů
        this.mapperFactory = new DefaultMapperFactory.Builder().build();

        mapperFactory.classMap(ArrItemUnitdate.class, UnitDate.class)
                .customize(new CustomMapper<ArrItemUnitdate, UnitDate>() {
                    @Override
                    public void mapAtoB(ArrItemUnitdate arrItemUnitdate, UnitDate unitDate, MappingContext context) {
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
    @Bean
    @Scope("prototype")
    public Output createOutput(final ArrOutput arrOutput) {
        // naplnit output
        final Output output = outputGeneratorFactory.getOutput(arrOutput);
        output.setName(arrOutput.getOutputDefinition().getName());
        output.setInternal_code(arrOutput.getOutputDefinition().getInternalCode());
        output.setTypeCode(arrOutput.getOutputDefinition().getOutputType().getCode());
        output.setType(arrOutput.getOutputDefinition().getOutputType().getName());

        // fund
        final ArrFund arrFund = arrOutput.getOutputDefinition().getFund();
        final ArrFundVersion arrFundVersion = arrFund.getVersions().parallelStream()
                .filter(fundVersion -> fundVersion.getLockChange() == null)
                .findFirst().orElse(null);
        Assert.notNull(arrFundVersion, "Musí existovat právě jedna odemčená verze.");

        Fund fund = new Fund(output, arrFund, arrFundVersion);
        fund.setName(arrFund.getName());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(arrFundVersion.getDateRange());
        fund.setInternalCode(arrFund.getInternalCode());
        output.setFund(fund);

        // zařadit do výstupu rootNode fundu
        final ArrNode arrFundRootNode = arrFundVersion.getRootNode();
        final Integer rootArrNodeId = arrFundRootNode.getNodeId();
        output.getNodesMap().put(rootArrNodeId, getNodeId(rootArrNodeId, output, rootArrNodeId, null));

        // plnit institution
        final ParInstitution arrFundInstitution = arrFund.getInstitution();
        final Institution institution = new Institution();
        institution.setTypeCode(arrFundInstitution.getInstitutionType().getCode());
        institution.setType(arrFundInstitution.getInstitutionType().getName());
        institution.setCode(arrFundInstitution.getInternalCode());

        // partyGroup k instituci
        final PartyGroup partyGroup = new PartyGroup();
        final ParParty parParty = arrFundInstitution.getParty();
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
        partyGroup.setUnitdateFrom(createUnitDateText(parParty.getFrom()));
        partyGroup.setUnitdateTo(createUnitDateText(parParty.getTo()));
        partyGroup.setPreferredName(createPartyName(parParty.getPreferredName()));
        partyNameRepository.findByParty(parParty).stream()
                .filter(parPartyName -> !parPartyName.getPartyNameId().equals(parParty.getPreferredName().getPartyNameId())) // kromě preferovaného jména
                .forEach(parPartyName -> partyGroup.getNames().add(createPartyName(parPartyName)));


        final RegRecord parPartyRecord = parParty.getRecord();
        Record partyRecord = new Record(output, null, parPartyRecord);
        partyRecord.setRecord(parPartyRecord.getRecord());
        partyRecord.setCharacteristics(parPartyRecord.getCharacteristics());
        parPartyRecord.getVariantRecordList().stream().forEach(regVariantRecord -> partyRecord.getVariantRecords().add(regVariantRecord.getRecord()));

        partyRecord.setType(getRecordTypeByPartyRecord(output, parPartyRecord));
        partyGroup.setRecord(partyRecord);

        institution.setPartyGroup(partyGroup);

        fund.setInstitution(institution);

        // zařadit items přímo přiřazené na output
        final List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(arrFundVersion, arrOutput.getOutputDefinition());
        outputItems.stream().forEach(arrOutputItem -> {
            final ArrItem arrItem = itemService.loadDataById(arrOutputItem.getItemId());
            final AbstractItem item = getItem(arrItem.getItemId(), output, null);
            item.setPosition(arrItem.getPosition());
            output.getItems().add(item);
        });

        // zařadit strom nodes
        // final Set<Node> nodes = new LinkedHashSet<>(output.getNodes()); // jako první použít již zařazený rootnode
        outputService.getNodesForOutput(arrOutput).stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getNodeId(), o2.getNodeId())
                        .toComparison())
                .forEach(arrNode -> addParentNodeByNode(arrNode.getNodeId(), output));
        // output.getNodesMap().putAll(nodes);

        return output;
    }

    /**
     * factory metoda konvertující objekty
     */
    private PartyName createPartyName(ParPartyName parPartyPreferredName) {
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
     * @param arrNodeId node přímo přiřazený k outputu
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void addParentNodeByNode(Integer arrNodeId, Output output) {
//        final Set<Node> nodes = new LinkedHashSet<>();
        // získat node vč potomků a atributů
        getNodeIdWithChildern(arrNodeId, output, null);

        // získat seznam rodičů node a zařadit
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(nodeRepository.findOne(arrNodeId), arrFundVersion);
        levelList.stream()
                .map(arrLevel -> getNodeId(arrNodeId, output, output.getFund().getRootNodeId().getArrNodeId(), null))
                .forEach(node -> output.getNodesMap().put(node.getArrNodeId(), node));

        // vytvořit návaznosti směrem k rootu
        final List<Integer> parentIds = levelList.stream()
                .map(arrLevel -> arrLevel.getNode().getNodeId())
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(parentIds)) {
            // nastavit na node první položku parentlistu
            output.getNodesMap().get(arrNodeId).setParentNodeId(parentIds.get(0)); // parent
            output.getNodesMap().get(parentIds.get(0)).getChildren().add(output.getNodesMap().get(arrNodeId)); // child
            
            // nastavit další položky v parentlistu až ke kořenu
            for (int i = 0; i < (parentIds.size() - 1); i++) {
                final NodeId nodeIdChild = output.getNodesMap().get(parentIds.get(i));
                final NodeId nodeIdParent = output.getNodesMap().get(parentIds.get(i + 1));
                nodeIdChild.setParentNodeId(nodeIdParent.getArrNodeId());
                nodeIdParent.getChildren().add(nodeIdChild);
            }
        }

        // zařadit vlastní uzel a potomky
//        nodes.addAll(nodeWithChildernAndAttributes);
//        return nodes;
    }

    /**
     * Vytvoří node vč. celého stromu potomků.
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrNodeId zdrojový node
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void getNodeIdWithChildern(Integer arrNodeId, Output output, Integer parentNodeId) {
        final NodeId nodeId = getNodeId(arrNodeId, output, output.getFund().getRootNodeId().getArrNodeId(), parentNodeId);
        output.getNodesMap().put(arrNodeId, nodeId);

        // získat childern
        final ArrNode rootArrNode = nodeRepository.findOne(output.getFund().getRootNodeId().getArrNodeId());
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();
        Assert.notNull(rootArrNode, "Nelze určit kořenový node výstupu.");
        final List<ArrLevel> allChildrenByNode = levelRepository.findAllChildrenByNode(nodeRepository.findOne(arrNodeId), arrChange);
        allChildrenByNode.stream()
                .forEach(arrLevel -> {
                    final Integer childNodeId = arrLevel.getNode().getNodeId();
                    getNodeIdWithChildern(childNodeId, output, arrNodeId);
                    nodeId.getChildren().add(output.getNodesMap().get(childNodeId));
                });
    }

    /**
     * Vytvoří node vč. celého stromu potomků.
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrNodeId     zdrojový node
     * @param output      výstup, ke kterému se budou nody zařazovat
     * @param rootArrNodeId kořenový node výstupu (definovaný jako kořenový ve fundu v outputu)
     * @param parentNodeId ID pro odkaz na nadřazený uzel
     * @return node vč. items
     */
    private NodeId getNodeId(Integer arrNodeId, Output output, @NotNull final Integer rootArrNodeId, Integer parentNodeId) {
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();
        Assert.notNull(rootArrNodeId, "Nelze určit kořenový node výstupu.");
        final ArrNode arrNode = nodeRepository.findOne(arrNodeId);
        final ArrNode rootArrNode = nodeRepository.findOne(rootArrNodeId);

        final ArrLevel arrLevel = levelRepository.findNodeInRootTreeByNodeId(arrNode, rootArrNode, arrChange);
        final NodeId nodeId = outputGeneratorFactory.getNodeId(output, arrNodeId, arrLevel.getLevelId(), parentNodeId);
        nodeId.setPosition(arrLevel.getPosition());

        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(arrNode, arrFundVersion);
        nodeId.setDepth(levelList.size() + 1);
        getRecordByNodeId(output, nodeId);

        return nodeId;
    }

    /**
     * @param nodeId ID Požadovaného node (odpovídá ID arrNode)
     * @param output output pod který node patří
     * @return Node pro tisk
     */
    public Node getNode(NodeId nodeId, Output output) {
        return outputGeneratorFactory.getNode(nodeId, output);
    }

    /**
     * Vrací registers navázané přímo k node.
     *
     * @param output output na který jsou registers navázané
     * @param nodeId node na které jsou registers navázané
     * @return seznam registers navázaných přímo k node
     */
    @Transactional(readOnly = true)
    public List<Record> getRecordByNodeId(Output output, NodeId nodeId) {
        return nodeRegisterRepository.findByNode(nodeRepository.findOne(nodeId.getArrNodeId())).stream()
                .map(ArrNodeRegister::getRecord)
                .map(regRecord -> getRecordByNode(output, nodeId, regRecord))
                .collect(Collectors.toList());
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
    public AbstractItem getItem(Integer arrItemId, Output output, NodeId nodeId) {
        final ArrItem arrItem = itemService.loadDataById(arrItemId);
        final RulItemType rulItemType = arrItem.getItemType();
        final RulItemSpec rulItemSpec = arrItem.getItemSpec();

        // založit itemSpec
        final ItemSpec itemSpec = new ItemSpec();
        if (rulItemSpec != null) {
            itemSpec.setName(rulItemSpec.getName());
            itemSpec.setShortcut(rulItemSpec.getShortcut());
            itemSpec.setDescription(rulItemSpec.getDescription());
            itemSpec.setCode(rulItemSpec.getCode());
        }

        // založit itemType
        final ItemType itemType = new ItemType();
        itemType.setName(rulItemType.getName());
        itemType.setDataType(rulItemType.getDataType().getCode());
        itemType.setShortcut(rulItemType.getShortcut());
        itemType.setDescription(rulItemType.getDescription());
        itemType.setCode(rulItemType.getCode());

        final AbstractItem item;
        final ArrItemData itemData = arrItem.getItem();
        item = getItemByType(output, nodeId, arrItem, itemData);
        item.setType(itemType);
        item.setSpecification(itemSpec);

        return item;
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItem  zdrojový item
     * @param itemData zdrojová data k itemu
     * @param output   výstup, ke kterému se budou items zařazovat
     * @param nodeId     node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    private AbstractItem getItemByType(Output output, NodeId nodeId, ArrItem arrItem, ArrItemData itemData) {
        AbstractItem item;
        if (itemData instanceof ArrItemUnitid) {
            item = getItemUnitid(output, nodeId, arrItem, (ArrItemUnitid) itemData);
        } else if (itemData instanceof ArrItemUnitdate) {
            item = getItemUnitdate(output, nodeId, arrItem, (ArrItemUnitdate) itemData);
        } else if (itemData instanceof ArrItemText) {
            item = getItemUnitText(output, nodeId, arrItem, (ArrItemText) itemData);
        } else if (itemData instanceof ArrItemString) {
            item = getItemUnitString(output, nodeId, arrItem, (ArrItemString) itemData);
        } else if (itemData instanceof ArrItemRecordRef) {
            item = getItemUnitRecordRef(output, nodeId, arrItem, (ArrItemRecordRef) itemData);
        } else if (itemData instanceof ArrItemPartyRef) {
            item = getItemUnitPartyRef(output, nodeId, arrItem, (ArrItemPartyRef) itemData);
        } else if (itemData instanceof ArrItemPacketRef) {
            item = getItemUnitPacketRef(output, nodeId, arrItem, (ArrItemPacketRef) itemData);
        } else if (itemData instanceof ArrItemJsonTable) {
            item = getItemUnitJsonTable(output, nodeId, arrItem, (ArrItemJsonTable) itemData);
        } else if (itemData instanceof ArrItemInt) {
            item = getItemUnitInteger(output, nodeId, arrItem, (ArrItemInt) itemData);
        } else if (itemData instanceof ArrItemFormattedText) {
            item = getItemUnitFormatedText(output, nodeId, arrItem, (ArrItemFormattedText) itemData);
        } else if (itemData instanceof ArrItemFileRef) {
            item = getItemFile(output, nodeId, arrItem, (ArrItemFileRef) itemData);
        } else if (itemData instanceof ArrItemEnum) {
            item = getItemUnitEnum(output, nodeId, arrItem, (ArrItemEnum) itemData);
        } else if (itemData instanceof ArrItemDecimal) {
            item = getItemUnitDecimal(output, nodeId, arrItem, (ArrItemDecimal) itemData);
        } else if (itemData instanceof ArrItemCoordinates) {
            item = getItemUnitCoordinates(output, nodeId, arrItem, (ArrItemCoordinates) itemData);
        } else {
            logger.warn("Neznámý datový typ hodnoty Item ({}) je zpracován jako string.", itemData.getClass().getName());
            item = new ItemString(arrItem, output, nodeId, itemData.toString());
        }
        return item;
    }

    private AbstractItem getItemFile(Output output, NodeId nodeId, ArrItem arrItem, ArrItemFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFile itemFile = new ItemFile(arrItem, output, nodeId, arrFile);
        itemFile.setName(arrFile.getName());
        itemFile.setFileName(arrFile.getFileName());
        itemFile.setFileSize(arrFile.getFileSize());
        itemFile.setMimeType(arrFile.getMimeType());
        itemFile.setPagesCount(arrFile.getPagesCount());

        return itemFile;
    }

    private AbstractItem getItemUnitString(Output output, NodeId nodeId, ArrItem arrItem, ArrItemString itemData) {
        return new ItemString(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitRecordRef(Output output, NodeId nodeId, ArrItem arrItem, ArrItemRecordRef itemData) {
        final Record record = getRecordByItem(output, nodeId, itemData);
        return new ItemRecordRef(arrItem, output, nodeId, record);
    }

    private Record getRecordByItem(Output output, NodeId nodeId, ArrItemRecordRef itemData) {
        Record record = getRecord(output, nodeId, itemData.getRecord());
        record.setType(getRecordTypeByItem(output, itemData));
        return record;
    }

    private Record getRecordByNode(Output output, NodeId nodeId, RegRecord regRecord) {
        Record record =  getRecord(output, nodeId, regRecord);
        record.setType(getRecordTypeByNode(output, regRecord.getRegisterType()));
        return record;
    }

    private Record getRecordByParty(Output output, RegRecord partyRecord) {
        Record record = getRecord(output, null, partyRecord);
        record.setType(getRecordTypeByPartyRecord(output, partyRecord));
        return record;
    }

    private Record getRecord(@NotNull Output output, NodeId nodeId, @NotNull final RegRecord regRecord) {
        Record record = outputGeneratorFactory.getRecord(output, nodeId, regRecord);
        record.setRecord(regRecord.getRecord());
        record.setCharacteristics(regRecord.getCharacteristics());
        regRecord.getVariantRecordList().stream().forEach(regVariantRecord -> record.getVariantRecords().add(regVariantRecord.getRecord()));
        return record;
    }

    private RecordType getRecordTypeByItem(Output output, ArrItemRecordRef itemData) {
        final RegRegisterType registerType = itemData.getRecord().getRegisterType();
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
    }

    private RecordType getRecordTypeByNode(Output output, RegRegisterType registerType) {
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountDirectRecords(recordType.getCountDirectRecords() + 1);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
        }

    private RecordType getRecordTypeByPartyRecord(Output output, RegRecord parPartyRecord) {
        final RegRegisterType registerType = parPartyRecord.getRegisterType();
        final RecordType recordType = getRecordType(output, registerType);
        recordType.setCountRecords(recordType.getCountDirectRecords() + 1);
        return recordType;
    }

    private RecordType getRecordType(Output output, RegRegisterType registerType) {
        RecordType recordType = output.getRecordTypes().get(registerType.getCode());
        if (recordType == null) {
            recordType = mapper.map(registerType, RecordType.class);
            output.getRecordTypes().put(registerType.getCode(), recordType);
        }
        return recordType;
    }


    private AbstractItem getItemUnitPartyRef(Output output, NodeId nodeId, ArrItem arrItem, ArrItemPartyRef itemData) {
        final ParParty parParty = itemData.getParty();
        Party party = new Party();
        party.setPreferredName(createPartyName(parParty.getPreferredName()));
        partyNameRepository.findByParty(parParty).stream()
                .filter(parPartyName -> !parPartyName.getPartyNameId().equals(parParty.getPreferredName().getPartyNameId())) // kromě preferovaného jména
                .forEach(parPartyName -> party.getNames().add(createPartyName(parPartyName)));
        party.setHistory(parParty.getHistory());
        party.setSourceInformation(parParty.getSourceInformation());
        party.setCharacteristics(parParty.getCharacteristics());
        party.setRecord(getRecordByParty(output, parParty.getRecord()));
        party.setUnitdateFrom(createUnitDateText(parParty.getFrom()));
        party.setUnitdateTo(createUnitDateText(parParty.getTo()));
        party.setType(parParty.getPartyType().getName());
        party.setTypeCode(parParty.getPartyType().getCode());

        return new ItemPartyRef(arrItem, output, nodeId, party);
    }

    private AbstractItem getItemUnitPacketRef(Output output, NodeId nodeId, ArrItem arrItem, ArrItemPacketRef itemData) {
        final ArrPacket arrPacket = itemData.getPacket();
        Packet packet = new Packet();
        packet.setType(arrPacket.getPacketType().getName());
        packet.setTypeCode(arrPacket.getPacketType().getCode());
        packet.setTypeShortcut(arrPacket.getPacketType().getShortcut());
        packet.setStorageNumber(arrPacket.getStorageNumber());
        packet.setState(arrPacket.getState().name());
        return new ItemPacketRef(arrItem, output, nodeId, packet);
    }

    private AbstractItem getItemUnitJsonTable(Output output, NodeId nodeId, ArrItem arrItem, ArrItemJsonTable itemData) {
        return new ItemJsonTable(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitFormatedText(Output output, NodeId nodeId, ArrItem arrItem, ArrItemFormattedText itemData) {
        return new ItemText(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitInteger(Output output, NodeId nodeId, ArrItem arrItem, ArrItemInt itemData) {
        return new ItemInteger(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitEnum(Output output, NodeId nodeId, ArrItem arrItem, ArrItemEnum itemData) {
        return new ItemEnum(arrItem, output, nodeId, itemData.toString());
    }

    private AbstractItem getItemUnitDecimal(Output output, NodeId nodeId, ArrItem arrItem, ArrItemDecimal itemData) {
        return new ItemDecimal(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitCoordinates(Output output, NodeId nodeId, ArrItem arrItem, ArrItemCoordinates itemData) {
        return new ItemCoordinates(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitText(Output output, NodeId nodeId, ArrItem arrItem, ArrItemText itemData) {
        return new ItemText(arrItem, output, nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitdate(Output output, NodeId nodeId, ArrItem arrItem, ArrItemUnitdate itemData) {
        UnitDate data = mapper.map(itemData, UnitDate.class);
        return new ItemUnitdate(arrItem, output, nodeId, data);
    }

    private AbstractItem getItemUnitid(Output output, NodeId nodeId, ArrItem arrItem, ArrItemUnitid itemData) {
        return new ItemUnitId(arrItem, output, nodeId, itemData.getValue());
    }
}
