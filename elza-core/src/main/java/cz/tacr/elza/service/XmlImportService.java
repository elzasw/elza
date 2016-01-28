package cz.tacr.elza.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.ImportDataFormat;
import cz.tacr.elza.api.vo.XmlImportConfig;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.exception.LevelImportException;
import cz.tacr.elza.service.exception.PartyImportException;
import cz.tacr.elza.service.exception.RecordImportException;
import cz.tacr.elza.service.exception.XmlImportException;
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
import cz.tacr.elza.xmlimport.v1.vo.arrangement.FindingAid;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Level;
import cz.tacr.elza.xmlimport.v1.vo.arrangement.Packet;
import cz.tacr.elza.xmlimport.v1.vo.party.AbstractParty;
import cz.tacr.elza.xmlimport.v1.vo.party.PartyName;
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
    private FindingAidRepository findingAidRepository;

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

    /**
     * Naimportuje data.
     *
     * @param importDataFormat formát vstupních dat
     * @param xmlFile vstupní data
     * @param stopOnError příznak zda se má import přerušit při první chybě nebo se má pokusit naimportovat co nejvíce
     *          dat
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
        boolean importFindingAid = xmlImport.getFindingAid() != null;
        boolean importParties = xmlImport.getParties() != null;
        boolean importAllRecords = !importFindingAid && !importParties;
        boolean importAllParties = !importFindingAid;
        checkData(xmlImport, usedRecords, usedParties, usedPackets, importAllRecords, importAllParties);

        // rejstříky - párovat podle ext id a ext systému
        Map<String, Integer> xmlIdIntIdRecordMap;
        try {
            xmlIdIntIdRecordMap = importRecords(xmlImport.getRecords(), usedRecords, stopOnError);
        } catch (RecordImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdRecordMap = new HashMap<>();
        }

        // osoby - zakládat nové
        Map<String, Integer> xmlIdIntIdPartyMap;
        try {
            xmlIdIntIdPartyMap = importParties(xmlImport.getParties(), usedParties, stopOnError, xmlIdIntIdRecordMap);
        } catch (PartyImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdPartyMap = new HashMap<>();
        }


        // párování - podle uuid root uzlu pokud existuje
        // smazat fa
        if  (importFindingAid) {
            Level rootLevel = xmlImport.getFindingAid().getRootLevel();
            if (rootLevel != null) {
                // najít fa, smazat
                ArrFindingAid findingAid;
                String rootUuid = rootLevel.getUuid();
                if (StringUtils.isNotBlank(rootUuid)) {
                    findingAid = findingAidRepository.findFindingAidByRootNodeUUID(rootUuid);
                    if (findingAid != null) {
                        arrangementService.deleteFindingAid(findingAid.getFindingAidId());
                    }
                }

                // založit fa
                ArrChange change = arrangementService.createChange();
                findingAid = createFindingAid(xmlImport.getFindingAid(), change, config);
                Map<String, Integer> xmlIdIntIdPacketMap = importPackets(xmlImport.getPackets(), usedPackets, stopOnError, findingAid);
                // importovat
                ArrFindingAidVersion findingAidVersion = arrangementService.getOpenVersionByFindingAidId(findingAid.getFindingAidId());
                ArrNode rootNode = findingAidVersion.getRootLevel().getNode();
                if (StringUtils.isNotBlank(rootUuid)) {
                    rootNode.setUuid(rootLevel.getUuid());
                    nodeRepository.save(rootNode);
                }

                importFindingAid(xmlImport.getFindingAid(), change, rootNode, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap, config);
            }
        }
    }

    private void importFindingAid(FindingAid findingAid, ArrChange change, ArrNode rootNode, Map<String, Integer> xmlIdIntIdRecordMap,
            Map<String, Integer> xmlIdIntIdPartyMap, Map<String, Integer> xmlIdIntIdPacketMap, XmlImportConfig config) throws LevelImportException {
        Level rootLevel = findingAid.getRootLevel();
        int position = 1;

        if (rootLevel.getSubLevels() != null) {
            for (Level level : rootLevel.getSubLevels()) {
                importLevel(level, position++, rootNode, config, change, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap);
            }
        }
    }

    private void importLevel(Level level, int position, ArrNode parent, XmlImportConfig config, ArrChange change, Map<String, Integer> xmlIdIntIdRecordMap, Map<String, Integer> xmlIdIntIdPartyMap, Map<String, Integer> xmlIdIntIdPacketMap) throws LevelImportException {
        ArrNode arrNode = arrangementService.createNode(level.getUuid());
        ArrLevel arrLevel = arrangementService.createLevel(change, arrNode, parent, position);

        importDescItems(arrLevel.getNode(), level, change, config, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap);

        int childPosition = 1;
        if (level.getSubLevels() != null) {
            for (Level subLevel : level.getSubLevels()) {
                try {
                    importLevel(subLevel, childPosition++, arrNode, config, change, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap);
                } catch (LevelImportException e) {
                    if (config.isStopOnError()) {
                        throw e;
                    }
                }
            }
        }
    }

    private void importDescItems(ArrNode node, Level level, ArrChange change, XmlImportConfig config, Map<String, Integer> xmlIdIntIdRecordMap, Map<String, Integer> xmlIdIntIdPartyMap, Map<String, Integer> xmlIdIntIdPacketMap) throws LevelImportException {
        List<AbstractDescItem> descItems = level.getDescItems();
        if (descItems != null) {
            for (AbstractDescItem descItem : descItems) {
                ArrDescItem arrDescItem;
                try {
                    arrDescItem = createArrDescItem(change, node, descItem);
                } catch (LevelImportException e) {
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
                    arrData.setValue(descItemCoordinates.getValue());

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
                    arrData.setPacketId(xmlIdIntIdPacketMap.get(storageNumber));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef descItemPartyRef = (DescItemPartyRef) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("PARTY_REF");

                    ArrDataPartyRef arrData = new ArrDataPartyRef();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String partyId = descItemPartyRef.getParty().getPartyId();
                    arrData.setPartyId(xmlIdIntIdPartyMap.get(partyId));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef descItemRecordRef = (DescItemRecordRef) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("RECORD_REF");

                    ArrDataRecordRef arrData = new ArrDataRecordRef();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);

                    String recordId = descItemRecordRef.getRecord().getRecordId();
                    arrData.setRecordId(xmlIdIntIdRecordMap.get(recordId));

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemString) {
                    DescItemString descItemString = (DescItemString) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("STRING");

                    ArrDataString arrData = new ArrDataString();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemString.getValue());

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
                    arrData.setCalendarTypeId(calendarType.getCalendarTypeId());
                    arrData.setFormat(descItemUnitDate.getFormat());

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date fromDate = descItemUnitDate.getValueFrom();
                    if (fromDate != null) {
                        arrData.setValueFrom(dateFormat.format(fromDate));
                    }

                    Date toDate = descItemUnitDate.getValueTo();
                    if (toDate != null) {
                        arrData.setValueTo(dateFormat.format(toDate));
                    }

                    arrData.setValueFromEstimated(descItemUnitDate.getValueFromEstimated());
                    arrData.setValueToEstimated(descItemUnitDate.getValueToEstimated());

                    dataRepository.save(arrData);
                } else if (descItem instanceof DescItemUnitId) {
                    DescItemUnitId descItemUnitId = (DescItemUnitId) descItem;
                    RulDataType dataType = dataTypeRepository.findByCode("UNITID");

                    ArrDataUnitid arrData = new ArrDataUnitid();
                    arrData.setDataType(dataType);
                    arrData.setDescItem(arrDescItem);
                    arrData.setValue(descItemUnitId.getValue());

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

    private ArrFindingAid createFindingAid(FindingAid findingAid, ArrChange change, XmlImportConfig config) throws XmlImportException {
        ImportDataFormat importDataFormat = config.getImportDataFormat();

        RulArrangementType arrangementType;
        RulRuleSet ruleSet;
        if (importDataFormat == ImportDataFormat.ELZA) {
            String arrangementTypeCode = findingAid.getArrangementTypeCode();
            arrangementType = arrangementTypeRepository.findByCode(arrangementTypeCode);
            if (arrangementType == null) {
                throw new XmlImportException("Nebyl nalezen typ výstupu s kódem " + arrangementTypeCode);
            }
            String ruleSetCode = findingAid.getRuleSetCode();
            ruleSet = ruleSetRepository.findByCode(ruleSetCode);
            if (ruleSet == null) {
                throw new XmlImportException("Nebyla nalezena pravidla s kódem " + ruleSetCode);
            }
        } else { // jen pro SUZAP, INTERPI by se sem nemělo dostat
            arrangementType = arrangementTypeRepository.findOne(config.getArrangementTypeId());
            ruleSet = ruleSetRepository.findOne(config.getRuleSetId());
        }

        return arrangementService.createFindingAid(findingAid.getName(), ruleSet, arrangementType, change);
    }

    private void checkData(XmlImport xmlImport, Set<String> usedRecords, Set<String> usedParties, Set<String> usedPackets, boolean importAllRecords, boolean importAllParties) {
        FindingAid findingAid = xmlImport.getFindingAid();
        List<AbstractParty> parties = xmlImport.getParties();

        if (importAllParties) {
            if (parties != null) {
                parties.forEach(party -> {
                    usedRecords.add(party.getRecord().getRecordId());
                    usedParties.add(party.getPartyId());
                });
            }
        } else {
            Level rootLevel = findingAid.getRootLevel();
            checkLevel(rootLevel, usedRecords, usedParties, usedPackets);
        }

        if (importAllRecords || !usedRecords.isEmpty()) {
            List<Record> records = xmlImport.getRecords();
            if (records != null) {
                records.forEach(r -> addUsedRecord(r, importAllRecords, usedRecords));
            }
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

        level.getDescItems().forEach(descItem -> {
            if (descItem instanceof DescItemRecordRef) {
                DescItemRecordRef recordRefItem = (DescItemRecordRef) descItem;
                usedRecords.add(recordRefItem.getRecord().getRecordId());
            } else if (descItem instanceof DescItemPartyRef) {
                DescItemPartyRef partyRefItem = (DescItemPartyRef) descItem;
                usedParties.add(partyRefItem.getParty().getPartyId());
                usedRecords.add(partyRefItem.getParty().getRecord().getRecordId());
            } else if (descItem instanceof DescItemPacketRef) {
                DescItemPacketRef packetRefItem = (DescItemPacketRef) descItem;
                usedPackets.add(packetRefItem.getPacket().getStorageNumber());
            }
        });

        if (level.getSubLevels() != null) {
            level.getSubLevels().forEach(l -> {
                checkLevel(l, usedRecords, usedParties, usedPackets);
            });
        }
    }


    private Map<String, Integer> importPackets(List<Packet> packets, Set<String> usedPackets, boolean stopOnError, ArrFindingAid findingAid) {
        Map<String, Integer> xmlIdIntIdPacketMap = new HashMap<>();
        if (CollectionUtils.isEmpty(packets)) {
            return xmlIdIntIdPacketMap;
        }

        packets.forEach(packet -> {
            if (usedPackets.contains(packet.getStorageNumber())) {
                ArrPacket arrPacket = importPacket(packet, stopOnError, findingAid);
                xmlIdIntIdPacketMap.put(packet.getStorageNumber(), arrPacket.getPacketId());
            }
        });

        return xmlIdIntIdPacketMap;
    }

    private ArrPacket importPacket(Packet packet, boolean stopOnError, ArrFindingAid findingAid) {
        ArrPacket arrPacket = new ArrPacket();
        arrPacket.setFindingAid(findingAid);
        arrPacket.setInvalidPacket(packet.isInvalid());

        String packetTypeCode = packet.getPacketTypeCode();
        if (packetTypeCode != null) {
            RulPacketType arrPacketType = packetTypeRepository.findByCode(packetTypeCode);
            arrPacket.setPacketType(arrPacketType);
        }
        arrPacket.setStorageNumber(packet.getStorageNumber());

        return packetRepository.save(arrPacket);
    }

    private Map<String, Integer> importParties(List<AbstractParty> parties, Set<String> usedParties, boolean stopOnError, Map<String, Integer> xmlIdIntIdRecordMap) throws PartyImportException {
        Map<String, Integer> xmlIdIntIdPartyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(parties)) {
            return xmlIdIntIdPartyMap;
        }

        for (AbstractParty party : parties) {
            if (usedParties.contains(party.getPartyId())) {
                try {
                    ParParty parParty = importParty(party, stopOnError, xmlIdIntIdRecordMap);
                    xmlIdIntIdPartyMap.put(party.getPartyId(), parParty.getPartyId());
                } catch (PartyImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return xmlIdIntIdPartyMap;
    }

    private ParParty importParty(AbstractParty party, boolean stopOnError, Map<String, Integer> xmlIdIntIdRecordMap) throws PartyImportException {
        ParParty parParty = new ParParty();

        String partyTypeCode = party.getPartyTypeCode();
        ParPartyType partyType = partyTypeRepository.findPartyTypeByCode(partyTypeCode);
        if (partyType == null) {
            throw new PartyImportException("Typ osoby s kódem " + partyTypeCode + " neexistuje.");
        }
        parParty.setPartyType(partyType);


        String recordId = party.getRecord().getRecordId();
        Integer internalRecordId = xmlIdIntIdRecordMap.get(recordId);
        if (internalRecordId == null) {
            throw new IllegalStateException("Rejsříkové heslo s externím identifikátorem " + recordId + " nebylo nalezeno.");
        }
        RegRecord regRecord = recordRepository.findOne(internalRecordId);
        parParty.setRecord(regRecord);

        parParty = partyRepository.save(parParty);

        ParPartyName parPartyName = createPartyName(party);
        parPartyName.setParty(parParty);
        parPartyName = partyNameRepository.save(parPartyName);

        parParty.setPreferredName(parPartyName);
        return partyRepository.save(parParty);
    }

    private ParPartyName createPartyName(AbstractParty party) {
        PartyName preferredName = party.getPreferredName();
        ParPartyName parPartyName = new ParPartyName();

        parPartyName.setNote(preferredName.getNote());
        parPartyName.setDegreeAfter(preferredName.getDegreeAfter());
        parPartyName.setDegreeBefore(preferredName.getDegreeBefore());
        parPartyName.setMainPart(preferredName.getMainPart());
        parPartyName.setOtherPart(preferredName.getOtherPart());

        Date specificDateFrom = preferredName.getValidFrom().getSpecificDateFrom();
        if (specificDateFrom != null) {
            // TODO pro J. Vaněk: opravit pro nový model
            //parPartyName.setValidFrom(LocalDateTime.ofInstant(specificDateFrom.toInstant(), ZoneId.systemDefault()));
        }

        Date specificDateTo = preferredName.getValidTo().getSpecificDateTo();
        if (specificDateTo != null) {
            // TODO pro J. Vaněk: opravit pro nový model
            //parPartyName.setValidTo(LocalDateTime.ofInstant(specificDateTo.toInstant(), ZoneId.systemDefault()));
        }
        return parPartyName;
    }

    /**
     * Import rejstříků.
     *
     * @param records rejstříky
     * @param parent nadřazené rejstříkové heslo
     * @param stopOnError příznak zda se má import přerušit při první chybě nebo se má pokusit naimportovat co nejvíce dat
     * @param usedRecords množina s externími id rejstříků které se mají importovat
     *
     * @return mapa externí id rejstříku -> interní id rejstříku
     */
    private Map<String, Integer> importRecords(List<Record> records, Set<String> usedRecords, boolean stopOnError) throws RecordImportException{
        Map<String, Integer> xmlIdIntIdRecordMap = new HashMap<>();
        if (CollectionUtils.isEmpty(records)) {
            return xmlIdIntIdRecordMap;
        }

        for (Record record : records) {
            if (usedRecords.contains(record.getRecordId())) {
                try {
                    importRecord(record, null, stopOnError, usedRecords, xmlIdIntIdRecordMap);
                } catch (RecordImportException e) {
                    if (stopOnError) {
                        throw e;
                    }
                }
            }
        }

        return xmlIdIntIdRecordMap;
    }

    private void importRecord(Record record, RegRecord parent, boolean stopOnError, Set<String> usedRecords, Map<String, Integer> xmlIdIntIdRecordMap) throws RecordImportException {
        String externalId = record.getExternalId();
        String externalSourceCode = record.getExternalSourceCode();
        RegRecord regRecord;
        boolean isNew = false;

        if (record.isLocal()) {
            // vytvořit lokální
            checkRequiredAttributes(record);
            regRecord = createRecord(null, null, true);
            isNew = true;
        } else if (externalId != null && externalSourceCode != null) {
            // zkusit napárovat -> update, create
            regRecord = recordRepository.findRegRecordByExternalIdAndExternalSourceCode(externalId, externalSourceCode);
            if (regRecord == null) {
                if (stopOnError) {
                    checkRequiredAttributes(record);
                }
                regRecord = createRecord(externalId, externalSourceCode, false);
                isNew = true;
            }
        } else {
            throw new RecordImportException("Globální rejstřík s identifikátorem " + record.getRecordId()
                    + " nemá vyplněné externí id nebo typ zdroje.");
        }

        updateRecord(record, regRecord, parent);
        regRecord = recordRepository.save(regRecord);
        xmlIdIntIdRecordMap.put(record.getRecordId(), regRecord.getRecordId());
        syncVariantRecords(record, regRecord, isNew);

        if (record.getRecords() != null) {
            for (Record subRecord : record.getRecords()) {
                if (usedRecords.contains(record.getRecordId())) {
                    try {
                        importRecord(subRecord, regRecord, stopOnError, usedRecords, xmlIdIntIdRecordMap);
                    } catch (RecordImportException e) {
                        if (stopOnError) {
                            throw e;
                        }
                    }
                }
            };
        }
    }

    private void syncVariantRecords(Record record, RegRecord regRecord, boolean isNew) {
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
        recordsToCreate.forEach(varRec -> {
            RegVariantRecord regVarRecord = new RegVariantRecord();
            regVarRecord.setRecord(varRec.getVariantName());
            regVarRecord.setRegRecord(regRecord);

            variantRecordRepository.save(regVarRecord);
        });
    }

    private void updateRecord(Record record, RegRecord regRecord, RegRecord parent) {
        regRecord.setCharacteristics(record.getCharacteristics());
        regRecord.setNote(record.getNote());
        regRecord.setRecord(record.getPreferredName());

        RegRegisterType regType = registerTypeRepository.findRegisterTypeByCode(record.getRegisterTypeCode());
        if (regType == null) {
            throw new IllegalStateException("Neexistuje typ rejstříku s kódem " + record.getRegisterTypeCode() + ".");
        }
        regRecord.setRegisterType(regType);
    }

    private RegRecord createRecord(String externalId, String externalSourceCode, boolean local) {
        RegRecord regRecord = new RegRecord();
        regRecord.setExternalId(externalId);
        //TODO doplnit scope a odebrat local
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
        Assert.notNull(config.getImportDataFormat());
        Assert.notNull(config.getXmlFile());

        ImportDataFormat importDataFormat = config.getImportDataFormat();
        File xmlFile = config.getXmlFile();

        InputStream is;
        switch (importDataFormat) {
            case ELZA:
                try {
                    is = new FileInputStream(xmlFile);
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException("Chyba při otevírání vstupního souboru.", e);
                }
                break;
            case INTERPI:
            case SUZAP:
                is = transformXml(xmlFile, config.getTransformationFile());
                break;
            default:
                throw new IllegalStateException("Nepodporovaný typ dat " + importDataFormat);
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

    private InputStream transformXml(File xmlFile, File transformationFile)
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

    private StreamSource getStreamSource(File sourceFile) {
        Assert.notNull(sourceFile);

        try {
            logger.info("Otevírání souboru " + sourceFile);
            return new StreamSource(new FileInputStream(sourceFile));
        } catch (IOException ex) {
            throw new IllegalStateException("Chyba při otevírání souboru " + sourceFile, ex);
        }
    }

    /**
     * Převede data z xml do objektu {@link XmlImport}
     *
     * @param xml stream
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
}
