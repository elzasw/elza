package cz.tacr.elza.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.ArrPacket.State;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
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
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.service.vo.XmlExportResult;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.utils.ProxyUtils;
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
import cz.tacr.elza.xmlimport.v1.vo.arrangement.DescItemJsonTable;
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
import cz.tacr.elza.xmlimport.v1.vo.arrangement.PacketState;
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
 * @since 22. 4. 2016
 */
@Service
public class XmlExportService {

    @Value("${elza.xmlExport.transformationDir}")
    private String transformationsDirectory;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private LevelTreeCacheWalker levelTreeCacheWalker;

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

    @Autowired
    private InstitutionRepository institutionRepository;

    /**
     * Export archivního souboru.
     *
     * @param config nastavení exportu
     *
     * @return výsledek exportu
     */
    public XmlExportResult exportData(final XmlExportConfig config) {
        Assert.notNull(config);
        Assert.notNull(config.getVersionId());

        XmlImport xmlImport = exportFund(config.getVersionId(), config.getNodeIds());

        return createExportResult(config, xmlImport);
    }

    /**
     * Převede exportovaná data na výsledný objekt exportu.
     *
     * @param config nastavení exportu
     * @param xmlImport exportovaná data
     *
     * @return výsledek exportu
     */
    private XmlExportResult createExportResult(final XmlExportConfig config, final XmlImport xmlImport) {
        String fundName = xmlImport.getFund().getName();
        File exportFile;
        String downloadfileName;

        String transformationName = config.getTransformationName();
        boolean isCompressed;
        try {
            if (StringUtils.isNotBlank(transformationName)) {
                downloadfileName = fundName + ".zip";
                isCompressed = true;
                byte[] xmlData = XmlUtils.marshallData(xmlImport, XmlImport.class);
                byte[] transformedData = XmlUtils.transformData(xmlData, transformationName, transformationsDirectory);

                exportFile = File.createTempFile("elza-export", ".zip");
                FileOutputStream fos = new FileOutputStream(exportFile);
                ZipOutputStream zos = new ZipOutputStream(fos);
                ZipEntry zipEntry = new ZipEntry(fundName + ".xml");
                zos.putNextEntry(zipEntry);

                ByteArrayInputStream in = new ByteArrayInputStream(xmlData);
                IOUtils.copy(in, zos);
                in.close();

                zipEntry = new ZipEntry(fundName + ".transformed");
                zos.putNextEntry(zipEntry);

                in = new ByteArrayInputStream(transformedData);
                IOUtils.copy(in, zos);
                in.close();
                zos.close();
            } else {
                downloadfileName = fundName + ".xml";
                isCompressed = false;
                byte[] xmlData = XmlUtils.marshallData(xmlImport, XmlImport.class);

                exportFile = XmlUtils.createTempFile(xmlData, "elza-export-", ".xml");
                ByteArrayInputStream in = new ByteArrayInputStream(xmlData);
                FileOutputStream fos = new FileOutputStream(exportFile);
                IOUtils.copy(in, fos);
                in.close();
                fos.close();
            }

            return new XmlExportResult(exportFile, downloadfileName, isCompressed);
        } catch (IOException e) {
            throw new IllegalStateException("Chyba při zápisu exportovaných dat.", e);
        }
    }

    /**
     * Export archivního souboru.
     *
     * @param versionId id verze jaká se má exportovat
     * @param nodeIds množina nodůkteré se mají exportovat, pokud je prázdná exportuje se celý strom
     *
     * @return exprotovaná data
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_EXPORT_ALL, UsrPermission.Permission.FUND_EXPORT})
    private XmlImport exportFund(@AuthParam(type = AuthParam.Type.FUND_VERSION) final Integer versionId,
                                 final Set<Integer> nodeIds) {
        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        ArrFund arrFund = version.getFund();
        // zatím se má exportovat jen otevřená verze
        version = arrangementService.getOpenVersionByFundId(arrFund.getFundId());

        XmlImport xmlImport = new XmlImport();
        xmlImport.setFund(createFund(arrFund, version));

        RelatedEntities relatedEntities = new RelatedEntities();

        Level rootLevel = exportNodeTree(nodeIds, version, relatedEntities);
        xmlImport.getFund().setRootLevel(rootLevel);

        Map<Integer, Record> recordMap = exportRecords(xmlImport, relatedEntities);
        exportParties(xmlImport, relatedEntities.getPartyDescItems(), recordMap);
        exportPackets(xmlImport, relatedEntities.getPacketDescItems());

        return xmlImport;
    }

    /**
     * Export obalů. Přidá do exportovaných dat obaly a nastaví je do hodnot které na ně odkazují.
     *
     * @param xmlImport exportovaná data
     * @param packetDescItems použité obaly a hodnoty atributů kde byly použity
     */
    private void exportPackets(final XmlImport xmlImport, final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notNull(xmlImport);
        Assert.notNull(packetDescItems);

        List<Packet> packets = new ArrayList<>(packetDescItems.size());
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(packetDescItems.keySet());
        while (iterator.hasNext()) {
            List<Integer> packetIds = iterator.next();
            List<ArrPacket> arrPackets = packetRepository.findAll(packetIds);
            for (ArrPacket arrPacket : arrPackets) {
                Packet packet = createPacket(arrPacket);
                packets.add(packet);
                updateDescItemPacketReferences(arrPacket.getPacketId(), packet, packetDescItems);
            }
        }

        xmlImport.setPackets(packets);
    }

    /**
     * Doplní obal do hodnot atributů které na ně odkazují.
     *
     * @param packetId id obalu
     * @param packet obal
     * @param packetDescItems mapa id obalů na hodnoty atributů
     */
    private void updateDescItemPacketReferences(final Integer packetId, final Packet packet,
            final Map<Integer, List<DescItemPacketRef>> packetDescItems) {
        Assert.notNull(packetId);
        Assert.notNull(packet);
        Assert.notNull(packetDescItems);

        List<DescItemPacketRef> descItemPacketRefs = packetDescItems.get(packetId);
        if (CollectionUtils.isNotEmpty(descItemPacketRefs)) {
            descItemPacketRefs.forEach(diPR -> diPR.setPacket(packet));
        }
    }

    /**
     * Vytvoří obal.
     *
     * @param arrPacket obalz db
     *
     * @return obal pro xml
     */
    private Packet createPacket(final ArrPacket arrPacket) {
        Packet packet = new Packet();

        RulPacketType packetType = arrPacket.getPacketType();
        if (packetType != null) {
            packet.setPacketTypeCode(packetType.getCode());
        }
        State arrPacketState = arrPacket.getState();
        packet.setState(PacketState.valueOf(arrPacketState.name()));

        packet.setStorageNumber(arrPacket.getStorageNumber());

        return packet;
    }

    /**
     * Export osob. Přidá do exportovaných dat osoby a nastaví je do hodnot které na ně odkazují.
     *
     * @param xmlImport exportovaná data
     * @param partyDescItems použité osoby a hodnoty atributů kde byly použity
     * @param recordMap exportované rejstříky, pro vazbu na osoby
     */
    private void exportParties(final XmlImport xmlImport, final Map<Integer, List<DescItemPartyRef>> partyDescItems,
            final Map<Integer, Record> recordMap) {
        Assert.notNull(xmlImport);
        Assert.notNull(partyDescItems);
        Assert.notNull(recordMap);

        // id osoby na osobu
        Map<String, AbstractParty> partyMap = new HashMap<>(partyDescItems.size());
        // id osoby na seznam id autorů
        Map<AbstractParty, List<String>> partyCreatorsMap = new HashMap<>(partyDescItems.size());

        List<AbstractParty> parties = new ArrayList<>(partyDescItems.size());
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(partyDescItems.keySet());
        while (iterator.hasNext()) {
            List<Integer> partyIds = iterator.next();
            List<ParParty> parParties = partyRepository.findAll(partyIds);
            for (ParParty parParty : parParties) {
                AbstractParty party = createParty(parParty, recordMap, partyCreatorsMap);
                parties.add(party);
                partyMap.put(party.getPartyId(), party);
                updateDescItemPartyReferences(parParty.getPartyId(), party, partyDescItems);
            }
        }

        partyCreatorsMap.forEach((party, creatorIds) -> {
            List<AbstractParty> creators = creatorIds.stream().map(id -> partyMap.get(id)).collect(Collectors.toList());
            party.setCreators(creators);
        });

        xmlImport.setParties(parties);
    }

    /**
     * Doplní osoby do hodnot atributů které na ně odkazují.
     *
     * @param partyId id osoby
     * @param party osoba
     * @param partyDescItems mapa id osob na hodnoty atributů
     */
    private void updateDescItemPartyReferences(final Integer partyId, final AbstractParty party,
            final Map<Integer, List<DescItemPartyRef>> partyDescItems) {
        Assert.notNull(partyId);
        Assert.notNull(party);
        Assert.notNull(partyDescItems);

        List<DescItemPartyRef> descItemPartyRefs = partyDescItems.get(partyId);
        if (CollectionUtils.isNotEmpty(descItemPartyRefs)) {
            descItemPartyRefs.forEach(diPR -> diPR.setParty(party));
        }
    }

    /**
     * Vytvoření osoby.
     *
     * @param parParty osoba do
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa do které se naplní vazby na autory vytvářené osoby
     *
     * @return exportovaná osoba
     */
    private AbstractParty createParty(final ParParty parParty, final Map<Integer, Record> recordMap,
            final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parParty);

        ParParty deproxiedParty = ProxyUtils.deproxy(parParty);

        AbstractParty party;
        if (deproxiedParty instanceof ParDynasty) {
            party = createDynasty((ParDynasty) deproxiedParty, recordMap, partyCreatorsMap);
        } else if (deproxiedParty instanceof ParEvent) {
            party = createEvent((ParEvent) deproxiedParty, recordMap, partyCreatorsMap);
        } else if (deproxiedParty instanceof ParPartyGroup) {
            party = createPartyGroup((ParPartyGroup) deproxiedParty, recordMap, partyCreatorsMap);
        } else if (deproxiedParty instanceof ParPerson) {
            party = createPerson((ParPerson) deproxiedParty, recordMap, partyCreatorsMap);
        } else {
            throw new IllegalStateException("Nepodporovaný typ osoby " + parParty.getClass());
        }

        return party;
    }

    /**
     * Zkopírování společných vlastností osob.
     *
     * @param party exportovaná osoba
     * @param parParty osoba do
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa do které se naplní vazby na autory vytvářené osoby
     */
    private void fillCommonAttributes(final AbstractParty party, final ParParty parParty,
            final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
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

        party.setHistory(parParty.getHistory());

        ParInstitution parInstitution = institutionRepository.findByParty(parParty);
        party.setInstitution(createInstituion(parInstitution));
        party.setPartyId(parParty.getPartyId().toString());
        party.setPreferredName(createPartyName(parParty.getPreferredName()));

        RegRecord regRecord = parParty.getRecord();
        Record record = recordMap.get(regRecord.getRecordId());
        if (record == null) {
            throw new IllegalStateException("Nebyl nalezen vyexportovaný rejstřík s id " + regRecord.getRecordId());
        }
        party.setRecord(record);

        party.setSourceInformations(parParty.getSourceInformation());
        List<ParPartyName> partyNames = parParty.getPartyNames();
        if (CollectionUtils.isNotEmpty(partyNames)) {
            List<ParPartyName> namesToExport = new ArrayList<>(partyNames);
            namesToExport.remove(parParty.getPreferredName());
            party.setVariantNames(createVariantNames(namesToExport));
        }
    }

    /**
     * Vytvoření vztahů a událostí.
     *
     * @param parRelations do vztahy a události
     * @param recordMap exportované rejstříky
     *
     * @return exportované vztahy a události
     */
    private List<Relation> createEvents(final List<ParRelation> parRelations, final Map<Integer, Record> recordMap) {
        if (CollectionUtils.isEmpty(parRelations)) {
            return null;
        }

        return parRelations.stream().map(relation -> createRelation(relation, recordMap)).collect(Collectors.toList());
    }

    /**
     * Vytvoření vztahu nebo události.
     *
     * @param parRelation do vztah neob událost
     * @param recordMap exportované rejstříky
     *
     * @return exportovaný vztahynebo událost
     */
    private Relation createRelation(final ParRelation parRelation, final Map<Integer, Record> recordMap) {
        Assert.notNull(parRelation);

        Relation relation = new Relation();

        relation.setClassTypeCode(parRelation.getRelationType().getRelationClassType().getCode());
        relation.setFromDate(XmlImportUtils.createComplexDate(parRelation.getFrom()));
        relation.setNote(parRelation.getNote());
        relation.setRelationTypeCode(parRelation.getRelationType().getCode());


        List<ParRelationEntity> parRelationEntities = relationEntityRepository.findByParty(parRelation.getParty());
        relation.setRoleTypes(createRoleTypes(parRelationEntities, recordMap));
        relation.setSource(parRelation.getSource());
        relation.setToDate(XmlImportUtils.createComplexDate(parRelation.getTo()));

        return relation;
    }

    /**
     * Vytvoření entit souvisejících se vztahem/událostí.
     *
     * @param parRelationEntities entity související se vztahem/událostí
     * @param recordMap exportované rejstříky
     *
     * @return exportované entity související se vztahem/událostí
     */
    private List<RoleType> createRoleTypes(final List<ParRelationEntity> parRelationEntities, final Map<Integer, Record> recordMap) {
        if (CollectionUtils.isEmpty(parRelationEntities)) {
            return null;
        }

        return parRelationEntities.stream().map(entity -> createRoleType(entity, recordMap)).collect(Collectors.toList());
    }

    /**
     * Vytvoření entity související se vztahem/událostí.
     *
     * @param parRelationEntity entita související se vztahem/událostí
     * @param recordMap exportované rejstříky
     *
     * @return exportovaná entita související se vztahem/událostí
     */
    private RoleType createRoleType(final ParRelationEntity parRelationEntity, final Map<Integer, Record> recordMap) {
        RoleType roleType = new RoleType();

        Record record = recordMap.get(parRelationEntity.getRecord().getRecordId());
        roleType.setRecord(record);
        roleType.setRoleTypeCode(parRelationEntity.getRoleType().getCode());
        roleType.setNote(parRelationEntity.getNote());
        //roleType.setSource(); //nepoužívá se

        return roleType;
    }

    /**
     * Vytvoření jmen osoby.
     *
     * @param parPartyNames do jméno osoby
     *
     * @return exportovaná jména osoby
     */
    private List<PartyName> createVariantNames(final List<ParPartyName> parPartyNames) {
        if (CollectionUtils.isEmpty(parPartyNames)) {
            return null;
        }

        return parPartyNames.stream().map(name -> createPartyName(name)).collect(Collectors.toList());
    }

    /**
     * Vytvoření jména osoby.
     *
     * @param parPartyName do jméno osoby
     *
     * @return exportované jméno osoby
     */
    private PartyName createPartyName(final ParPartyName parPartyName) {
        if (parPartyName == null) {
            return null;
        }

        PartyName partyName = new PartyName();

        partyName.setDegreeAfter(parPartyName.getDegreeAfter());
        partyName.setDegreeBefore(parPartyName.getDegreeBefore());
        partyName.setMainPart(parPartyName.getMainPart());
        partyName.setNote(parPartyName.getNote());
        partyName.setOtherPart(parPartyName.getOtherPart());

        partyName.setPartyNameComplements(createPartyNameComplements(parPartyName.getPartyNameComplements()));

        ParPartyNameFormType nameFormType = parPartyName.getNameFormType();
        if (nameFormType != null) {
            partyName.setPartyNameFormTypeCode(nameFormType.getCode());
        }

        partyName.setValidFrom(XmlImportUtils.createComplexDate(parPartyName.getValidFrom()));
        partyName.setValidTo(XmlImportUtils.createComplexDate(parPartyName.getValidTo()));

        return partyName;
    }

    /**
     * Vytvoření doplňků jména osoby.
     *
     * @param parPartyNameComplements do doplňky jména osoby
     *
     * @return exportované doplňky jména osoby
     */
    private List<PartyNameComplement> createPartyNameComplements(final List<ParPartyNameComplement> parPartyNameComplements) {
        if (CollectionUtils.isEmpty(parPartyNameComplements)) {
            return null;
        }

        return parPartyNameComplements.stream().map(complement -> createPartyNameComplement(complement)).
                collect(Collectors.toList());
    }

    /**
     * Vytvoření doplňku jména osoby.
     *
     * @param parPartyNameComplement do doplněk jména osoby
     *
     * @return exportovaný doplněk jména osoby
     */
    private PartyNameComplement createPartyNameComplement(final ParPartyNameComplement parPartyNameComplement) {
        Assert.notNull(parPartyNameComplement);

        PartyNameComplement partyNameComplement = new PartyNameComplement();

        partyNameComplement.setComplement(parPartyNameComplement.getComplement());
        partyNameComplement.setPartyNameComplementTypeCode(parPartyNameComplement.getComplementType().getCode());

        return partyNameComplement;
    }

    /**
     * Vytvoření instituce.
     *
     * @param parInstitution instituce
     *
     * @return exportovaná instituce
     */
    private Institution createInstituion(final ParInstitution parInstitution) {
        if (parInstitution == null) {
            return null;
        }
        Institution institution = new Institution();

        institution.setInternalCode(parInstitution.getInternalCode());
        institution.setTypeCode(parInstitution.getInstitutionType().getCode());

        return institution;
    }

    /**
     * Vytvoření osoby.
     *
     * @param parPerson do osoby
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa pro naplnění id autorů exportované osoby
     *
     * @return exportovaná osoba
     */
    private AbstractParty createPerson(final ParPerson parPerson, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parPerson);
        Assert.notNull(recordMap);

        Person person = new Person();
        fillCommonAttributes(person, parPerson, recordMap, partyCreatorsMap);

        return person;
    }

    /**
     * Vytvoření organizace nebo skupiny osob.
     *
     * @param parPartyGroup do organizace nebo skupiny osob
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa pro naplnění id autorů exportované osoby
     *
     * @return exportovaná organizace nebo skupina osob
     */
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

    /**
     * Vytvoření kódů/identifikací osob.
     *
     * @param partyGroupIdentifiers do kódy/identifikace osob
     *
     * @return exportované kódy/identifikace osob
     */
    private List<PartyGroupId> createPartyGroupIds(final List<ParPartyGroupIdentifier> partyGroupIdentifiers) {
        if (CollectionUtils.isEmpty(partyGroupIdentifiers)) {
            return null;
        }

        return partyGroupIdentifiers.stream().map(groupId -> createPartyGroupId(groupId)).
                collect(Collectors.toList());
    }

    /**
     * Vytvoření kódu/identifikace osoby.
     *
     * @param parPartyGroupIdentifier do kód/identifikace osoby
     *
     * @return exportovaný kód/identifikace osoby
     */
    private PartyGroupId createPartyGroupId(final ParPartyGroupIdentifier parPartyGroupIdentifier) {
        Assert.notNull(parPartyGroupIdentifier);

        PartyGroupId partyGroupId = new PartyGroupId();

        partyGroupId.setId(parPartyGroupIdentifier.getIdentifier());
        partyGroupId.setNote(parPartyGroupIdentifier.getNote());
        partyGroupId.setSource(parPartyGroupIdentifier.getSource());
        partyGroupId.setValidFrom(XmlImportUtils.createComplexDate(parPartyGroupIdentifier.getFrom()));
        partyGroupId.setValidTo(XmlImportUtils.createComplexDate(parPartyGroupIdentifier.getTo()));

        return partyGroupId;
    }

    /**
     * Vytvoření akce.
     *
     * @param parEvent do akce
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa pro naplnění id autorů exportované osoby
     *
     * @return exportovaná akce
     */
    private AbstractParty createEvent(final ParEvent parEvent, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parEvent);
        Assert.notNull(recordMap);

        Event event = new Event();
        fillCommonAttributes(event, parEvent, recordMap, partyCreatorsMap);

        return event;
    }

    /**
     * Vytvoření rodu.
     *
     * @param parDynasty do rod
     * @param recordMap exportované rejstříky
     * @param partyCreatorsMap mapa pro naplnění id autorů exportované osoby
     *
     * @return exportovaný rod
     */
    private AbstractParty createDynasty(final ParDynasty parDynasty, final Map<Integer, Record> recordMap, final Map<AbstractParty, List<String>> partyCreatorsMap) {
        Assert.notNull(parDynasty);
        Assert.notNull(recordMap);

        Dynasty dynasty = new Dynasty();
        fillCommonAttributes(dynasty, parDynasty, recordMap, partyCreatorsMap);

        dynasty.setGenealogy(parDynasty.getGenealogy());

        return dynasty;
    }

    /**
     * Export rejstříků. Přidá do exportovaných dat rejstříky a nastaví je do hodnot a uzlů které na ně odkazují.
     *
     * @param xmlImport exportovaná data
     * @param relatedEntities vazby na entity které se budou exportova později
     *
     * @return recordMap exportované rejstříky
     */
    private Map<Integer, Record> exportRecords(final XmlImport xmlImport, final RelatedEntities relatedEntities) {
        Assert.notNull(xmlImport);
        Assert.notNull(relatedEntities);

        Map<Integer, List<DescItemRecordRef>> recordDescItems = relatedEntities.getRecordDescItems();
        Map<Integer, List<Level>> recordLevels = relatedEntities.getRecordLevels();

        List<Integer> recordIdsToExport = getRecordIdsToExport(relatedEntities);

        List<Record> rootRecords = new LinkedList<>();
        Map<Integer, Record> recordMap = new HashMap<>();
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(recordIdsToExport);
        while (iterator.hasNext()) {
            List<Integer> recordIds = iterator.next();
            List<RegRecord> records = findRecordsOrdered(recordIds);
            for (RegRecord regRecord : records) {
                Record record = createRecord(regRecord);

                RegRecord parentRecord = regRecord.getParentRecord();
                if (parentRecord != null) {
                    Record parent = recordMap.get(parentRecord.getRecordId());
                    List<Record> children = parent.getRecords();
                    if (children == null) {
                        children = new LinkedList<>();
                        parent.setRecords(children);
                    }
                    children.add(record);
                } else {
                    rootRecords.add(record);
                }
                updateLevelRecordReferences(regRecord.getRecordId(), record, recordLevels);
                updateDescItemRecordReferences(regRecord.getRecordId(), record, recordDescItems);
                recordMap.put(regRecord.getRecordId(), record);
            }
        }

        xmlImport.setRecords(rootRecords);
        return recordMap;
    }

    /**
     * Načte rejstříky z db ve stejném pořadí jako jsou jejich id na vstupu.
     *
     * @param recordIds id rejstříků
     *
     * @return rejstříky ve stejném pořadí jako id na vstupu
     */
    private List<RegRecord> findRecordsOrdered(final List<Integer> recordIds) {
        List<RegRecord> records = recordRepository.findAll(recordIds);

        Map<Integer, RegRecord> recordMap = records.stream().collect(
                Collectors.toMap(RegRecord::getRecordId, Function.identity()));

        return recordIds.stream().map(id -> recordMap.get(id)).collect(Collectors.toList());
    }

    /**
     * Zjistí jaké rejstříky se budou exportovat a vrátí jejich id seřazená od kořenových uzlů po ty nejspodnější.
     *
     * @param relatedEntities vazby na entity které se budou exportova později
     *
     * @return seznam id rejstříků k exportu v pořadí v jakém se mají exportovat
     */
    private List<Integer> getRecordIdsToExport(final RelatedEntities relatedEntities) {
        Set<Integer> allRecordIds = new HashSet<>(relatedEntities.getRecordLevels().keySet());
        allRecordIds.addAll(relatedEntities.getRecordDescItems().keySet());
        allRecordIds.addAll(relatedEntities.getOtherUsedRecords());

        List<RecordEntry> recordsToExport = new LinkedList<>();

        for (Integer recordId : allRecordIds) {
            List<Integer> parents = recordRepository.findRecordParents(recordId);
            int depth = parents.size();

            RecordEntry entry = new RecordEntry(recordId, depth + 1);
            if (!recordsToExport.contains(entry)) {
                recordsToExport.add(entry);
            }

            for (Integer parentId : parents) {
                entry = new RecordEntry(parentId, depth--);
                if (!recordsToExport.contains(entry)) {
                    recordsToExport.add(entry);
                }
            }
        }
        recordsToExport.sort((a, b)-> a.getDepth().compareTo(b.getDepth()));
        return recordsToExport.stream().mapToInt(e -> e.getRecordId()).boxed().collect(Collectors.toList());
    }

    /**
     * Doplní rejstříky do hodnot atributů které na ně odkazují.
     *
     * @param recordId id rejstříku
     * @param record rejstřík
     * @param recordDescItems mapa id rejstříků na hodnoty atributů
     */
    private void updateDescItemRecordReferences(final Integer recordId, final Record record, final Map<Integer, List<DescItemRecordRef>> recordDescItems) {
        Assert.notNull(recordId);
        Assert.notNull(record);
        Assert.notNull(recordDescItems);

        List<DescItemRecordRef> descItemRecordRefs = recordDescItems.get(recordId);
        if (CollectionUtils.isNotEmpty(descItemRecordRefs)) {
            descItemRecordRefs.forEach(diRR -> diRR.setRecord(record));
        }
    }

    /**
     * Doplní rejstříky do levelů které na ně odkazují.
     *
     * @param recordId id rejstříku
     * @param record rejstřík
     * @param recordLevels mapa id rejstříků na levely
     */
    private void updateLevelRecordReferences(final Integer recordId, final Record record, final Map<Integer, List<Level>> recordLevels) {
        Assert.notNull(recordId);
        Assert.notNull(record);
        Assert.notNull(recordLevels);

        List<Level> levels = recordLevels.get(recordId);
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

    /**
     * Vytvoření rejstříku.
     *
     * @param regRecord do rejstříku
     *
     * @return exportovaný rejstřík
     */
    private Record createRecord(final RegRecord regRecord) {
        Record record = new Record();

        record.setCharacteristics(regRecord.getCharacteristics());

        RegExternalSystem externalSystem = regRecord.getExternalSystem();
        if (externalSystem != null) {
            record.setExternalId(regRecord.getExternalId());
            record.setExternalSystemCode(externalSystem.getCode());
        }

        record.setLocal(false);
        record.setNote(regRecord.getNote());
        record.setPreferredName(regRecord.getRecord());
//        record.setRecordCoordinates(); //zatím se s nimi nepracuje
        record.setRecordId(regRecord.getRecordId().toString());
        record.setRegisterTypeCode(regRecord.getRegisterType().getCode());
        record.setUuid(regRecord.getUuid());
        record.setLastUpdate(Date.from(regRecord.getLastUpdate().atZone(ZoneId.systemDefault()).toInstant()));

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
     * @param nodeIds id nodů které se mají exportovat,pokud je prázdné exportuje se celý strom
     * @param version verze archivního fondu
     * @param relatedEntities vazby na entity které se budou exportova později
     */
    private Level exportNodeTree(final Set<Integer> nodeIds, final ArrFundVersion version,
            final RelatedEntities relatedEntities) {
        Assert.notNull(version);
        Assert.notNull(relatedEntities);

        Map<Integer, Level> nodeIdToLevel = new HashMap<>();
        List<Integer> nodeIdsToExport = getNodeIdsToExport(nodeIds, version);

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIdsToExport);
        while (iterator.hasNext()) {
            List<Integer> nodeIdsSubList = iterator.next();
            List<ArrNode> nodes = getNodesByIdsOrdered(nodeIdsSubList);
            nodes.forEach(arrNode -> {
                Level level = exportLevel(arrNode, version, nodeIdToLevel, relatedEntities);
                nodeIdToLevel.put(arrNode.getNodeId(), level);
            });
        }

        return nodeIdToLevel.get(version.getRootNode().getNodeId());
    }

    /**
     * Načte uzly podle identifikátorů a vrátí je ve stejném pořadí v jakém jsou předány identifikátory.
     *
     * @param nodeIds id uzlů
     *
     * @return uzly ve stejném pořadí jako předané identifikátory
     */
    private List<ArrNode> getNodesByIdsOrdered(final List<Integer> nodeIds) {
        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);

        Map<Integer, ArrNode> nodeMap = nodes.stream().collect(
                Collectors.toMap(ArrNode::getNodeId, Function.identity()));

        return nodeIds.stream().map(id -> nodeMap.get(id)).collect(Collectors.toList());
    }

    /**
     * Načze id všech uzlů které se budou exportovat.
     *
     * @param nodeIds předané identifikátory, mohou být prázdné
     * @param version verze
     *
     * @return id všech uzlů které se budou exportovat
     */
    private List<Integer> getNodeIdsToExport(final Set<Integer> nodeIds, final ArrFundVersion version) {
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(version);
        TreeNode rootNode = versionTreeCache.get(version.getRootNode().getNodeId());
        LinkedHashSet<Integer> allNodes = levelTreeCacheWalker.walkThroughDFS(rootNode);

        List<Integer> allNodeIdsToExport;
        if (CollectionUtils.isEmpty(nodeIds)) {
            allNodeIdsToExport = new ArrayList<>(allNodes);
        } else {
            Set<Integer> usedNodeIds = new HashSet<>();

            // přidání rodičů
            for (Integer nodeId : nodeIds) {
                List<Integer> parentIds = getParentIds(nodeId, versionTreeCache);
                if (parentIds != null) {
                    usedNodeIds.addAll(parentIds);
                }
            }

            //přidání potomků vstupních uzlů
            for (Integer nodeId : nodeIds) {
                Set<Integer> children = levelTreeCacheWalker.walkThroughDFS(versionTreeCache.get(nodeId));
                usedNodeIds.addAll(children);
            }

            allNodeIdsToExport = allNodes.stream().filter(id -> usedNodeIds.contains(id)).collect(Collectors.toList());
        }

        return allNodeIdsToExport;
    }

    /**
     * Export uzlu.
     *
     * @param arrNode do uzlu
     * @param version do verze
     * @param nodeIdToLevel mapa id uzlu na exportovaný uzel
     * @param relatedEntities vazby na entity které se budou exportova později
     *
     * @return exportovaný uzel
     */
    private Level exportLevel(final ArrNode arrNode, final ArrFundVersion version, final Map<Integer, Level> nodeIdToLevel,
            final RelatedEntities relatedEntities) {
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

        Assert.isTrue(levels.size() == 1, "Node nemůže mít pro jednu změnu/verzi více než jeden level.");

        ArrLevel arrLevel = levels.iterator().next();
        Set<Integer> nodeIds = new HashSet<>();
        nodeIds.add(arrNode.getNodeId());
        List<ArrData> dataList = dataRepository.findDescItemsByNodeIds(nodeIds, null, version);

        Level parent = null;
        ArrNode nodeParent = arrLevel.getNodeParent();
        if (nodeParent != null) {
            parent = nodeIdToLevel.get(nodeParent.getNodeId());
        }

        return createLevel(arrLevel, parent, dataList, relatedEntities);
    }

    /**
     * Vytvoření uzlu.
     *
     * @param arrLevel do level
     * @param parent exprotovaný rodič
     * @param dataList hodnoty uzlu
     * @param relatedEntities vazby na entity které se budou exportova později
     *
     * @return exportovaný uzel
     */
    private Level createLevel(final ArrLevel arrLevel, final Level parent, final List<ArrData> dataList,
            final RelatedEntities relatedEntities) {
        Level level = new Level();

        level.setPosition(arrLevel.getPosition());
        level.setUuid(arrLevel.getNode().getUuid());

        level.setDescItems(createDescItems(dataList, relatedEntities));

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
            relatedEntities.addRecordLevel(r.getRecordId(), level);
        });

        return level;
    }

    /**
     * Vytvoření hodnot atributů.
     *
     * @param dataList do seznam hodnot
     * @param relatedEntities vazby na entity které se budou exportova později
     *
     * @return seznam exportovaných hodnot
     */
    private List<AbstractDescItem> createDescItems(final List<ArrData> dataList,
            final RelatedEntities relatedEntities) {
        Assert.notNull(dataList);

        List<AbstractDescItem> descItems = new ArrayList<>(dataList.size());
        for (ArrData arrData : dataList) {
            ArrDescItem arrdescItem = (ArrDescItem) arrData.getItem();
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

                ParParty parParty = arrDataPartyRef.getParty();
                Integer partyId = parParty.getPartyId();
                // přidat rejstřík z osoby a z roleType
                Integer recordId = parParty.getRecord().getRecordId();
                relatedEntities.addOtherUsedRecords(recordId);
                List<ParRelationEntity> parRelationEntities = relationEntityRepository.findByParty(parParty);
                parRelationEntities.forEach(entity -> {
                    relatedEntities.addOtherUsedRecords(entity.getRecord().getRecordId());
                });


                relatedEntities.addPartyDescItem(partyId, descItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("RECORD_REF")) {
                DescItemRecordRef descItem = new DescItemRecordRef();
                ArrDataRecordRef arrDataRecordRef = (ArrDataRecordRef) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                Integer recordId = arrDataRecordRef.getRecord().getRecordId();
                relatedEntities.addRecordDescItem(recordId, descItem);

                descItems.add(descItem);
            } else if (dataTypeCode.equals("PACKET_REF")) {
                DescItemPacketRef descItem = new DescItemPacketRef();
                ArrDataPacketRef arrDataPacketRef = (ArrDataPacketRef) arrData;

                fillCommonAttributes(descItem, arrdescItem);

                Integer packetId = arrDataPacketRef.getPacket().getPacketId();
                relatedEntities.addPacketDescItem(packetId, descItem);

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
            } else if(dataTypeCode.equals("JSON_TABLE")) {
                DescItemJsonTable descItem = new DescItemJsonTable();
                fillCommonAttributes(descItem, arrdescItem);

                ArrDataJsonTable arrDataJsonTable = (ArrDataJsonTable) arrData;

                ElzaTable table = arrDataJsonTable.getValue();
                descItem.setValue(table.toJsonString(table));

                descItems.add(descItem);
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
        descItem.setDescItemTypeCode(arrDescItem.getItemType().getCode());
        descItem.setPosition(arrDescItem.getPosition());

        RulItemSpec rulDescItemSpec = arrDescItem.getItemSpec();
        if (rulDescItemSpec != null) {
            descItem.setDescItemSpecCode(rulDescItemSpec.getCode());
        }
    }

    /**
     * Najde v cache id rodičů předaného uzlu.
     *
     * @param nodeId id uzlu pro který hledáme rodiče
     * @param versionTreeCache cache
     *
     * @return seznam id rodičů, první prvek je přímý rodič, poslední prvek je root
     */
    private List<Integer> getParentIds(final Integer nodeId, final Map<Integer, TreeNode> versionTreeCache) {
        TreeNode treeNode = versionTreeCache.get(nodeId);
        if (treeNode == null) {
            return null;
        }

        List<Integer> parentIds = new LinkedList<>();
        while (treeNode != null) {
            treeNode = treeNode.getParent();
            if (treeNode != null) {
                Integer parentId = treeNode.getId();
                parentIds.add(parentId);
            }
        }

        return parentIds;
    }

    /**
     * Vytvoření archivního souboru.
     *
     * @param arrFund do archivního souboru
     * @param version verze
     *
     * @return exportovaný archivní soubor
     */
    private Fund createFund(final ArrFund arrFund, final ArrFundVersion version) {
        Fund fund = new Fund();

        RulRuleSet ruleSet = version.getRuleSet();
        fund.setInstitutionCode(arrFund.getInstitution().getInternalCode());

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

    /**
     * Záznam s id rejstříku a jeho hloubkou ve stromu rejstříků kvůli seřazení.
     */
    private class RecordEntry {

        private Integer recordId;

        private Integer depth;

        public RecordEntry(final Integer recordId, final Integer depth) {
            this.recordId = recordId;
            this.depth = depth;
        }

        public Integer getRecordId() {
            return recordId;
        }

        public Integer getDepth() {
            return depth;
        }
    }

    /**
     * Vazby na entity které se budou exportova později.
     */
    private class RelatedEntities {

        /** Mapa do které se naplní id rejstříku a seznam levelů které na něj odkazují. */
        private Map<Integer, List<Level>> recordLevels = new HashMap<>();

        /** Mapa do které se naplní id rejstříku a seznam hodnot atributů které na něj odkazují. */
        private Map<Integer, List<DescItemRecordRef>> recordDescItems = new HashMap<>();

        /** Mapa do které se naplní id osoby a seznam hodnot atributů které na ni odkazují. */
        private Map<Integer, List<DescItemPartyRef>> partyDescItems = new HashMap<>();

        /** Mapa do které se naplní id obalu a seznam hodnot atributů které na něj odkazují. */
        private Map<Integer, List<DescItemPacketRef>> packetDescItems = new HashMap<>();

        /** Použité rejstříky z dalších entit. */
        private Set<Integer> otherUsedRecords = new HashSet<>();

        public void addRecordLevel(final Integer recordId, final Level level) {
            Assert.notNull(recordId);
            Assert.notNull(level);

            List<Level> levels = recordLevels.get(recordId);
            if (levels == null) {
                levels = new LinkedList<>();
                recordLevels.put(recordId, levels);
            }
            levels.add(level);
        }

        public void addRecordDescItem(final Integer recordId, final DescItemRecordRef descItem) {
            Assert.notNull(recordId);
            Assert.notNull(descItem);

            List<DescItemRecordRef> descItemsRecordRef = recordDescItems.get(recordId);
            if (descItemsRecordRef == null) {
                descItemsRecordRef = new LinkedList<>();
                recordDescItems.put(recordId, descItemsRecordRef);
            }
            descItemsRecordRef.add(descItem);
        }

        public void addPartyDescItem(final Integer partyId, final DescItemPartyRef descItem) {
            Assert.notNull(partyId);
            Assert.notNull(descItem);

            List<DescItemPartyRef> descItemsPartyRef = partyDescItems.get(partyId);
            if (descItemsPartyRef == null) {
                descItemsPartyRef = new LinkedList<>();
                partyDescItems.put(partyId, descItemsPartyRef);
            }
            descItemsPartyRef.add(descItem);
        }

        public void addPacketDescItem(final Integer packetId, final DescItemPacketRef descItem) {
            Assert.notNull(packetId);
            Assert.notNull(descItem);

            List<DescItemPacketRef> descItemsPacketRef = packetDescItems.get(packetId);
            if (descItemsPacketRef == null) {
                descItemsPacketRef = new LinkedList<>();
                packetDescItems.put(packetId, descItemsPacketRef);
            }
            descItemsPacketRef.add(descItem);
        }

        public void addOtherUsedRecords(final Integer recordId) {
            Assert.notNull(recordId);

            otherUsedRecords.add(recordId);
        }

        public Map<Integer, List<Level>> getRecordLevels() {
            return recordLevels;
        }

        public Map<Integer, List<DescItemRecordRef>> getRecordDescItems() {
            return recordDescItems;
        }

        public Map<Integer, List<DescItemPartyRef>> getPartyDescItems() {
            return partyDescItems;
        }

        public Map<Integer, List<DescItemPacketRef>> getPacketDescItems() {
            return packetDescItems;
        }

        public Set<Integer> getOtherUsedRecords() {
            return otherUsedRecords;
        }
    }
}
