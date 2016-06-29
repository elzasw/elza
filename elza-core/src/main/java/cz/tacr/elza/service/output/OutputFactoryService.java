package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
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
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.print.Fund;
import cz.tacr.elza.print.ItemSpec;
import cz.tacr.elza.print.ItemType;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.UnitDateText;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
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
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private PartyGroupRepository partyGroupRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OutputService outputService;

    // TODO - JavaDoc - Lebeda
    // Factory metoda pro vytvoření logické struktury Output struktury
    @Bean
    @Scope("prototype")
    public Output createOutput(final ArrOutput arrOutput) {
        // naplnit output
        final Output output = new Output(arrOutput);
        output.setName(arrOutput.getOutputDefinition().getName());
        output.setInternal_code(arrOutput.getOutputDefinition().getInternalCode());
        output.setTypeCode(arrOutput.getOutputDefinition().getOutputType().getCode());

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
        output.getNodes().add(getNodeWithItems(arrFundRootNode, output, arrFundRootNode));

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
//        final RecordType recordType = new RecordType(); // TODO Lebeda -
//        partyRecord.setType();
//        partyRecord.setType();
//        partyGroup.setRecord(partyRecord); // TODO Lebeda -

        institution.setPartyGroup(partyGroup); // TODO Lebeda -

        fund.setInstitution(institution);

        // TODO Lebeda - records

        // zařadit items přímo přiřazené na output
        final List<ArrOutputItem> outputItems = outputService.getOutputItems(arrFundVersion, arrOutput.getOutputDefinition());
        outputItems.stream().forEach(arrOutputItem -> {
            final ArrItem arrItem = itemRepository.findOne(arrOutputItem.getItemId());
            final AbstractItem item = getItem(arrItem, output, null);
//            item.setPosition(arrDescItem.getPosition()); TODO Lebeda - Kde vzít pozici itemu?
            output.getItems().add(item);
        });

        // zařadit strom nodes
        final Set<Node> nodes = new LinkedHashSet<>(output.getNodes()); // jako první použít již zařazený rootnode
        outputService.getNodesForOutput(arrOutput).stream()
                .sorted((o1, o2) -> new CompareToBuilder()
                        .append(o1.getNodeId(), o2.getNodeId())
                        .toComparison())
                .forEach(arrNode -> nodes.addAll(getParentNodeByNode(arrNode, output)));
        output.getNodes().clear(); // nahradit novou kolekcí
        output.getNodes().addAll(nodes);

        return output;
    }

    // TODO - JavaDoc - Lebeda
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

    // TODO - JavaDoc - Lebeda
    private UnitDateText createUnitDateText(ParUnitdate from) {
        UnitDateText dateFrom = null;
        if (StringUtils.isNotBlank(from.getFormat())) { // musí být nastaven formát
            try {
                dateFrom = new UnitDateText();
                dateFrom.setValueText(UnitDateConvertor.convertToString(from));
            } catch (Exception e) {
                // TODO Lebeda - ošetřit exception
                logger.error("Nepodařilo se datum " + from.toString() + " převést na textovou reprezentaci.", e);
            }
        }
        return dateFrom;
    }

    // TODO - JavaDoc - Lebeda
    private Set<Node> getParentNodeByNode(ArrNode arrNode, Output output) {
        final Set<Node> nodes = new LinkedHashSet<>();
        // získat node vč potomků a atributů
        final Set<Node> nodeWithChildernAndAttributes = createNodeWithChildernAndAttributes(arrNode, output);

        // získat seznam rodičů node a zařadit
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(arrNode, arrFundVersion);
        nodes.addAll(levelList.stream().sorted(Collections.reverseOrder())
                .map(arrLevel -> getNodeWithItems(arrNode, output, output.getFund().getRootNode().getArrNode()))
                .collect(Collectors.toList()));

        // zařadit vlastní uzel a potomky
        nodes.addAll(nodeWithChildernAndAttributes);

        return nodes;
    }

    // TODO - JavaDoc - Lebeda
    private Set<Node> createNodeWithChildernAndAttributes(ArrNode arrNode, Output output) {
        final Set<Node> nodes = new LinkedHashSet<>();
        final Node nodeWithAttributes = getNodeWithItems(arrNode, output, output.getFund().getRootNode().getArrNode());
        nodes.add(nodeWithAttributes);

        // získat childern
        final ArrNode rootArrNode = output.getFund().getRootNode().getArrNode();
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();
        Assert.notNull(rootArrNode, "Nelze určit kořenový node výstupu.");
        final List<ArrLevel> allChildrenByNode = levelRepository.findAllChildrenByNode(arrNode, arrChange);
        allChildrenByNode.stream()
                .forEach(arrLevel -> {
                    final ArrNode arrNode1 = arrLevel.getNode();
                    nodes.addAll(createNodeWithChildernAndAttributes(arrNode1, output)); // rekurzivně přidat node
                });

        return nodes;
    }

    // TODO - JavaDoc - Lebeda
    private Node getNodeWithItems(ArrNode arrNode, Output output, @NotNull final ArrNode rootArrNode) {
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();
        Assert.notNull(rootArrNode, "Nelze určit kořenový node výstupu.");

        final ArrLevel arrLevel = levelRepository.findNodeInRootTreeByNodeId(arrNode, rootArrNode, arrChange);
        final Node node = new Node(output, arrNode, arrLevel);
        node.setPosition(arrLevel.getPosition());

        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(arrNode, arrFundVersion);
        node.setDepth(levelList.size()+1);

//        node.getRecords().add(); // TODO Lebeda - plnit návazné records

        // items navázané k node
        final List<ArrDescItem> descItems = arrangementService.getDescItems(arrFundVersion, arrNode);
        descItems.stream()
                .sorted((o1, o2) -> o1.getPosition().compareTo(o2.getPosition()))
                .forEach(arrDescItem -> {
                    final ArrItem arrItem = itemRepository.findOne(arrDescItem.getItemId());

                    final AbstractItem item = getItem(arrItem, output, node);
                    item.setPosition(arrDescItem.getPosition());

                    node.getItems().add(item);
                });

        return node;
    }

    // TODO - JavaDoc - Lebeda
    private AbstractItem getItem(ArrItem arrItem, Output output, Node node) {
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
        final String valueTable = rulItemType.getDataType().getStorageTable();
        itemType.setShortcut(rulItemType.getShortcut());
        itemType.setDescription(rulItemType.getDescription());
        itemType.setCode(rulItemType.getCode());

        final AbstractItem item;
        final ArrItemData itemData = arrItem.getItem();
        item = getItemByType(output, node, arrItem, itemData);
        item.setType(itemType);
        item.setSpecification(itemSpec);

        return item;
    }

    // TODO - JavaDoc - Lebeda
    private AbstractItem getItemByType(Output output, Node node, ArrItem arrItem, ArrItemData itemData) {
        AbstractItem item;
        if (itemData instanceof ArrItemUnitid) {
            item = getItemUnitid(output, node, arrItem, (ArrItemUnitid) itemData);
        } else if (itemData instanceof ArrItemUnitdate) {
            item = getItemUnitdate(output, node, arrItem, (ArrItemUnitdate) itemData);
        } else if (itemData instanceof ArrItemText) {
            item = getItemUnitText(output, node, arrItem, (ArrItemText) itemData);
        } else if (itemData instanceof ArrItemString) {
            item = getItemUnitString(output, node, arrItem, (ArrItemString) itemData);
        } else if (itemData instanceof ArrItemRecordRef) {
            item = getItemUnitRecordRef(output, node, arrItem, (ArrItemRecordRef) itemData);
        } else if (itemData instanceof ArrItemPartyRef) {
            item = getItemUnitPartyRef(output, node, arrItem, (ArrItemPartyRef) itemData);
        } else if (itemData instanceof ArrItemPacketRef) {
            item = getItemUnitPacketRef(output, node, arrItem, (ArrItemPacketRef) itemData);
        } else if (itemData instanceof ArrItemJsonTable) {
            item = getItemUnitJsonTable(output, node, arrItem, (ArrItemJsonTable) itemData);
        } else if (itemData instanceof ArrItemInt) {
            item = getItemUnitInteger(output, node, arrItem, (ArrItemInt) itemData);
        } else if (itemData instanceof ArrItemFormattedText) {
            item = getItemUnitFormatedText(output, node, arrItem, (ArrItemFormattedText) itemData);
//        } else if (itemData instanceof ArrItemFile) {
//            item = getItemUnitid(output, node, arrItem, (ArrItemFile) itemData);
//            item = new ItemFile(arrItem, output, node, value);
//             TODO Lebeda - implementovat pro jaký zdrojový typ???
        } else if (itemData instanceof ArrItemEnum) {
            item = getItemUnitEnum(output, node, arrItem, (ArrItemEnum) itemData);
        } else if (itemData instanceof ArrItemDecimal) {
            item = getItemUnitDecimal(output, node, arrItem, (ArrItemDecimal) itemData);
        } else if (itemData instanceof ArrItemCoordinates) {
            item = getItemUnitCoordinates(output, node, arrItem, (ArrItemCoordinates) itemData);
        } else {
            logger.warn("Neznámý datový typ hodnoty Item ({}) je zpracován jako string.", itemData.getClass().getName());
            item = new ItemString(arrItem, output, node, itemData.toString());
        }
        return item;
    }

    private AbstractItem getItemUnitString(Output output, Node node, ArrItem arrItem, ArrItemString itemData) {
        return new ItemString(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitRecordRef(Output output, Node node, ArrItem arrItem, ArrItemRecordRef itemData) {
        return new ItemRecordRef(arrItem, output, node, getRecord(output, node, itemData));
    }

    @Bean
    @Scope("prototype")
    private Record getRecord(Output output, Node node, ArrItemRecordRef itemData) {
        Record record = new Record(output, node, itemData.getRecord());
//        RecordType recordType = new RecordType();
//        final RegRegisterType registerType = itemData.getRecord().getRegisterType();
//        recordType.setName(registerType.getName());
//        recordType.setCode(registerType.getCode());
//        recordType.setCountRecords(registerType.);
//        private String name;
//            private String code;
//            private Integer countRecords;
//            private Integer countDirectRecords;
//        record.setType(recordType);
        // TODO Lebeda - práce s recordtypem
//        private RecordType type;
        record.setRecord(itemData.getRecord().getRecord());
        record.setCharacteristics(itemData.getRecord().getCharacteristics());
        itemData.getRecord().getVariantRecordList().stream().forEach(regVariantRecord -> record.getVariantRecords().add(regVariantRecord.getRecord()));
        return record;
    }

    private AbstractItem getItemUnitPartyRef(Output output, Node node, ArrItem arrItem, ArrItemPartyRef itemData) {
        final ParParty parParty = itemData.getParty();
        Party party = new Party();
        party.setPreferredName(createPartyName(parParty.getPreferredName()));
        partyNameRepository.findByParty(parParty).stream()
                .filter(parPartyName -> !parPartyName.getPartyNameId().equals(parParty.getPreferredName().getPartyNameId())) // kromě preferovaného jména
                .forEach(parPartyName -> party.getNames().add(createPartyName(parPartyName)));
        party.setHistory(parParty.getHistory());
        party.setSourceInformation(parParty.getSourceInformation());
        party.setCharacteristics(parParty.getCharacteristics());
//            private Record record; TODO Lebeda - dopsat konverzi
//            private UnitDateText unitdateFrom; TODO Lebeda -
//            private UnitDateText unitdateTo; TODO Lebeda -
        party.setType(parParty.getPartyType().getName());
        party.setTypeCode(parParty.getPartyType().getCode());

        return new ItemPartyRef(arrItem, output, node, party);
    }

    private AbstractItem getItemUnitPacketRef(Output output, Node node, ArrItem arrItem, ArrItemPacketRef itemData) {
        final ArrPacket arrPacket = itemData.getPacket();
        Packet packet = new Packet();
        packet.setType(arrPacket.getPacketType().getName());
        packet.setTypeCode(arrPacket.getPacketType().getCode());
        packet.setTypeShortcut(arrPacket.getPacketType().getShortcut());
        packet.setStorageNumber(arrPacket.getStorageNumber());
        packet.setState(arrPacket.getState().name());
        return new ItemPacketRef(arrItem, output, node, packet);
    }

    private AbstractItem getItemUnitJsonTable(Output output, Node node, ArrItem arrItem, ArrItemJsonTable itemData) {
        return new ItemJsonTable(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitFormatedText(Output output, Node node, ArrItem arrItem, ArrItemFormattedText itemData) {
        return new ItemText(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitInteger(Output output, Node node, ArrItem arrItem, ArrItemInt itemData) {
        return new ItemInteger(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitEnum(Output output, Node node, ArrItem arrItem, ArrItemEnum itemData) {
        return new ItemEnum(arrItem, output, node, itemData.toString());
    }

    private AbstractItem getItemUnitDecimal(Output output, Node node, ArrItem arrItem, ArrItemDecimal itemData) {
        return new ItemDecimal(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitCoordinates(Output output, Node node, ArrItem arrItem, ArrItemCoordinates itemData) {
        return new ItemCoordinates(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitText(Output output, Node node, ArrItem arrItem, ArrItemText itemData) {
        return new ItemText(arrItem, output, node, itemData.getValue());
    }

    private AbstractItem getItemUnitdate(Output output, Node node, ArrItem arrItem, ArrItemUnitdate itemData) {
        UnitDate data = new UnitDate();
        data.setValueText(itemData.toString());
        // TODO Lebeda - Jak korektně převést strukturované datum
        return new ItemUnitdate(arrItem, output, node, data);
    }

    private AbstractItem getItemUnitid(Output output, Node node, ArrItem arrItem, ArrItemUnitid itemData) {
        return new ItemUnitId(arrItem, output, node, itemData.getValue());
    }
}
