package cz.tacr.elza.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import cz.tacr.elza.domain.*;
import liquibase.util.file.FilenameUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.exception.FatalXmlImportException;
import cz.tacr.elza.service.exception.InvalidDataException;
import cz.tacr.elza.service.exception.LevelImportException;
import cz.tacr.elza.service.exception.NonFatalXmlImportException;
import cz.tacr.elza.service.exception.PartyImportException;
import cz.tacr.elza.service.exception.RecordImportException;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
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
 * Import dat z xml.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 11. 2015
 */
@Service
public class XmlImportService {

    private static final String XSLT_EXTENSION = ".xslt";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ExternalSourceRepository externalSourceRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private ComplementTypeRepository complementTypeRepository;

    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;

    @Autowired
    private PartyGroupIdentifierRepository partyGroupIdentifierRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private PartyCreatorRepository partyCreatorRepository;

    @Value("${elza.xmlImport.transformationDir}")
    private String transformationsDirectory;

    /**
     * Naimportuje data.
     *
     * @throws XmlImportException chyba při importu
     */
    @Transactional
    public void importData(XmlImportConfig config) throws XmlImportException {
        // transformace dat
        XmlImport xmlImport = readData(config);

        // najít použité rejstříky a osoby
        Set<String> usedRecords = new HashSet<>();
        Set<String> usedParties = new HashSet<>();
        Set<String> usedPackets = new HashSet<>();
        boolean stopOnError = config.isStopOnError();

        boolean importFund;
        boolean importAllRecords;
        boolean importAllParties;

        XmlImportType xmlImportType = config.getXmlImportType();
        switch (xmlImportType) {
            case FUND:
                importFund = true;
                importAllRecords = false;
                importAllParties = false;
                break;
            case PARTY:
                importFund = false;
                importAllRecords = false;
                importAllParties = true;
                break;
            case RECORD:
                importFund = false;
                importAllRecords = true;
                importAllParties = false;
                break;
            default:
                throw new FatalXmlImportException("Neznánmý typ importu: " + xmlImportType);
        }

        checkData(xmlImport, usedRecords, usedParties, usedPackets, importAllRecords, importAllParties, importFund);

        // rejstříky - párovat podle ext id a ext systému
        Map<String, RegRecord> xmlIdIntIdRecordMap = importRecords(xmlImport, usedRecords, stopOnError, config.getRegScope());

        // osoby - zakládat nové
        Map<String, ParParty> xmlIdIntIdPartyMap = importParties(xmlImport, usedParties, stopOnError, xmlIdIntIdRecordMap);


        // párování - podle uuid root uzlu pokud existuje
        // smazat fa
        if  (importFund) {
            Level rootLevel = xmlImport.getFund().getRootLevel();

            // najít fa, smazat
            deleteFundIfExists(rootLevel);

            // založit fa
            ArrChange change = arrangementService.createChange();
            ArrFund fund = createFund(xmlImport.getFund(), change, config, stopOnError);

            // importovat
            ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());
            ArrNode rootNode = fundVersion.getRootNode();

            Map<String, ArrPacket> xmlIdIntIdPacketMap = importPackets(xmlImport, usedPackets, stopOnError, fund);
            importFund(xmlImport.getFund(), change, rootNode, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap,
                    config);
        }
    }

    private void deleteFundIfExists(Level rootLevel) {
        ArrFund fund;
        String rootUuid = rootLevel.getUuid();
        if (StringUtils.isNotBlank(rootUuid)) {
            fund = fundRepository.findFundByRootNodeUUID(rootUuid);
            if (fund != null) {
                arrangementService.deleteFund(fund.getFundId());
                fundRepository.flush();
            }
        }
    }

    private Map<String, ArrPacket> importPackets(XmlImport xmlImport, Set<String> usedPackets, boolean stopOnError,
            ArrFund fund) throws NonFatalXmlImportException {
        Map<String, ArrPacket> xmlIdIntIdPacketMap;
        try {
            xmlIdIntIdPacketMap = importPackets(xmlImport.getPackets(), usedPackets, fund, stopOnError);
        } catch (NonFatalXmlImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdPacketMap = new HashMap<>();
        }
        return xmlIdIntIdPacketMap;
    }

    private Map<String, ParParty> importParties(XmlImport xmlImport, Set<String> usedParties, boolean stopOnError,
            Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
        Map<String, ParParty> xmlIdIntIdPartyMap;
        try {
            xmlIdIntIdPartyMap = importParties(xmlImport.getParties(), usedParties, stopOnError, xmlIdIntIdRecordMap);
        } catch (NonFatalXmlImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdPartyMap = new HashMap<>();
        }
        return xmlIdIntIdPartyMap;
    }

    private Map<String, RegRecord> importRecords(XmlImport xmlImport, Set<String> usedRecords, boolean stopOnError,
            RegScope regScope) throws NonFatalXmlImportException {
        Map<String, RegRecord> xmlIdIntIdRecordMap;
        try {
            xmlIdIntIdRecordMap = importRecords(xmlImport.getRecords(), usedRecords, stopOnError, regScope);
        } catch (NonFatalXmlImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdRecordMap = new HashMap<>();
        }
        return xmlIdIntIdRecordMap;
    }

    private void importFund(Fund fund, ArrChange change, ArrNode rootNode, Map<String, RegRecord> xmlIdIntIdRecordMap,
                            Map<String, ParParty> xmlIdIntIdPartyMap, Map<String, ArrPacket> xmlIdIntIdPacketMap, XmlImportConfig config) throws LevelImportException, InvalidDataException {
        Level rootLevel = fund.getRootLevel();
        int position = 1;

        if (rootLevel.getSubLevels() != null) {
            for (Level level : rootLevel.getSubLevels()) {
                importLevel(level, position++, rootNode, config, change, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap);
            }
        }
    }

    private void importLevel(Level level, int position, ArrNode parent, XmlImportConfig config, ArrChange change,
            Map<String, RegRecord> xmlIdIntIdRecordMap, Map<String, ParParty> xmlIdIntIdPartyMap, Map<String, ArrPacket> xmlIdIntIdPacketMap)
            throws LevelImportException, InvalidDataException {
        ArrNode arrNode = arrangementService.createNode(XmlImportUtils.trimStringValue(level.getUuid(), StringLength.LENGTH_36,
                config.isStopOnError()));
        ArrLevel arrLevel = arrangementService.createLevel(change, arrNode, parent, position);

        try {
            importDescItems(arrLevel.getNode(), level, change, config, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap,
                    config.isStopOnError());
        } catch (InvalidDataException e1) {
            if (config.isStopOnError()) {
                throw e1;
            }
        }

        int childPosition = 1;
        if (level.getSubLevels() != null) {
            for (Level subLevel : level.getSubLevels()) {
                try {
                    importLevel(subLevel, childPosition++, arrNode, config, change, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap,
                            xmlIdIntIdPacketMap);
                } catch (NonFatalXmlImportException e) {
                    if (config.isStopOnError()) {
                        throw e;
                    }
                }
            }
        }
    }

    private void importDescItems(ArrNode node, Level level, ArrChange change, XmlImportConfig config,
            Map<String, RegRecord> xmlIdIntIdRecordMap, Map<String, ParParty> xmlIdIntIdPartyMap, Map<String, ArrPacket> xmlIdIntIdPacketMap,
            boolean stopOnError) throws LevelImportException, InvalidDataException {
        List<AbstractDescItem> descItems = level.getDescItems();
        if (descItems != null) {
            for (AbstractDescItem descItem : descItems) {
                ArrDescItem arrDescItem;
                try {
                    arrDescItem = createArrDescItem(change, node, descItem);
                } catch (NonFatalXmlImportException e) {
                    if (config.isStopOnError()) {
                        throw e;
                    }
                    continue;
                }
                descItemRepository.save(arrDescItem);

                if (descItem instanceof DescItemCoordinates) {
                    DescItemCoordinates descItemCoordinates = (DescItemCoordinates) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("COORDINATES");

                    ArrDataCoordinates arrData = new ArrDataCoordinates();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(XmlImportUtils.trimStringValue(descItemCoordinates.getValue(), StringLength.LENGTH_250, stopOnError));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemDecimal) {
                    DescItemDecimal descItemDecimal = (DescItemDecimal) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("DECIMAL");

                    ArrDataDecimal arrData = new ArrDataDecimal();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemDecimal.getValue());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemFormattedText) {
                    DescItemFormattedText descItemFormattedText = (DescItemFormattedText) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("FORMATTED_TEXT");

                    ArrDataText arrData = new ArrDataText();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemFormattedText.getValue());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemInteger) {
                    DescItemInteger descItemInteger = (DescItemInteger) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("INT");

                    ArrDataInteger arrData = new ArrDataInteger();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemInteger.getValue());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemPacketRef) {
                    DescItemPacketRef descItemPacketRef = (DescItemPacketRef) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("PACKET_REF");

                    ArrDataPacketRef arrData = new ArrDataPacketRef();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String storageNumber = descItemPacketRef.getPacket().getStorageNumber();
                    arrData.setPacket(xmlIdIntIdPacketMap.get(storageNumber));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef descItemPartyRef = (DescItemPartyRef) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("PARTY_REF");

                    ArrDataPartyRef arrData = new ArrDataPartyRef();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String partyId = descItemPartyRef.getParty().getPartyId();
                    arrData.setParty(xmlIdIntIdPartyMap.get(partyId));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef descItemRecordRef = (DescItemRecordRef) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("RECORD_REF");

                    ArrDataRecordRef arrData = new ArrDataRecordRef();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String recordId = descItemRecordRef.getRecord().getRecordId();
                    arrData.setRecord(xmlIdIntIdRecordMap.get(recordId));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemString) {
                    DescItemString descItemString = (DescItemString) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("STRING");

                    ArrDataString arrData = new ArrDataString();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(XmlImportUtils.trimStringValue(descItemString.getValue(), StringLength.LENGTH_1000, stopOnError));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemText) {
                    DescItemText descItemText = (DescItemText) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("TEXT");

                    ArrDataText arrData = new ArrDataText();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemText.getValue());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemUnitDate) {
                    DescItemUnitDate descItemUnitDate = (DescItemUnitDate) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("UNITDATE");

                    ArrDataUnitdate arrData = new ArrDataUnitdate();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String calendarTypeCode = descItemUnitDate.getCalendarTypeCode();
                    ArrCalendarType calendarType = calendarTypeRepository.findByCode(calendarTypeCode);
                    arrData.setCalendarType(calendarType);
                    arrData.setFormat(descItemUnitDate.getFormat());

                    arrData.setValueFrom(XmlImportUtils.dateToString(descItemUnitDate.getValueFrom()));
                    arrData.setValueTo(XmlImportUtils.dateToString(descItemUnitDate.getValueTo()));

                    arrData.setValueFromEstimated(descItemUnitDate.getValueFromEstimated());
                    arrData.setValueToEstimated(descItemUnitDate.getValueToEstimated());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemUnitId) {
                    DescItemUnitId descItemUnitId = (DescItemUnitId) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("UNITID");

                    ArrDataUnitid arrData = new ArrDataUnitid();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(XmlImportUtils.trimStringValue(descItemUnitId.getValue(), StringLength.LENGTH_250, stopOnError));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemEnum) {
                    RulDataType dataType = dataTypeRepository.findByCode("ENUM");

                    ArrDataNull arrData = new ArrDataNull();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    dataRepository.save(arrData);
                }
            }
        }

    }

    private ArrDescItem createArrDescItem(ArrChange change, ArrNode node, AbstractDescItem descItem) throws LevelImportException {
        ArrDescItem arrDescItem = new ArrDescItem();

        arrDescItem.setCreateChange(change);
        arrDescItem.setNode(node);
        arrDescItem.setPosition(descItem.getPosition());
        arrDescItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());

        String descItemTypeCode = descItem.getDescItemTypeCode();
        if (descItemTypeCode !=  null) {
            RulDescItemType descItemType = descItemTypeRepository.findOneByCode(descItemTypeCode);
            if (descItemType == null) {
                throw new LevelImportException("Chybí desc item type");
            }
            arrDescItem.setDescItemType(descItemType);
        } else {
            throw new LevelImportException("Chybí desc item type code");
        }

        String descItemSpecCode = descItem.getDescItemSpecCode();
        if (descItemSpecCode !=  null) {
            RulDescItemSpec descItemSpec = descItemSpecRepository.findByDescItemTypeAndCode(arrDescItem.getDescItemType(), descItemSpecCode);
            if (descItemSpec == null) {
                throw new LevelImportException("Neexistuje specifikace s kódem " + descItemSpecCode
                        + " pro desc item type s kódem " + descItemTypeCode);
            }
            arrDescItem.setDescItemSpec(descItemSpec);
        }

        return arrDescItem;
    }

    private ArrFund createFund(Fund fund, ArrChange change, XmlImportConfig config, boolean stopOnError) throws FatalXmlImportException, InvalidDataException {
        RulArrangementType arrangementType;
        RulRuleSet ruleSet;
        if (StringUtils.isBlank(config.getTransformationName())) {
            String arrangementTypeCode = fund.getArrangementTypeCode();
            arrangementType = arrangementTypeRepository.findByCode(arrangementTypeCode);
            if (arrangementType == null) {
                throw new FatalXmlImportException("Nebyl nalezen typ výstupu s kódem " + arrangementTypeCode);
            }
            String ruleSetCode = fund.getRuleSetCode();
            ruleSet = ruleSetRepository.findByCode(ruleSetCode);
            if (ruleSet == null) {
                throw new FatalXmlImportException("Nebyla nalezena pravidla s kódem " + ruleSetCode);
            }
        } else {
            arrangementType = arrangementTypeRepository.findOne(config.getArrangementTypeId());
            ruleSet = ruleSetRepository.findOne(config.getRuleSetId());
        }

        String uuid = XmlImportUtils.trimStringValue(fund.getRootLevel().getUuid(), StringLength.LENGTH_36, stopOnError);
        ArrFund arrFund = arrangementService.createFund(fund.getName(), ruleSet, change, uuid, "TST", null); // TODO: dateRange zatím nevyplněn, internalCode TST
        arrangementService.addScopeToFund(arrFund, config.getRegScope());

        return arrFund;
    }

    private void checkData(XmlImport xmlImport, Set<String> usedRecords, Set<String> usedParties, Set<String> usedPackets,
            boolean importAllRecords, boolean importAllParties, boolean importFund) throws FatalXmlImportException {
        Fund fund = xmlImport.getFund();
        List<AbstractParty> parties = xmlImport.getParties();

        if (importFund) {
            if (fund == null) {
                throw new FatalXmlImportException("V datech chybí archivní pomůcka.");
            }

            Level rootLevel = fund.getRootLevel();
            checkLevel(rootLevel, usedRecords, usedParties, usedPackets);
        }

        if (importAllParties) {
            if (parties != null) {
                parties.forEach(party -> {
                    usedParties.add(party.getPartyId());
                    addPartyRecords(usedRecords, party);
                });
            }
        }

        if (importAllRecords || !usedRecords.isEmpty()) { // přidání všech rejstříků nebo doplnění parentů k rejstříkům použitým ve stromu a u osob
            List<Record> records = xmlImport.getRecords();
            if (records != null) {
                records.forEach(r -> addUsedRecord(r, importAllRecords, usedRecords));
            }
        }
    }

    private void addPartyRecords(Set<String> usedRecords, AbstractParty party) {
        usedRecords.add(party.getRecord().getRecordId());
        List<Relation> events = party.getEvents();
        if (events != null) {
            events.forEach(event -> {
                List<RoleType> roleTypes = event.getRoleTypes();
                if (roleTypes != null) {
                    roleTypes.forEach(roleType -> {
                        Record record = roleType.getRecord();
                        if (record != null) {
                            usedRecords.add(record.getRecordId());
                        }
                    });
                }
            });
        }
    }

    private boolean addUsedRecord(Record record, boolean importAllRecords, Set<String> usedRecords) {
        boolean usedChild = false;

        if (record.getRecords() != null) {
            for (Record r : record.getRecords()) {
                if (addUsedRecord(r, importAllRecords, usedRecords) && !usedChild) {
                    usedChild = true;
                }
            }
        }

        if (usedChild || importAllRecords) {
            usedRecords.add(record.getRecordId());

            return true;
        } else if (usedRecords.contains(record.getRecordId())) { // je použitý v FA - v uzlu nebo v atributu
            return true;
        }

        return false;
    }

    private void checkLevel(Level level, Set<String> usedRecords, Set<String> usedParties, Set<String> usedPackets) {
        if (level.getRecords() != null) {
            level.getRecords().forEach(record -> {
                usedRecords.add(record.getRecordId());
            });
        }

        if (level.getDescItems() != null) {
            level.getDescItems().forEach(descItem -> {
                if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef recordRefItem = (DescItemRecordRef) descItem;
                    usedRecords.add(recordRefItem.getRecord().getRecordId());
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef partyRefItem = (DescItemPartyRef) descItem;
                    usedParties.add(partyRefItem.getParty().getPartyId());
                    addPartyRecords(usedRecords, partyRefItem.getParty());
                } else if (descItem instanceof DescItemPacketRef) {
                    DescItemPacketRef packetRefItem = (DescItemPacketRef) descItem;
                    usedPackets.add(packetRefItem.getPacket().getStorageNumber());
                }
            });
        }

        if (level.getSubLevels() != null) {
            level.getSubLevels().forEach(l -> {
                checkLevel(l, usedRecords, usedParties, usedPackets);
            });
        }
    }

    private Map<String, ArrPacket> importPackets(List<Packet> packets, Set<String> usedPackets, ArrFund fund,
            boolean stopOnError) throws InvalidDataException {
        Map<String, ArrPacket> xmlIdIntIdPacketMap = new HashMap<>();
        if (CollectionUtils.isEmpty(packets)) {
            return xmlIdIntIdPacketMap;
        }

        for (Packet packet : packets) {
            if (usedPackets.contains(packet.getStorageNumber())) {
                try {
                    ArrPacket arrPacket = importPacket(packet, fund, stopOnError);
                    xmlIdIntIdPacketMap.put(packet.getStorageNumber(), arrPacket);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return xmlIdIntIdPacketMap;
    }

    private ArrPacket importPacket(Packet packet, ArrFund fund, boolean stopOnError) throws InvalidDataException {
        ArrPacket arrPacket = new ArrPacket();
        arrPacket.setFund(fund);
        arrPacket.setInvalidPacket(packet.isInvalid());

        String packetTypeCode = packet.getPacketTypeCode();
        if (packetTypeCode != null) {
            RulPacketType arrPacketType = packetTypeRepository.findByCode(packetTypeCode);
            arrPacket.setPacketType(arrPacketType);
        }
        arrPacket.setStorageNumber(XmlImportUtils.trimStringValue(packet.getStorageNumber(), StringLength.LENGTH_50, stopOnError));

        return packetRepository.save(arrPacket);
    }

    private Map<String, ParParty> importParties(List<AbstractParty> parties, Set<String> usedParties, boolean stopOnError,
            Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
        Map<String, ParParty> xmlIdIntIdPartyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(parties)) {
            return xmlIdIntIdPartyMap;
        }

        for (AbstractParty party : parties) {
            if (usedParties.contains(party.getPartyId())) {
                try {
                    ParParty parParty = importParty(party, stopOnError, xmlIdIntIdRecordMap);
                    xmlIdIntIdPartyMap.put(party.getPartyId(), parParty);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        // import autorů osob
        for (AbstractParty party : parties) {
            if (usedParties.contains(party.getPartyId())) {
                try {
                    importCreators(party, xmlIdIntIdPartyMap);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return xmlIdIntIdPartyMap;
    }

    private void importCreators(AbstractParty party, Map<String, ParParty> xmlIdIntIdPartyMap) throws PartyImportException {
        List<AbstractParty> creators = party.getCreators();
        if (CollectionUtils.isEmpty(creators)) {
            return;
        }

        ParParty parParty = xmlIdIntIdPartyMap.get(party.getPartyId());
        if (parParty == null) {
            throw new PartyImportException("Nebyla nalezena osoba podle externího identifikátoru " + party.getPartyId());
        }

        for (AbstractParty creator : creators) {
            ParParty parPartyCreator = xmlIdIntIdPartyMap.get(creator.getPartyId());
            if (parPartyCreator == null) {
                throw new PartyImportException("Nebyl nalezen autor osoby podle externího identifikátoru " + creator.getPartyId());
            }

            ParCreator parCreator = new ParCreator();
            parCreator.setCreatorParty(parPartyCreator);
            parCreator.setParty(parParty);

            partyCreatorRepository.save(parCreator);
        }
    }

    private ParParty importParty(AbstractParty party, boolean stopOnError, Map<String, RegRecord> xmlIdIntIdRecordMap)
        throws NonFatalXmlImportException {
        ParParty parParty;
        boolean isPartyGroup = false;

        if (party instanceof Dynasty) {
            Dynasty dynasty = (Dynasty) party;
            parParty = createDynasty(dynasty, xmlIdIntIdRecordMap);
        } else if (party instanceof Event) {
            Event event = (Event) party;
            parParty = createEvent(event, xmlIdIntIdRecordMap);
        } else if (party instanceof PartyGroup) {
            PartyGroup partyGroup = (PartyGroup) party;
            parParty = createPartyGroup(partyGroup, xmlIdIntIdRecordMap, stopOnError);
            isPartyGroup = true;
        } else if (party instanceof Person) {
            Person person = (Person) party;
            parParty = createPerson(person, xmlIdIntIdRecordMap);
        } else {
            throw new PartyImportException("Neznámý typ osoby " + party);
        }

        parParty = partyRepository.save(parParty);

        importPartyNames(party, parParty, stopOnError);
        importEvents(party.getEvents(), parParty, stopOnError, xmlIdIntIdRecordMap);

        if (isPartyGroup) {
            PartyGroup partyGroup = (PartyGroup) party;
            ParPartyGroup parPartyGroup = (ParPartyGroup) parParty;
            importPartyGroupIdentifiers(partyGroup, parPartyGroup, stopOnError);
        }


        return partyRepository.save(parParty);
    }

    private void importEvents(List<Relation> events, ParParty parParty, boolean stopOnError, Map<String, RegRecord> xmlIdIntIdRecordMap)
        throws NonFatalXmlImportException {
        if (events != null) {
            for (Relation relation : events) {
                try {
                    importRelation(parParty, relation, xmlIdIntIdRecordMap, stopOnError);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }
    }

    private ParRelation importRelation(ParParty parParty, Relation relation, Map<String, RegRecord> xmlIdIntIdRecordMap,
            boolean stopOnError)
        throws PartyImportException, InvalidDataException {
        ParRelation parRelation = createRelation(parParty, relation, stopOnError);
        relationRepository.save(parRelation);

        List<RoleType> roleTypes = relation.getRoleTypes();
        if (roleTypes != null) {
            for (RoleType roleType : roleTypes) {
                try {
                    ParRelationEntity parRelationEntity = createRoleType(xmlIdIntIdRecordMap, parRelation, roleType);
                    relationEntityRepository.save(parRelationEntity);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return parRelation;
    }

    private ParRelationEntity createRoleType(Map<String, RegRecord> xmlIdIntIdRecordMap, ParRelation parRelation, RoleType roleType)
            throws PartyImportException {
        ParRelationEntity parRelationEntity = new ParRelationEntity();
        Record record = roleType.getRecord();
        RegRecord regRecord = xmlIdIntIdRecordMap.get(record.getRecordId());
        if (regRecord ==  null) {
            throw new PartyImportException("Nebyl nalezen rejstřík podle externího id " + record.getRecordId());
        }
        parRelationEntity.setRecord(regRecord);

        parRelationEntity.setRelation(parRelation);

        String roleTypeCode = roleType.getRoleTypeCode();
        ParRelationRoleType parRelationRoleType = relationRoleTypeRepository.findByCode(roleTypeCode);
        if (parRelationRoleType ==  null) {
            throw new PartyImportException("Nebyl nalezen seznam rolí entit ve vztahu s kódem " + roleTypeCode);
        }
        parRelationEntity.setRoleType(parRelationRoleType);

        return parRelationEntity;
    }

    private ParRelation createRelation(ParParty parParty, Relation relation, boolean stopOnError) throws PartyImportException,
            InvalidDataException {
        ParRelation parRelation = new ParRelation();


        String relationTypeCode = relation.getRelationTypeCode();
        String classTypeCode = relation.getClassTypeCode();

        ParRelationType parRelationType;
        if (classTypeCode == null) {
            parRelationType = relationTypeRepository.findByCodeAndClassTypeIsNull(relationTypeCode);
            if (parRelationType ==  null) {
                throw new PartyImportException("Nebyl nalezen typ vztahu s kódem " + relationTypeCode);
            }
        } else {
            parRelationType = relationTypeRepository.findByCodeAndClassType(relationTypeCode, classTypeCode);
            if (parRelationType ==  null) {
                throw new PartyImportException("Nebyl nalezen typ vztahu s kódem " + relationTypeCode
                        + " a s třídou " + classTypeCode);
            }
        }
        parRelation.setComplementType(parRelationType);

        parRelation.setDateNote(XmlImportUtils.trimStringValue(relation.getDateNote(), StringLength.LENGTH_1000, stopOnError));
        parRelation.setFrom(importComplexDate(relation.getFromDate()));
        parRelation.setNote(XmlImportUtils.trimStringValue(relation.getNote(), StringLength.LENGTH_1000, stopOnError));
        parRelation.setParty(parParty);
        parRelation.setTo(importComplexDate(relation.getToDate()));
        parRelation.setSource(relation.getSource());

        return parRelation;
    }

    private void importPartyGroupIdentifiers(PartyGroup partyGroup, ParPartyGroup parPartyGroup, boolean stopOnError) throws InvalidDataException {
        List<PartyGroupId> partyGroupIds = partyGroup.getPartyGroupIds();
        if (partyGroupIds != null) {
            List<ParPartyGroupIdentifier> parPartyGroupIdentifiers = new ArrayList<ParPartyGroupIdentifier>(partyGroupIds.size());
            for (PartyGroupId partyGroupId : partyGroupIds) {
                try {
                    ParPartyGroupIdentifier parPartyGroupIdentifier = createPartyGroupIdentifier(parPartyGroup,
                            partyGroupId, stopOnError);

                    partyGroupIdentifierRepository.save(parPartyGroupIdentifier);
                    parPartyGroupIdentifiers.add(parPartyGroupIdentifier);
                } catch (InvalidDataException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }

            if (!parPartyGroupIdentifiers.isEmpty()) {
                parPartyGroup.setPartyGroupIdentifiers(parPartyGroupIdentifiers);
                partyRepository.save(parPartyGroup);
            }
        }
    }

    private ParPartyGroupIdentifier createPartyGroupIdentifier(ParPartyGroup parPartyGroup, PartyGroupId partyGroupId, boolean stopOnError)
            throws InvalidDataException {
        ParPartyGroupIdentifier parPartyGroupIdentifier = new ParPartyGroupIdentifier();
        parPartyGroupIdentifier.setIdentifier(XmlImportUtils.trimStringValue(partyGroupId.getId(), StringLength.LENGTH_50, stopOnError));
        parPartyGroupIdentifier.setNote(partyGroupId.getNote());
        parPartyGroupIdentifier.setPartyGroup(parPartyGroup);
        parPartyGroupIdentifier.setSource(XmlImportUtils.trimStringValue(partyGroupId.getSource(), StringLength.LENGTH_50, stopOnError));

        parPartyGroupIdentifier.setFrom(importComplexDate(partyGroupId.getValidFrom()));
        parPartyGroupIdentifier.setTo(importComplexDate(partyGroupId.getValidTo()));

        return parPartyGroupIdentifier;
    }

    private ParUnitdate importComplexDate(ComplexDate complexDate) throws InvalidDataException {
        ParUnitdate parUnitdate = XmlImportUtils.convertComplexDateToUnitdate(complexDate);

        if (parUnitdate == null) {
            return null;
        }

        return unitdateRepository.save(parUnitdate);
    }

    private void importPartyNames(AbstractParty party, ParParty parParty, boolean stopOnError) throws InvalidDataException, PartyImportException {
        ParPartyName parPartyName = importPartyName(party.getPreferredName(), parParty, stopOnError);
        parParty.setPreferredName(parPartyName);


        List<PartyName> variantNames = party.getVariantNames();
        if (variantNames != null) {
            List<ParPartyName> partyNames = new ArrayList<ParPartyName>(variantNames.size());
            for (PartyName variantName : variantNames) {
                ParPartyName newPartyName = importPartyName(variantName, parParty, stopOnError);
                partyNames.add(newPartyName);
            }

            if (!partyNames.isEmpty()) {
                parParty.setPartyNames(partyNames);
            }
        }
        partyRepository.save(parParty);
    }

    private ParPerson createPerson(Person person, Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
        Assert.notNull(person);

        ParPerson parPerson = new ParPerson();
        fillCommonAttributes(parPerson, person, xmlIdIntIdRecordMap);

        return parPerson;
    }

    private ParPartyGroup createPartyGroup(PartyGroup partyGroup, Map<String, RegRecord> xmlIdIntIdRecordMap, boolean stopOnError) throws NonFatalXmlImportException {
        Assert.notNull(partyGroup);

        ParPartyGroup parPartyGroup = new ParPartyGroup();
        fillCommonAttributes(parPartyGroup, partyGroup, xmlIdIntIdRecordMap);

        parPartyGroup.setFoundingNorm(XmlImportUtils.trimStringValue(partyGroup.getFoundingNorm(), StringLength.LENGTH_50, stopOnError));
        parPartyGroup.setOrganization(XmlImportUtils.trimStringValue(partyGroup.getOrganization(), StringLength.LENGTH_1000, stopOnError));
        parPartyGroup.setScope(XmlImportUtils.trimStringValue(partyGroup.getScope(), StringLength.LENGTH_1000, stopOnError));
        parPartyGroup.setScopeNorm(XmlImportUtils.trimStringValue(partyGroup.getScopeNorm(), StringLength.LENGTH_250, stopOnError));

        return parPartyGroup;
    }

    private ParEvent createEvent(Event event, Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
        Assert.notNull(event);

        ParEvent parEvent = new ParEvent();
        fillCommonAttributes(parEvent, event, xmlIdIntIdRecordMap);

        return parEvent;
    }

    private ParDynasty createDynasty(Dynasty dynasty, Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
        Assert.notNull(dynasty);

        ParDynasty parDynasty = new ParDynasty();
        fillCommonAttributes(parDynasty, dynasty, xmlIdIntIdRecordMap);

        parDynasty.setGenealogy(dynasty.getGenealogy());

        return parDynasty;
    }

    private void fillCommonAttributes(ParParty parParty, AbstractParty party, Map<String, RegRecord> xmlIdIntIdRecordMap)
        throws PartyImportException, InvalidDataException {
        String partyTypeCode = party.getPartyTypeCode();
        ParPartyType partyType = partyTypeRepository.findPartyTypeByCode(partyTypeCode);
        if (partyType == null) {
            throw new PartyImportException("Typ osoby s kódem " + partyTypeCode + " neexistuje.");
        }
        parParty.setPartyType(partyType);


        String recordId = party.getRecord().getRecordId();
        RegRecord regRecord = xmlIdIntIdRecordMap.get(recordId);
        if (regRecord == null) {
            throw new IllegalStateException("Rejsříkové heslo s identifikátorem " + recordId + " nebylo nalezeno.");
        }
        parParty.setRecord(regRecord);

        String characteristics = party.getCharacteristics();
        if (characteristics != null && characteristics.length() > StringLength.LENGTH_1000) {
            characteristics = characteristics.substring(0, StringLength.LENGTH_1000);
        }
        parParty.setCharacteristics(characteristics);

        parParty.setHistory(party.getHistory());
        parParty.setSourceInformation(party.getSourceInformations());
        parParty.setFrom(importComplexDate(party.getFromDate()));
        parParty.setTo(importComplexDate(party.getFromDate()));
    }

    private ParPartyName importPartyName(PartyName partyName, ParParty parParty, boolean stopOnError) throws InvalidDataException, PartyImportException {
        ParPartyName parPartyName = new ParPartyName();

        parPartyName.setNote(partyName.getNote());
        parPartyName.setDegreeAfter(XmlImportUtils.trimStringValue(partyName.getDegreeAfter(), StringLength.LENGTH_50, stopOnError));
        parPartyName.setDegreeBefore(XmlImportUtils.trimStringValue(partyName.getDegreeBefore(), StringLength.LENGTH_50, stopOnError));
        parPartyName.setMainPart(XmlImportUtils.trimStringValue(partyName.getMainPart(), StringLength.LENGTH_250, stopOnError));
        parPartyName.setOtherPart(XmlImportUtils.trimStringValue(partyName.getOtherPart(), StringLength.LENGTH_250, stopOnError));
        parPartyName.setParty(parParty);
        parPartyName.setValidFrom(importComplexDate(partyName.getValidFrom()));
        parPartyName.setValidTo(importComplexDate(partyName.getValidTo()));

        String partyNameFormTypeCode = partyName.getPartyNameFormTypeCode();
        ParPartyNameFormType partyNameFormType = partyNameFormTypeRepository.findByCode(partyNameFormTypeCode);
        if (partyNameFormType == null) {
            throw new PartyImportException("Neexistuje typ formy jména s kódem " + partyNameFormTypeCode);
        }
        parPartyName.setNameFormType(partyNameFormType);

        parPartyName = partyNameRepository.save(parPartyName);

        List<PartyNameComplement> partyNameComplements = partyName.getPartyNameComplements();
        if (partyNameComplements != null) {
            List<ParPartyNameComplement> parPartyNameComplements = new ArrayList<ParPartyNameComplement>(partyNameComplements.size());
            for (PartyNameComplement partyNameComplement : partyNameComplements) {
                try {
                    ParPartyNameComplement parPartyNameComplement = createPartyNameComplement(parPartyName, partyNameComplement, stopOnError);

                    partyNameComplementRepository.save(parPartyNameComplement);
                    parPartyNameComplements.add(parPartyNameComplement);
                } catch (PartyImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }

            if (!parPartyNameComplements.isEmpty()) {
                parPartyName.setPartyNameComplements(parPartyNameComplements);
                parPartyName = partyNameRepository.save(parPartyName);
            }
        }

        return parPartyName;
    }

    private ParPartyNameComplement createPartyNameComplement(ParPartyName parPartyName,
            PartyNameComplement partyNameComplement, boolean stopOnError) throws PartyImportException, InvalidDataException {
        ParPartyNameComplement parPartyNameComplement = new ParPartyNameComplement();
        parPartyNameComplement.setComplement(XmlImportUtils.trimStringValue(partyNameComplement.getComplement(), StringLength.LENGTH_1000, stopOnError));

        String partyNameComplementTypeCode = partyNameComplement.getPartyNameComplementTypeCode();
        ParComplementType parComplementType = complementTypeRepository.findByCode(partyNameComplementTypeCode);
        if (parComplementType == null) {
            throw new PartyImportException("Neexistuje typ doplňku jména s kódem " + partyNameComplementTypeCode);
        }
        parPartyNameComplement.setComplementType(parComplementType);
        parPartyNameComplement.setPartyName(parPartyName);
        return parPartyNameComplement;
    }

    /**
     * Import rejstříků.
     *
     * @param records rejstříky
     * @param stopOnError příznak zda se má import přerušit při první chybě nebo se má pokusit naimportovat co nejvíce dat
     * @param usedRecords množina s externími id rejstříků které se mají importovat
     * @param regScope třída rejstříků
     *
     * @return mapa externí id rejstříku -> interní id rejstříku
     */
    private Map<String, RegRecord> importRecords(List<Record> records, Set<String> usedRecords, boolean stopOnError,
            RegScope regScope) throws NonFatalXmlImportException {
        Map<String, RegRecord> xmlIdIntIdRecordMap = new HashMap<>();
        if (CollectionUtils.isEmpty(records)) {
            return xmlIdIntIdRecordMap;
        }

        for (Record record : records) {
            if (usedRecords.contains(record.getRecordId())) {
                try {
                    importRecord(record, null, stopOnError, usedRecords, xmlIdIntIdRecordMap, regScope);
                } catch (NonFatalXmlImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return xmlIdIntIdRecordMap;
    }

    private void importRecord(Record record, RegRecord parent, boolean stopOnError, Set<String> usedRecords,
            Map<String, RegRecord> xmlIdIntIdRecordMap, RegScope regScope) throws RecordImportException, InvalidDataException {
        String externalId = record.getExternalId();
        String externalSourceCode = record.getExternalSourceCode();
        RegRecord regRecord;
        boolean isNew = false;

        if (record.isLocal()) {
            // vytvořit lokální
            checkRequiredAttributes(record);
            regRecord = createRecord(null, null, regScope, stopOnError);
            isNew = true;
        } else if (externalId != null && externalSourceCode != null) {
            // zkusit napárovat -> update, create
            regRecord = recordRepository.findRegRecordByExternalIdAndExternalSourceCode(externalId, externalSourceCode);
            if (regRecord == null) {
                if (stopOnError) {
                    checkRequiredAttributes(record);
                }
                regRecord = createRecord(externalId, externalSourceCode, regScope, stopOnError);
                isNew = true;
            }
        } else {
            throw new RecordImportException("Globální rejstřík s identifikátorem " + record.getRecordId()
                    + " nemá vyplněné externí id nebo typ zdroje.");
        }

        updateRecord(record, regRecord, parent, stopOnError);
        regRecord = recordRepository.save(regRecord);
        xmlIdIntIdRecordMap.put(record.getRecordId(), regRecord);
        syncVariantRecords(record, regRecord, isNew, stopOnError);

        if (record.getRecords() != null) {
            for (Record subRecord : record.getRecords()) {
                if (usedRecords.contains(record.getRecordId())) {
                    try {
                        importRecord(subRecord, regRecord, stopOnError, usedRecords, xmlIdIntIdRecordMap, regScope);
                    } catch (NonFatalXmlImportException e) {
                        if (stopOnError) {
                            throw e;
                        }
                    }
                }
            };
        }
    }

    private void syncVariantRecords(Record record, RegRecord regRecord, boolean isNew, boolean stopOnError) throws InvalidDataException {
        List<RegVariantRecord> existingVariantRecords;
        if (isNew) {
            existingVariantRecords = new ArrayList<>();
        } else {
            existingVariantRecords = variantRecordRepository.findByRegRecordId(regRecord.getRecordId());
        }

        // heslo -> instance, za předpokladu že že různé instance mají různá hesla
        Map<String, RegVariantRecord> existingVarRecordsMap = existingVariantRecords.stream()
                .collect(Collectors.toMap(RegVariantRecord::getRecord, Function.identity()));

        List<VariantRecord> recordsToCreate = new LinkedList<>();
        List<VariantRecord> variantNames = record.getVariantNames();
        if (variantNames != null) {
            variantNames.forEach(varRecord -> {
                if (existingVarRecordsMap.containsKey(varRecord.getVariantName())) {
                    existingVarRecordsMap.remove(varRecord.getVariantName());
                } else {
                    recordsToCreate.add(varRecord);
                }
            });
        }

        // delete
        existingVarRecordsMap.values().forEach(varRecord ->
            variantRecordRepository.delete(varRecord)
        );

        // update - není co aktualizovat

        // create
        for (VariantRecord varRec : recordsToCreate) {
            RegVariantRecord regVarRecord = new RegVariantRecord();
            regVarRecord.setRecord(XmlImportUtils.trimStringValue(varRec.getVariantName(), StringLength.LENGTH_1000, stopOnError));
            regVarRecord.setRegRecord(regRecord);

            variantRecordRepository.save(regVarRecord);
        }
    }

    private void updateRecord(Record record, RegRecord regRecord, RegRecord parent, boolean stopOnError) throws InvalidDataException {
        regRecord.setCharacteristics(record.getCharacteristics());
        regRecord.setNote(record.getNote());
        regRecord.setRecord(XmlImportUtils.trimStringValue(record.getPreferredName(), StringLength.LENGTH_1000, stopOnError));
        regRecord.setParentRecord(parent);

        RegRegisterType regType = registerTypeRepository.findRegisterTypeByCode(record.getRegisterTypeCode());
        if (regType == null) {
            throw new IllegalStateException("Neexistuje typ rejstříku s kódem " + record.getRegisterTypeCode() + ".");
        }
        regRecord.setRegisterType(regType);
    }

    private RegRecord createRecord(String externalId, String externalSourceCode, RegScope regScope, boolean stopOnError) throws InvalidDataException {
        RegRecord regRecord = new RegRecord();
        regRecord.setExternalId(XmlImportUtils.trimStringValue(externalId, StringLength.LENGTH_250, stopOnError));
        regRecord.setScope(regScope);
        RegExternalSource externalSource = externalSourceRepository.findExternalSourceByCode(externalSourceCode);
        regRecord.setExternalSource(externalSource);

        return regRecord;
    }

    /**
     * Kontrola povinných atributů rejstříku.
     *
     * @param record rejstřík
     * @throws IllegalStateException pokud nejsou vyplněny povinné atributy
     */
    private void checkRequiredAttributes(Record record) {
        if (StringUtils.isBlank(record.getPreferredName())) {
            throw new IllegalStateException("Rejstřík s identifikátorem " + record.getRecordId() + " nemá vyplněné heslo.");
        }

        if (StringUtils.isBlank(record.getRegisterTypeCode())) {
            throw new IllegalStateException("Rejstřík s identifikátorem " + record.getRecordId() + " nemá vyplněný typ.");
        }
    }

    private XmlImport readData(XmlImportConfig config) {
        Assert.notNull(config);
        Assert.notNull(config.getXmlFile());
        Assert.notNull(config.getRegScope());

        MultipartFile xmlFile = config.getXmlFile();
        String transformationName = config.getTransformationName();

        InputStream is;
        if (StringUtils.isBlank(transformationName)) {
            try {
                is = xmlFile.getInputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Chyba při otevírání vstupního souboru.", e);
            }
        } else {
            File transformationFile = getTransformationFileByName(config.getTransformationName());
            is = transformXml(xmlFile, transformationFile);
        }

        try {
            return unmarshallData(is);
        } catch (JAXBException e) {
            throw new IllegalStateException("Chyba při převodu dat z xml ", e);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                logger.error("Chyba při zavírání souboru " + is, ex);
            }
        }
    }

    private File getTransformationFileByName(String transformationName) {
        File transformationFile = new File(transformationsDirectory + File.separator + transformationName + XSLT_EXTENSION);
        return transformationFile;
    }

    private InputStream transformXml(MultipartFile xmlFile, File transformationFile)
        throws TransformerFactoryConfigurationError {
        Assert.notNull(xmlFile);

        StreamSource xmlSource = null;
        StreamSource xsltSource = null;
        ByteStreamResult result = null;
        byte[] byteArray = null;
        try {
            xmlSource = getStreamSource(xmlFile);
            xsltSource = getTransformationSource(transformationFile);
            result = new ByteStreamResult(new ByteArrayOutputStream());

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            trans.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            trans.transform(xmlSource, result);

            byteArray = result.toByteArray();
        } catch (TransformerException ex) {
            throw new IllegalStateException("Chyba při transformaci vstupních dat.", ex);
        } finally {
            if (xmlSource != null) {
                try {
                    xmlSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru se vstupními daty.", ex);
                }
            }

            if (xsltSource != null) {
                try {
                    xsltSource.getInputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání souboru s transformací.", ex);
                }
            }

            if (result != null && result.getOutputStream() != null) {
                try {
                    result.getOutputStream().close();
                } catch (IOException ex) {
                    logger.error("Chyba při zavírání výsledného souboru.", ex);
                }
            }
        }

        return new ByteArrayInputStream(byteArray);
    }

    private StreamSource getTransformationSource(File transformationFile) {
        Assert.notNull(transformationFile);

        return getStreamSource(transformationFile);
    }

    private StreamSource getStreamSource(File xmlFile) {
        Assert.notNull(xmlFile);

        try {
            logger.info("Otevírání souboru " + xmlFile);
            return new StreamSource(new FileInputStream(xmlFile));
        } catch (IOException ex) {
            throw new IllegalStateException("Chyba při otevírání souboru " + xmlFile, ex);
        }
    }

    private StreamSource getStreamSource(MultipartFile xmlFile) {
        Assert.notNull(xmlFile);

        try {
            logger.info("Otevírání souboru " + xmlFile);
            return new StreamSource(xmlFile.getInputStream());
        } catch (IOException ex) {
            throw new IllegalStateException("Chyba při otevírání souboru " + xmlFile, ex);
        }
    }

    /**
     * Převede data z xml do objektu {@link XmlImport}
     *
     * @param inputStream stream
     *
     * @return {@link XmlImport}
     */
    private XmlImport unmarshallData(InputStream inputStream) throws JAXBException {
        Assert.notNull(inputStream);

        Unmarshaller unmarshaller = createUnmarshaller();

        return  (XmlImport) unmarshaller.unmarshal(inputStream);
    }

    /**
     * Vytvoří umarshaller pro importní data.
     *
     * @return umarshaller pro importní data
     */
    private Unmarshaller createUnmarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlImport.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        return unmarshaller;
    }

    /**
     * Vrátí názvy šablon.
     *
     * @return názvy šablon
     */
    public List<String> getTransformationNames() {
        File transformDir = new File(transformationsDirectory);

        if (!transformDir.exists()) {
            transformDir.mkdirs();
            return Collections.EMPTY_LIST;
        }

        if (!transformDir.isDirectory()) {
            throw new IllegalStateException("Cesta " + transformDir.getAbsolutePath() + " není adresář.");
        }

        File[] listFiles = transformDir.listFiles((dir, name) -> name.endsWith(XSLT_EXTENSION));
        if (listFiles == null) {
            throw new IllegalStateException("Chyba při načítání souborů z adresáře " + transformDir.getAbsolutePath());
        }
        List<String> transformationNames = new ArrayList<>(listFiles.length);
        for (File file : listFiles) {
            String transformationName = FilenameUtils.getBaseName(file.getName());
            transformationNames.add(transformationName.toLowerCase(Locale.getDefault()));
        }

        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(transformationNames, collator);

        return transformationNames;
    }
}
