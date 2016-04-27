package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemDecimal;
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPacketRef;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.service.vo.XmlExportResult;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.utils.XmlUtils;
import cz.tacr.elza.xmlexport.v1.XmlExportConfig;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportUtils;
import cz.tacr.elza.xmlimport.v1.vo.XmlImport;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.AbstractDescItem;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemCoordinates;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemDecimal;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemEnum;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemFormattedText;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemInteger;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemPacketRef;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemPartyRef;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemRecordRef;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemString;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemText;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemUnitDate;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemUnitId;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Fund;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Level;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Packet;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;
import cz.tacr.elza.xmlimport.v1.vo.party.AbstractParty;
import cz.tacr.elza.xmlimport.v1.vo.party.Dynasty;
import cz.tacr.elza.xmlimport.v1.vo.party.Event;
import cz.tacr.elza.xmlimport.v1.vo.party.Institution;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyGroup;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyGroupId;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyName;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyNameComplement;
import cz.tacr.elza.xmlimport.v1.vo.party.Person;
import cz.tacr.elza.xmlimport.v1.vo.party.Relation;
import cz.tacr.elza.xmlimport.v1.vo.party.RoleType;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;
import cz.tacr.elza.xmlimport.v1.vo.record.VariantRecord;

/**
 * Export dat do xml.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @param <LevelWithChildrenIds>
 * @since 22. 4. 2016
 */
@Service
public class XmlExportService {

    @Value("${elza.xmlExport.transformationDir}")
    private String transformationsDirectory;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;

    public XmlExportResult exportData(final XmlExportConfig config) {
        Assert.notNull(config);
        Assert.notNull(config.getVersionId());

        XmlImport xmlImport = exportFund(config.getVersionId(), config.getNodeIds());

        return createExportResult(config, xmlImport);
    }

    private XmlExportResult createExportResult(final XmlExportConfig config, final XmlImport xmlImport) {
        byte[] xmlData = XmlUtils.marshallData(xmlImport, XmlImport.class);
        String fundName = xmlImport.getFund().getName();
        XmlExportResult xmlExportResult = new XmlExportResult(xmlData, fundName);
        String transformationName = config.getTransformationName();
        if (StringUtils.isNotBlank(transformationName)) {
            byte[] transformedData = XmlUtils.transformData(xmlData, transformationName, transformationsDirectory);
            xmlExportResult.setTransformedData(transformedData);
        }


        return xmlExportResult;
    }

    private XmlImport exportFund(final Integer versionId, final Set<Integer> nodeIds) {
        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        ArrFund arrFund = version.getFund();
        // zatím se má exportovat jen otevřená verze
        version = arrangementService.getOpenVersionByFundId(arrFund.getFundId());

        XmlImport xmlImport = new XmlImport();
        xmlImport.setFund(createFund(arrFund, version));

        boolean findParents = false;
        Set<Integer> nodeIdsToExport = new HashSet<>();
        if (CollectionUtils.isEmpty(nodeIds)) {
            nodeIdsToExport.add(version.getRootNode().getNodeId());
        } else {
            findParents = true;
            nodeIdsToExport.addAll(nodeIds);
        }

        Map<Integer, List<Level>> recordLevels = new HashMap<>();
        Map<Integer, List<DescItemRecordRef>> recordDescItems = new HashMap<>();
        Map<Integer, List<DescItemPartyRef>> partyDescItems = new HashMap<>();
        Map<Integer, List<DescItemPacketRef>> packetDescItems = new HashMap<>();

        Level rootLevel = exportNodeTree(nodeIdsToExport, version, recordLevels, recordDescItems, partyDescItems,
                packetDescItems, findParents);
        xmlImport.getFund().setRootLevel(rootLevel);

        Map<Integer, Record> recordMap = exportRecords(xmlImport, recordLevels, recordDescItems);
        exportParties(xmlImport, partyDescItems, recordMap);
        exportPackets(xmlImport, packetDescItems);

        return xmlImport;
    }

    private void exportPackets(final XmlImport xmlImport, final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notNull(xmlImport);
        Assert.notNull(packetDescItems);

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(packetDescItems.keySet());
        while (iterator.hasNext()) {
            List<Integer> packetIds = iterator.next();
            List<ArrPacket> packets = packetRepository.findAll(packetIds);
            for (ArrPacket arrPacket : packets) {
                Packet packet = createPacket(arrPacket);
                updateDescItemPacketReferences(packet, packetDescItems);
            }
        }
    }

    /**
     * Doplní obal do hodnot atributů které na ně odkazují.
     *
     * @param packet obal
     * @param packetDescItems mapa id obalů na hodnoty atributů
     */
    private void updateDescItemPacketReferences(final Packet packet, final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notNull(packet);
        Assert.notNull(packetDescItems);

        List<DescItemPacketRef> descItemPacketRefs = packetDescItems.get(packet.getStorageNumber());
        if (CollectionUtils.isNotEmpty(descItemPacketRefs)) {
            descItemPacketRefs.forEach(diPR -> diPR.setPacket(packet));
        }
    }

    private Packet createPacket(final ArrPacket arrPacket) {
        Packet packet = new Packet();

        packet.setPacketTypeCode(arrPacket.getPacketType().getCode());
        packet.setState(arrPacket.getState());
        packet.setStorageNumber(arrPacket.getStorageNumber());

        return packet;
    }

    private void exportParties(final XmlImport xmlImport, final Map<Integer, List<DescItemPartyRef>> partyDescItems,
            final Map<Integer, Record> recordMap) {
        Assert.notNull(xmlImport);
        Assert.notNull(partyDescItems);
        Assert.notNull(recordMap);

        // id osoby na osobu
        Map<String, AbstractParty> partyMap = new HashMap<>(partyDescItems.size());
        // id osoby na seznam id autorů
        Map<AbstractParty, List<String>> partyCreatorsMap = new HashMap<>(partyDescItems.size());
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(partyDescItems.keySet());
        while (iterator.hasNext()) {
            List<Integer> partyIds = iterator.next();
            List<ParParty> parties = partyRepository.findAll(partyIds);
            for (ParParty parParty : parties) {
                AbstractParty party = createParty(parParty, recordMap, partyCreatorsMap);
                partyMap.put(party.getPartyId(), party);
                updateDescItemPartyReferences(party, partyDescItems);
            }
        }

        partyCreatorsMap.forEach((party, creatorIds) -> {
            List<AbstractParty> creators = creatorIds.stream().map(id -> partyMap.get(id)).collect(Collectors.toList());
            party.setCreators(creators);
        });
    }

    /**
     * Doplní osoby do hodnot atributů které na ně odkazují.
     *
     * @param party osoba
     * @param partyDescItems mapa id osob na hodnoty atributů
     */
    private void updateDescItemPartyReferences(final AbstractParty party, final Map<Integer, List<DescItemPartyRef>> partyDescItems) {
        Assert.notNull(party);
        Assert.notNull(partyDescItems);

        List<DescItemPartyRef> descItemPartyRefs = partyDescItems.get(party.getPartyId());
        if (CollectionUtils.isNotEmpty(descItemPartyRefs)) {
            descItemPartyRefs.forEach(diPR -> diPR.setParty(party));
        }
    }

    private AbstractParty createParty(final ParParty parParty, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parParty);

        AbstractParty party;
        if (parParty instanceof ParDynasty) {
            party = createDynasty((ParDynasty) parParty, recordMap, partyCreatorsMap);
        } else if (parParty instanceof ParEvent) {
            party = createEvent((ParEvent) parParty, recordMap, partyCreatorsMap);
        } else if (parParty instanceof ParPartyGroup) {
            party = createPartyGroup((ParPartyGroup) parParty, recordMap, partyCreatorsMap);
        } else if (parParty instanceof ParPerson) {
            party = createPerson((ParPerson) parParty, recordMap, partyCreatorsMap);
        } else {
            throw new IllegalStateException("Nepodporovaný typ osoby " + parParty.getClass());
        }

        return party;
    }

    private void fillCommonAttributes(final AbstractParty party, final ParParty parParty, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(party);
        Assert.notNull(parParty);
        Assert.notNull(recordMap);

        party.setCharacteristics(parParty.getCharacteristics());

        List<ParCreator> partyCreators = parParty.getPartyCreators();
        if (!partyCreators.isEmpty()) {
            List<String> creatorIds = partyCreators.stream().map(c -> c.getCreatorId().toString()).collect(Collectors.toList());
            partyCreatorsMap.put(party, creatorIds);
        }
        party.setEvents(createEvents(parParty.getRelations(), recordMap));

        party.setFromDate(createComplexDate(parParty.getFrom()));
        party.setHistory(parParty.getHistory());
        party.setInstitution(createInstituion(parParty.getInstitution()));
        party.setPartyId(parParty.getPartyId().toString());
        party.setPartyTypeCode(parParty.getPartyType().getCode());
        party.setPreferredName(createPartyName(parParty.getPreferredName()));

        RegRecord regRecord = parParty.getRecord();
        Record record = recordMap.get(regRecord.getRecordId());
        if (record == null) {
            throw new IllegalStateException("Nebyl nalezen vyexportovaný rejstřík s id " + regRecord.getRecordId());
        }
        party.setRecord(record);

        party.setSourceInformations(parParty.getSourceInformation());
        party.setToDate(createComplexDate(parParty.getTo()));
        party.setVariantNames(createVariantNames(parParty.getPartyNames()));
    }

    private List<Relation> createEvents(final List<ParRelation> parRelations, final Map<Integer, Record> recordMap) {
        if (CollectionUtils.isEmpty(parRelations)) {
            return null;
        }

        return parRelations.stream().map(relation -> createRelation(relation, recordMap)).collect(Collectors.toList());
    }

    private Relation createRelation(final ParRelation parRelation, final Map<Integer, Record> recordMap) {
        Assert.notNull(parRelation);

        Relation relation = new Relation();

        relation.setClassTypeCode(parRelation.getComplementType().getClassType());
        relation.setDateNote(parRelation.getDateNote());
        relation.setFromDate(createComplexDate(parRelation.getFrom()));
        relation.setNote(parRelation.getNote());
        relation.setRelationTypeCode(parRelation.getComplementType().getCode());


        List<ParRelationEntity> parRelationEntities = relationEntityRepository.findByParty(parRelation.getParty());
        relation.setRoleTypes(createRoleTypes(parRelationEntities, recordMap));
        relation.setSource(parRelation.getSource());
        relation.setToDate(createComplexDate(parRelation.getTo()));

        return relation;
    }

    private List<RoleType> createRoleTypes(final List<ParRelationEntity> parRelationEntities, final Map<Integer, Record> recordMap) {
        if (CollectionUtils.isEmpty(parRelationEntities)) {
            return null;
        }

        return parRelationEntities.stream().map(entity -> createRoleType(entity, recordMap)).collect(Collectors.toList());
    }

    private RoleType createRoleType(final ParRelationEntity parRelationEntity, final Map<Integer, Record> recordMap) {
        RoleType roleType = new RoleType();

        Record record = recordMap.get(parRelationEntity.getRecord().getRecordId());
        roleType.setRecord(record);
        roleType.setRoleTypeCode(parRelationEntity.getRoleType().getCode());
        //roleType.setSource(); //nepoužívá se

        return roleType;
    }

    private List<PartyName> createVariantNames(final List<ParPartyName> parPartyNames) {
        if (CollectionUtils.isEmpty(parPartyNames)) {
            return null;
        }

        return parPartyNames.stream().map(name -> createPartyName(name)).collect(Collectors.toList());
    }

    private PartyName createPartyName(final ParPartyName preferredName) {
        if (preferredName == null) {
            return null;
        }

        PartyName partyName = new PartyName();

        partyName.setDegreeAfter(preferredName.getDegreeAfter());
        partyName.setDegreeBefore(preferredName.getDegreeBefore());
        partyName.setMainPart(preferredName.getMainPart());
        partyName.setNote(preferredName.getNote());
        partyName.setOtherPart(preferredName.getOtherPart());

        partyName.setPartyNameComplements(createPartyNameComplements(preferredName.getPartyNameComplements()));
        partyName.setPartyNameFormTypeCode(preferredName.getNameFormType().getCode());
        partyName.setValidFrom(createComplexDate(preferredName.getValidFrom()));
        partyName.setValidTo(createComplexDate(preferredName.getValidTo()));

        return partyName;
    }

    private List<PartyNameComplement> createPartyNameComplements(final List<ParPartyNameComplement> parPartyNameComplements) {
        if (CollectionUtils.isEmpty(parPartyNameComplements)) {
            return null;
        }

        return parPartyNameComplements.stream().map(complement -> createPartyNameComplement(complement)).
                collect(Collectors.toList());
    }

    private PartyNameComplement createPartyNameComplement(final ParPartyNameComplement parPartyNameComplement) {
        Assert.notNull(parPartyNameComplement);

        PartyNameComplement partyNameComplement = new PartyNameComplement();

        partyNameComplement.setComplement(parPartyNameComplement.getComplement());
        partyNameComplement.setPartyNameComplementTypeCode(parPartyNameComplement.getComplementType().getCode());

        return partyNameComplement;
    }

    private Institution createInstituion(final ParInstitution parInstitution) {
        if (parInstitution == null) {
            return null;
        }
        Institution institution = new Institution();

        institution.setCode(parInstitution.getCode());
        institution.setTypeCode(parInstitution.getInstitutionType().getCode());

        return institution;
    }

    private ComplexDate createComplexDate(final ParUnitdate parUnitdate) {
        if (parUnitdate == null) {
            return null;
        }

        ComplexDate complexDate = new ComplexDate();

        complexDate.setSpecificDateFrom(XmlImportUtils.stringToDate(parUnitdate.getValueFrom()));
        complexDate.setSpecificDateTo(XmlImportUtils.stringToDate(parUnitdate.getValueTo()));
        complexDate.setTextDate(parUnitdate.getTextDate());

        return complexDate;
    }

    private AbstractParty createPerson(final ParPerson parPerson, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parPerson);
        Assert.notNull(recordMap);

        Person person = new Person();
        fillCommonAttributes(person, parPerson, recordMap, partyCreatorsMap);

        return person;
    }

    private AbstractParty createPartyGroup(final ParPartyGroup parPartyGroup, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parPartyGroup);
        Assert.notNull(recordMap);

        PartyGroup partyGroup = new PartyGroup();
        fillCommonAttributes(partyGroup, parPartyGroup, recordMap, partyCreatorsMap);

        partyGroup.setFoundingNorm(parPartyGroup.getFoundingNorm());
        partyGroup.setOrganization(parPartyGroup.getOrganization());
        partyGroup.setPartyGroupIds(createPartyGroupIds(parPartyGroup.getPartyGroupIdentifiers()));
        partyGroup.setScope(parPartyGroup.getScope());
        partyGroup.setScopeNorm(parPartyGroup.getScopeNorm());

        return partyGroup;
    }

    private List<PartyGroupId> createPartyGroupIds(final List<ParPartyGroupIdentifier> partyGroupIdentifiers) {
        if (CollectionUtils.isEmpty(partyGroupIdentifiers)) {
            return null;
        }

        return partyGroupIdentifiers.stream().map(groupId -> createPartyGroupId(groupId)).
                collect(Collectors.toList());
    }

    private PartyGroupId createPartyGroupId(final ParPartyGroupIdentifier parPartyGroupIdentifier) {
        Assert.notNull(parPartyGroupIdentifier);

        PartyGroupId partyGroupId = new PartyGroupId();

        partyGroupId.setId(parPartyGroupIdentifier.getIdentifier());
        partyGroupId.setNote(parPartyGroupIdentifier.getNote());
        partyGroupId.setSource(parPartyGroupIdentifier.getSource());
        partyGroupId.setValidFrom(createComplexDate(parPartyGroupIdentifier.getFrom()));
        partyGroupId.setValidTo(createComplexDate(parPartyGroupIdentifier.getTo()));

        return partyGroupId;
    }

    private AbstractParty createEvent(final ParEvent parEvent, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parEvent);
        Assert.notNull(recordMap);

        Event event = new Event();
        fillCommonAttributes(event, parEvent, recordMap, partyCreatorsMap);

        return event;
    }

    private AbstractParty createDynasty(final ParDynasty parDynasty, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parDynasty);
        Assert.notNull(recordMap);

        Dynasty dynasty = new Dynasty();
        fillCommonAttributes(dynasty, parDynasty, recordMap, partyCreatorsMap);

        dynasty.setGenealogy(parDynasty.getGenealogy());

        return dynasty;
    }

    private Map<Integer, Record> exportRecords(final XmlImport xmlImport, final Map<Integer, List<Level>> recordLevels, final Map<Integer, List<DescItemRecordRef>> recordDescItems) {
        Assert.notNull(xmlImport);
        Assert.notNull(recordLevels);
        Assert.notNull(recordDescItems);

        Set<Integer> allRecordIds = new HashSet<>(recordLevels.keySet());
        allRecordIds.addAll(recordDescItems.keySet());
        Map<Integer, Record> recordMap = new HashMap<>();
        Map<Integer, List<Record>> parentIdToChildrenRecords = new HashMap<>();
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(allRecordIds);
        while (iterator.hasNext()) {
            List<Integer> recordIds = iterator.next();
            List<RegRecord> records = recordRepository.findAll(recordIds);
            for (RegRecord regRecord : records) {
                Record record = createRecord(regRecord);
                updateLevelRecordReferences(record, recordLevels);
                updateDescItemRecordReferences(record, recordDescItems);
                recordMap.put(regRecord.getRecordId(), record);

                RegRecord parentRegRecord = regRecord.getParentRecord();
                if (parentRegRecord != null) {
                    Integer parentId = parentRegRecord.getRecordId();
                    List<Record> children = parentIdToChildrenRecords.get(parentId);
                    if (children == null) {
                        children = new LinkedList<>();
                        parentIdToChildrenRecords.put(parentId, children);
                    }
                    children.add(record);
                }
            }
        }

        setChildrenRecords(parentIdToChildrenRecords, recordMap);

        return recordMap;
    }

    /**
     * Nastaví podřízené rejstříky.
     *
     * @param parentIdToChildrenRecords mapa id parenta na seznam podřízených rejstříků
     * @param recordMap mapa id rejstříku na rejstřík
     */
    private void setChildrenRecords(final Map<Integer, List<Record>> parentIdToChildrenRecords,
            final Map<Integer, Record> recordMap) {
        parentIdToChildrenRecords.forEach((id, children) -> {
            recordMap.get(id).setRecords(children);
        });
    }

    /**
     * Doplní rejstříky do hodnot atributů které na ně odkazují.
     *
     * @param record rejstřík
     * @param recordDescItems mapa id rejstříků na hodnoty atributů
     */
    private void updateDescItemRecordReferences(final Record record, final Map<Integer, List<DescItemRecordRef>> recordDescItems) {
        Assert.notNull(record);
        Assert.notNull(recordDescItems);

        List<DescItemRecordRef> descItemRecordRefs = recordDescItems.get(record.getRecordId());
        if (CollectionUtils.isNotEmpty(descItemRecordRefs)) {
            descItemRecordRefs.forEach(diRR -> diRR.setRecord(record));
        }
    }

    /**
     * Doplní rejstříky do levelů které na ně odkazují.
     *
     * @param record rejstřík
     * @param recordLevels mapa id rejstříků na levely
     */
    private void updateLevelRecordReferences(final Record record, final Map<Integer, List<Level>> recordLevels) {
        Assert.notNull(record);
        Assert.notNull(recordLevels);

        List<Level> levels = recordLevels.get(record.getRecordId());
        if (CollectionUtils.isNotEmpty(levels)) {
            levels.forEach(l -> {
                List<Record> records = l.getRecords();
                if (records == null) {
                    records = new LinkedList<>();
                    l.setRecords(records);
                }

                records.add(record);
            });
        }
    }

    private Record createRecord(final RegRecord regRecord) {
        Record record = new Record();

        record.setCharacteristics(regRecord.getCharacteristics());

        RegExternalSource externalSource = regRecord.getExternalSource();
        if (externalSource != null) {
            record.setExternalId(regRecord.getExternalId());
            record.setExternalSourceCode(externalSource.getCode());

            record.setLocal(false);
        } else {
            record.setLocal(true);
        }

        record.setNote(regRecord.getNote());
        record.setPreferredName(regRecord.getRecord());
//        record.setRecordCoordinates(); //zatím se s nimi nepracuje
        record.setRecordId(regRecord.getRecordId().toString());
        record.setRegisterTypeCode(regRecord.getRegisterType().getCode());

        List<RegVariantRecord> variantRecordList = regRecord.getVariantRecordList();
        if (CollectionUtils.isNotEmpty(variantRecordList)) {
            List<VariantRecord> variantNames = variantRecordList.stream().map(variantName -> {
                VariantRecord variantRecord = new VariantRecord();
                variantRecord.setVariantName(variantName.getRecord());
                return variantRecord;
            }).collect(Collectors.toList());
            record.setVariantNames(variantNames);
        }

        return record;
    }

    /**
     * Export stromu archivního popisu.
     *
     * @param nodeIdsToExport id nodů které se mají exportovat
     * @param version verze archivního fondu
     * @param recordLevels mapa do které se naplní id rejstříku a seznam levelů které na něj odkazují
     * @param recordDescItems mapa do které se naplní id rejstříku a seznam hodnot atributů které na něj odkazují
     * @param partyDescItems mapa do které se naplní id osoby a seznam hodnot atributů které na ni odkazují
     * @param packetDescItems mapa do které se naplní id obalu a seznam hodnot atributů které na něj odkazují
     * @param findParents příznak zda se k nodům z parametru nodeIdsToExport mají dohledat rodiče
     */
    private Level exportNodeTree(final Set<Integer> nodeIdsToExport, final ArrFundVersion version,
            final Map<Integer, List<Level>> recordLevels, final Map<Integer, List<DescItemRecordRef>> recordDescItems,
            final Map<Integer, List<DescItemPartyRef>> partyDescItems, final Map<Integer, List<DescItemPacketRef>> packetDescItems,
            final boolean findParents) {
        Assert.notEmpty(nodeIdsToExport);
        Assert.notNull(version);
        Assert.notNull(recordLevels);
        Assert.notNull(recordDescItems);
        Assert.notNull(partyDescItems);
        Assert.notNull(packetDescItems);

        // mapa pro id nodu z parametru nodeIdsToExport na jeho Level který se vytvoří při hledání parentů
        Queue<LevelWithChildrenIds> childrenToExport = new LinkedList<>();
        Level rootLevel;
        if (findParents) {
            rootLevel = exportParents(nodeIdsToExport, version, childrenToExport, recordLevels, recordDescItems, partyDescItems, packetDescItems);
        } else {
            Assert.isTrue(nodeIdsToExport.size() == 1, "V tomto případě by v množině mělo být jen id kořenového uzlu.");

            Integer nodeId = nodeIdsToExport.iterator().next();
            ArrNode arrNode = nodeRepository.findOne(nodeId);
            LevelWithChildrenIds levelWithChildrenIds = exportLevel(arrNode, version, null, recordLevels, recordDescItems, partyDescItems, packetDescItems);
            childrenToExport.add(levelWithChildrenIds);

            rootLevel = levelWithChildrenIds.getLevel();
        }

        while (!childrenToExport.isEmpty()) {
            LevelWithChildrenIds parent = childrenToExport.poll();
            List<Integer> childrenIds = parent.getChildrenIds();
            if (!childrenIds.isEmpty()) {
                nodeRepository.findAll(childrenIds).forEach(arrNode -> {
                    childrenToExport.add(exportLevel(arrNode, version, parent.getLevel(), recordLevels, recordDescItems, partyDescItems, packetDescItems));
                });
            }
        }

        return rootLevel;
    }

    private LevelWithChildrenIds exportLevel(final ArrNode arrNode, final ArrFundVersion version, final Level parent,
            final Map<Integer, List<Level>> recordLevels, final Map<Integer, List<DescItemRecordRef>> recordDescItems,
            final Map<Integer, List<DescItemPartyRef>> partyDescItems,
            final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        ArrChange lockChange = version.getLockChange();
        ArrFund arrFund = version.getFund();
        List<ArrLevel> levels = levelRepository.findByNode(arrNode, lockChange);
        Iterator<ArrLevel> it = levels.iterator();
        while (it.hasNext()) {
            ArrLevel arrLevel = it.next();
            if (!arrLevel.getNode().getFund().equals(arrFund)) {
                it.remove();
            }
        }

        Assert.isTrue(levels.size() == 1, "Node nemůže mít pro jednu změnu více než jeden level.");

        ArrLevel arrLevel = levels.iterator().next();
        Set<Integer> nodeIds = new HashSet<>();
        nodeIds.add(arrNode.getNodeId());
        List<ArrData> dataList = dataRepository.findDescItemsByNodeIds(nodeIds, null, version);

        Level level = createLevel(arrLevel, parent, dataList, recordLevels, recordDescItems, partyDescItems, packetDescItems);

        Map<Integer, TreeNode> treeCache = levelTreeCacheService.getVersionTreeCache(version);
        List<Integer> childrenIds = treeCache.get(arrNode.getNodeId()).getChilds().stream().map(ch -> ch.getId()).collect(Collectors.toList());
        return new LevelWithChildrenIds(level, childrenIds);
    }

    private Level createLevel(final ArrLevel arrLevel, final Level parent, final List<ArrData> dataList, final Map<Integer, List<Level>> recordLevels,
            final Map<Integer, List<DescItemRecordRef>> recordDescItems, final Map<Integer, List<DescItemPartyRef>> partyDescItems,
            final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Level level = new Level();

        level.setPosition(arrLevel.getPosition());
        level.setUuid(arrLevel.getNode().getUuid());

        level.setDescItems(createDescItems(dataList, recordDescItems, partyDescItems, packetDescItems));

        if (parent != null) {
            List<Level> subLevels = parent.getSubLevels();
            if (subLevels == null) {
                subLevels = new LinkedList<>();
                parent.setSubLevels(subLevels);;
            }
            subLevels.add(level);
        }

        // zaznamenat rejstříky pro level
        List<RegRecord> records = nodeRegisterRepository.findRecordsByNode(arrLevel.getNode());
        records.forEach(r -> {
            Integer recordId = r.getRecordId();
            List<Level> levels = recordLevels.get(recordId);
            if (levels == null) {
                levels = new LinkedList<>();
                recordLevels.put(recordId, levels);
            }
            levels.add(level);
        });

        return level;
    }

    private List<AbstractDescItem> createDescItems(final List<ArrData> dataList, final Map<Integer, List<DescItemRecordRef>> recordDescItems, final Map<Integer, List<DescItemPartyRef>> partyDescItems, final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notNull(dataList);

        List<AbstractDescItem> descItems = new ArrayList<>(dataList.size());
        for (ArrData arrData : dataList) {
            ArrDescItem arrdescItem = arrData.getDescItem();
            if (arrdescItem == null) {
                continue;
            }

            String dataTypeCode = arrData.getDataType().getCode();
            if (dataTypeCode.equals("ENUM")) {
                DescItemEnum descItem = new DescItemEnum();

                fillCommonAttributes(descItem, arrdescItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("PARTY_REF")) {
                DescItemPartyRef descItem = new DescItemPartyRef();
                ArrDataPartyRef arrDataPartyRef = (ArrDataPartyRef) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                Integer partyId = arrDataPartyRef.getParty().getPartyId();
                List<DescItemPartyRef> descItemsPartyRef = partyDescItems.get(partyId);
                if (descItemsPartyRef == null) {
                    descItemsPartyRef = new LinkedList<>();
                    partyDescItems.put(partyId, descItemsPartyRef);
                }
                descItemsPartyRef.add(descItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("RECORD_REF")) {
                DescItemRecordRef descItem = new DescItemRecordRef();
                ArrDataRecordRef arrDataRecordRef = (ArrDataRecordRef) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                Integer recordId = arrDataRecordRef.getRecord().getRecordId();
                List<DescItemRecordRef> descItemsRecordRef = recordDescItems.get(recordId);
                if (descItemsRecordRef == null) {
                    descItemsRecordRef = new LinkedList<>();
                    recordDescItems.put(recordId, descItemsRecordRef);
                }
                descItemsRecordRef.add(descItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("PACKET_REF")) {
                DescItemPacketRef descItem = new DescItemPacketRef();
                ArrDataPacketRef arrDataPacketRef = (ArrDataPacketRef) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                Integer packetId = arrDataPacketRef.getPacket().getPacketId();
                List<DescItemPacketRef> descItemsPacketRef = packetDescItems.get(packetId);
                if (descItemsPacketRef == null) {
                    descItemsPacketRef = new LinkedList<>();
                    packetDescItems.put(packetId, descItemsPacketRef);
                }
                descItemsPacketRef.add(descItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("UNITDATE")) {
                DescItemUnitDate descItem = new DescItemUnitDate();
                ArrDataUnitdate arrDataUnitdate = (ArrDataUnitdate) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                descItem.setCalendarTypeCode(arrDataUnitdate.getCalendarType().getCode());
                descItem.setFormat(arrDataUnitdate.getFormat());
                descItem.setValueFrom(XmlImportUtils.stringToDate(arrDataUnitdate.getValueFrom()));
                descItem.setValueFromEstimated(arrDataUnitdate.getValueFromEstimated());
                descItem.setValueTo(XmlImportUtils.stringToDate(arrDataUnitdate.getValueTo()));
                descItem.setValueToEstimated(arrDataUnitdate.getValueToEstimated());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("STRING")) {
                DescItemString descItem = new DescItemString();
                ArrDataString arrDataString = (ArrDataString) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataString.getValue());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("TEXT")) {
                DescItemText descItem = new DescItemText();
                ArrDataText arrDataText = (ArrDataText) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataText.getValue());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("FORMATTED_TEXT")) {
                DescItemFormattedText descItem = new DescItemFormattedText();
                ArrDataText arrDataText = (ArrDataText) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataText.getValue());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("UNITID")) {
                DescItemUnitId descItem = new DescItemUnitId();
                ArrDataUnitid arrDataUnitid = (ArrDataUnitid) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataUnitid.getValue());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("INT")) {
                DescItemInteger descItem = new DescItemInteger();
                ArrDataInteger arrDataInteger = (ArrDataInteger) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataInteger.getValue());

                descItems.add(descItem);
            } else if (dataTypeCode.equals("DECIMAL")) {
                DescItemDecimal descItem = new DescItemDecimal();
                ArrDataDecimal arrDataDecimal = (ArrDataDecimal) arrData;

                fillCommonAttributes(descItem, arrdescItem);
                descItem.setValue(arrDataDecimal.getValue());

                descItems.add(descItem);
            } else if(dataTypeCode.equals("COORDINATES")){
                DescItemCoordinates descItem = new DescItemCoordinates();
                fillCommonAttributes(descItem, arrdescItem);

                ArrDataCoordinates arrDataCoordinates = (ArrDataCoordinates) arrData;

                Geometry geometry = arrDataCoordinates.getValue();
                descItem.setValue(new WKTWriter().write(geometry));

                descItems.add(descItem);
            }

            if (arrdescItem instanceof ArrDescItemCoordinates) {

            } else if (arrdescItem instanceof ArrDescItemDecimal) {

            } else if (arrdescItem instanceof ArrDescItemFormattedText) {

            } else if (arrdescItem instanceof ArrDescItemInt) {

            } else if (arrdescItem instanceof ArrDescItemPacketRef) {

            } else if (arrdescItem instanceof ArrDescItemPartyRef) {

            } else if (arrdescItem instanceof ArrDescItemRecordRef) {

            } else if (arrdescItem instanceof ArrDescItemString) {

            } else if (arrdescItem instanceof ArrDescItemText) {

            } else if (arrdescItem instanceof ArrDescItemUnitdate) {

            } else if (arrdescItem instanceof ArrDescItemUnitid) {

            } else if (arrdescItem instanceof ArrDescItemEnum) {

            }
        }

        return descItems;
    }

    /**
     * Vyplní společné atributy hodnoty uzlu.
     *
     * @param descItem xml reprezentace hodnoty
     * @param arrDescItem db reprezentace hodnoty
     */
    private void fillCommonAttributes(final AbstractDescItem descItem, final ArrDescItem arrDescItem) {
        descItem.setDescItemTypeCode(arrDescItem.getDescItemType().getCode());
        descItem.setPosition(arrDescItem.getPosition());

        RulDescItemSpec rulDescItemSpec = arrDescItem.getDescItemSpec();
        if (rulDescItemSpec != null) {
            descItem.setDescItemSpecCode(rulDescItemSpec.getCode());
        }
    }

    private Level exportParents(final Set<Integer> nodeIdsToExport, final ArrFundVersion version,
        final Queue<LevelWithChildrenIds> childrenToExport, final Map<Integer, List<Level>> recordLevels,
        final Map<Integer, List<DescItemRecordRef>> recordDescItems, final Map<Integer, List<DescItemPartyRef>> partyDescItems,
        final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notEmpty(nodeIdsToExport);

        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(version);
        Map<Integer, List<Integer>> parentNodeIdToChildrenIdMap = new HashMap<>();
        for (Integer nodeId : nodeIdsToExport) {
            List<Integer> parentIds = getParentIds(nodeId, versionTreeCache);
            putParentIdsToMap(nodeId, parentIds, parentNodeIdToChildrenIdMap);
        }

        Integer rootNodeId = null;
        if (parentNodeIdToChildrenIdMap.isEmpty()) {
            // do mapy se nedali  potomci takže na vstupu musel být root
            rootNodeId = nodeIdsToExport.iterator().next();
        } else {
            // vezmeme jeden záznam a z něj posledního rodiče(kořen)
            List<Integer> parentIds = parentNodeIdToChildrenIdMap.entrySet().iterator().next().getValue();
            rootNodeId = parentIds.get(parentIds.size());
        }

        ArrNode rootNode = nodeRepository.findOne(rootNodeId);
        LevelWithChildrenIds rootLevel = exportLevel(rootNode, version, null, recordLevels, recordDescItems, partyDescItems, packetDescItems);
        List<Integer> childrenNodeIds = parentNodeIdToChildrenIdMap.get(rootNodeId);
        if (CollectionUtils.isEmpty(childrenNodeIds)) {
            return rootLevel.getLevel();
        }

        Queue<Integer> parentsToExport = new LinkedList<>();
        parentsToExport.add(rootNodeId);
        Level parent = rootLevel.getLevel();
        while (!parentsToExport.isEmpty()) {
            Integer nodeId = parentsToExport.poll();
            List<Integer> childrenIds = parentNodeIdToChildrenIdMap.get(nodeId);
            parentsToExport.addAll(childrenIds);

            if (!childrenIds.isEmpty()) {
                nodeRepository.findAll(childrenIds).forEach(arrNode -> {
                    LevelWithChildrenIds levelWithChildrenIds = exportLevel(arrNode, version, parent, recordLevels, recordDescItems, partyDescItems, packetDescItems);
                    if (nodeIdsToExport.contains(arrNode.getNodeId())) {
                        childrenToExport.add(levelWithChildrenIds);
                    }
                });
            }
        }

        return rootLevel.getLevel();
    }

    private void putParentIdsToMap(final Integer nodeId, final List<Integer> parentIds,
            final Map<Integer, List<Integer>> parentNodeIdToChildrenIdMap) {
        Assert.notNull(nodeId);

        if (parentIds == null) {
            return;
        }

        Integer children = nodeId;
        for (Integer parentId : parentIds) {
            List<Integer> childrenIds = parentNodeIdToChildrenIdMap.get(parentId);
            if (childrenIds == null) {
                childrenIds = new LinkedList<>();
                parentNodeIdToChildrenIdMap.put(parentId, childrenIds);
            }
            if (!childrenIds.contains(children)) {
                childrenIds.add(children);
            }
            children = parentId;
        }
    }

    private List<Integer> getParentIds(final Integer nodeId, final Map<Integer, TreeNode> versionTreeCache) {
        TreeNode treeNode = versionTreeCache.get(nodeId);
        if (treeNode == null) {
            return null;
        }

        List<Integer> parentIds = new LinkedList<>();
        while (treeNode != null) {
            Integer parentId = treeNode.getParent().getId();
            parentIds.add(parentId);

            treeNode = versionTreeCache.get(parentId);
        }

        return parentIds;
    }

    private Fund createFund(final ArrFund arrFund, final ArrFundVersion version) {
        Fund fund = new Fund();

        RulRuleSet ruleSet = version.getRuleSet();
        fund.setInstitutionCode(arrFund.getInstitution().getCode());

        fund.setName(arrFund.getName());
        fund.setRuleSetCode(ruleSet.getCode());

        return fund;
    }

    /**
     * Vrátí názvy šablon.
     *
     * @return názvy šablon
     */
    public List<String> getTransformationNames() {
        return XmlUtils.getTransformationNames(transformationsDirectory);
    }

    private static class LevelWithChildrenIds {

        private Level level;

        private List<Integer> childrenIds;

        LevelWithChildrenIds(final Level level, final List<Integer> childrenIds) {
            Assert.notNull(level);
            Assert.notNull(childrenIds);

            this.level = level;
            this.childrenIds = childrenIds;
        }

        public Level getLevel() {
            return level;
        }

        public List<Integer> getChildrenIds() {
            return childrenIds;
        }
    }
}
