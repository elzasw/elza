package cz.tacr.elza.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.xml.bind.JAXBException;

import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrItemPartyRef;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
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
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.CalendarConverter.CalendarType;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.vo.XmlImportType;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.InstitutionTypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegExternalSystemRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventId;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.exception.FatalXmlImportException;
import cz.tacr.elza.service.exception.InvalidDataException;
import cz.tacr.elza.service.exception.LevelImportException;
import cz.tacr.elza.service.exception.NonFatalXmlImportException;
import cz.tacr.elza.service.exception.PartyImportException;
import cz.tacr.elza.service.exception.RecordImportException;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.utils.XmlUtils;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
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
 * Import dat z xml.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 11. 2015
 */
@Service
public class XmlImportService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private PartyService partyService;

    @Autowired
    private RegistryService registryServiceService;

    @Autowired
    private RegExternalSystemRepository regExternalSystemRepository;

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
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private InstitutionTypeRepository institutionTypeRepository;

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
    private UnitdateRepository unitdateRepository;

    @Autowired
    private PartyCreatorRepository partyCreatorRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private ArrangementCacheService arrangementCacheService;

    @Value("${elza.xmlImport.transformationDir}")
    private String transformationsDirectory;

    /**
     * Naimportuje data.
     *
     * @throws XmlImportException chyba při importu
     */
    @Transactional(rollbackOn = Throwable.class)
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ADMIN})
    public void importData(final XmlImportConfig config) throws XmlImportException {
        // transformace dat
        XmlImport xmlImport = readData(config);

        // najít použité rejstříky a osoby
        Map<String, RecordVO> usedRecords = new HashMap<>();
        Set<String> usedParties = new HashSet<>();
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

        checkData(xmlImport, usedRecords, usedParties, importAllRecords, importAllParties, importFund);
        pairRecords(usedRecords);

        // rejstříky - párovat podle uuid nebo ext id a ext systému
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
            ArrChange change = arrangementService.createChange(ArrChange.Type.IMPORT);
            ArrFund fund = createFund(xmlImport.getFund(), change, config, stopOnError);

            // importovat
            ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fund.getFundId());
            ArrNode rootNode = fundVersion.getRootNode();

            Map<String, ArrPacket> xmlIdIntIdPacketMap = importPackets(xmlImport, stopOnError, fund);
            importFund(xmlImport.getFund(), change, rootNode, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap,
                    config, fund);
        }
    }

    /** Napáruje rejstříky na ty v db. */
    private void pairRecords(final Map<String, RecordVO> usedRecords) {
        Map<String, RecordVO> uuidRecordMap = usedRecords.values().
                stream().
                filter(r -> StringUtils.isNotBlank(r.getUuid())).
                collect(Collectors.toMap(RecordVO::getUuid, Function.identity()));

        if (!uuidRecordMap.isEmpty()) {
            List<Object[]> rows = recordRepository.findUuidAndRecordIdByUuid(uuidRecordMap.keySet());
            for (Object[] row : rows) {
                String uuid = (String) row[0];
                Integer recordId = (Integer) row[1];

                uuidRecordMap.get(uuid).setInternalId(recordId);
            }
        }

        // externSystemCode -> externId -> recordVO
        Map<String, Map<String, RecordVO>> systemCodeToExternalIdToRecorVOMap = new HashMap<>();
        usedRecords.values().
            stream().
            filter(r -> r.getInternalId() == null).
            filter(r -> StringUtils.isNotBlank(r.getExternSystemCode())).
            filter(r -> StringUtils.isNotBlank(r.getExternId())).
            forEach(r -> {
                Map<String, RecordVO> externIdToRecordVOMap = systemCodeToExternalIdToRecorVOMap.get(r.getExternSystemCode());
                if (externIdToRecordVOMap == null) {
                    externIdToRecordVOMap = new HashMap<>();
                    systemCodeToExternalIdToRecorVOMap.put(r.getExternSystemCode(), externIdToRecordVOMap);
                }
                externIdToRecordVOMap.put(r.getExternId(), r);
            });

        for (String systemCode : systemCodeToExternalIdToRecorVOMap.keySet()) {
            Map<String, RecordVO> externIdToRecordVOMap = systemCodeToExternalIdToRecorVOMap.get(systemCode);
            if (!externIdToRecordVOMap.isEmpty()) {
                Set<String> externIds = externIdToRecordVOMap.keySet();
                List<Object[]> rows = recordRepository.findExternIdAndRecordIdBySystemCodeAndExternalIds(systemCode, externIds);
                for (Object[] row : rows) {
                    String externalId = (String) row[0];
                    Integer recordId = (Integer) row[1];

                    externIdToRecordVOMap.get(externalId).setInternalId(recordId);
                }
            }
        }
    }

    private void deleteFundIfExists(final Level rootLevel) {
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

    private Map<String, ArrPacket> importPackets(final XmlImport xmlImport, final boolean stopOnError,
            final ArrFund fund) throws NonFatalXmlImportException {
        Map<String, ArrPacket> xmlIdIntIdPacketMap;
        try {
            xmlIdIntIdPacketMap = importPackets(xmlImport.getPackets(), fund, stopOnError);
        } catch (NonFatalXmlImportException e) {
            if (stopOnError) {
                throw e;
            }
            xmlIdIntIdPacketMap = new HashMap<>();
        }
        return xmlIdIntIdPacketMap;
    }

    private Map<String, ParParty> importParties(final XmlImport xmlImport, final Set<String> usedParties, final boolean stopOnError,
            final Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
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

    private Map<String, RegRecord> importRecords(final XmlImport xmlImport, final Map<String, RecordVO> usedRecords, final boolean stopOnError,
            final RegScope regScope) throws NonFatalXmlImportException {
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

    private void importFund(final Fund fund, final ArrChange change, final ArrNode rootNode, final Map<String, RegRecord> xmlIdIntIdRecordMap,
                            final Map<String, ParParty> xmlIdIntIdPartyMap, final Map<String, ArrPacket> xmlIdIntIdPacketMap,
                            final XmlImportConfig config, final ArrFund arrFund) throws LevelImportException, InvalidDataException {
        Level rootLevel = fund.getRootLevel();
        int position = 1;

        try {
            importDescItems(rootNode, rootLevel, change, config, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap, xmlIdIntIdPacketMap,
                    config.isStopOnError());
        } catch (InvalidDataException e1) {
            if (config.isStopOnError()) {
                throw e1;
            }
        }

        if (rootLevel.getSubLevels() != null) {
            for (Level level : rootLevel.getSubLevels()) {
                importLevel(level, position++, rootNode, config, change, xmlIdIntIdRecordMap, xmlIdIntIdPartyMap,
                        xmlIdIntIdPacketMap, arrFund);
            }
        }
    }

    private void importLevel(final Level level, final int position, final ArrNode parent, final XmlImportConfig config, final ArrChange change,
                             final Map<String, RegRecord> xmlIdIntIdRecordMap, final Map<String, ParParty> xmlIdIntIdPartyMap,
                             final Map<String, ArrPacket> xmlIdIntIdPacketMap, final ArrFund arrFund)
            throws LevelImportException, InvalidDataException {
        ArrNode arrNode = arrangementService.createNode(XmlImportUtils.trimStringValue(level.getUuid(), StringLength.LENGTH_36,
                config.isStopOnError()), arrFund, change);
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
                            xmlIdIntIdPacketMap, arrFund);
                } catch (NonFatalXmlImportException e) {
                    if (config.isStopOnError()) {
                        throw e;
                    }
                }
            }
        }
    }

    private void importDescItems(final ArrNode node, final Level level, final ArrChange change, final XmlImportConfig config,
            final Map<String, RegRecord> xmlIdIntIdRecordMap, final Map<String, ParParty> xmlIdIntIdPartyMap, final Map<String, ArrPacket> xmlIdIntIdPacketMap,
            final boolean stopOnError) throws LevelImportException, InvalidDataException {
        List<AbstractDescItem> descItems = level.getDescItems();
        if (descItems != null) {
            for (AbstractDescItem descItem : descItems) {

                ArrItemData itemData;

                if (descItem instanceof DescItemCoordinates) {
                    DescItemCoordinates descItemCoordinates = (DescItemCoordinates) descItem;
                    ArrItemCoordinates itemCoordinates = new ArrItemCoordinates();
                    try {
                        itemCoordinates.setValue(new WKTReader().read(descItemCoordinates.getValue()));
                    } catch (ParseException e) {
                        if (stopOnError) {
                            throw new InvalidDataException(e.getMessage());
                        }
                    }
                    itemData = itemCoordinates;
                } else if (descItem instanceof DescItemDecimal) {
                    DescItemDecimal descItemDecimal = (DescItemDecimal) descItem;
                    ArrItemDecimal itemDecimal = new ArrItemDecimal();
                    itemDecimal.setValue(descItemDecimal.getValue());
                    itemData = itemDecimal;
                } else if (descItem instanceof DescItemFormattedText) {
                    DescItemFormattedText descItemFormattedText = (DescItemFormattedText) descItem;
                    ArrItemText itemText = new ArrItemText();
                    itemText.setValue(descItemFormattedText.getValue());
                    itemData = itemText;
                } else if (descItem instanceof DescItemInteger) {
                    DescItemInteger descItemInteger = (DescItemInteger) descItem;
                    ArrItemInt itemInt = new ArrItemInt();
                    itemInt.setValue(descItemInteger.getValue());
                    itemData = itemInt;
                } else if (descItem instanceof DescItemPacketRef) {
                    DescItemPacketRef descItemPacketRef = (DescItemPacketRef) descItem;
                    ArrItemPacketRef itemPacketRef = new ArrItemPacketRef();
                    String storageNumber = descItemPacketRef.getPacket().getStorageNumber();
                    itemPacketRef.setPacket(xmlIdIntIdPacketMap.get(storageNumber));
                    itemData = itemPacketRef;
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef descItemPartyRef = (DescItemPartyRef) descItem;
                    ArrItemPartyRef itemPartyRef = new ArrItemPartyRef();
                    String partyId = descItemPartyRef.getParty().getPartyId();
                    itemPartyRef.setParty(xmlIdIntIdPartyMap.get(partyId));
                    itemData = itemPartyRef;
                } else if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef descItemRecordRef = (DescItemRecordRef) descItem;
                    ArrItemRecordRef itemRecordRef = new ArrItemRecordRef();
                    String recordId = descItemRecordRef.getRecord().getRecordId();
                    itemRecordRef.setRecord(xmlIdIntIdRecordMap.get(recordId));
                    itemData = itemRecordRef;
                } else if (descItem instanceof DescItemString) {
                    DescItemString descItemString = (DescItemString) descItem;
                    ArrItemString itemString = new ArrItemString();
                    itemString.setValue(XmlImportUtils.trimStringValue(descItemString.getValue(), StringLength.LENGTH_1000, stopOnError));
                    itemData = itemString;
                } else if (descItem instanceof DescItemText) {
                    DescItemText descItemText = (DescItemText) descItem;
                    ArrItemText itemText = new ArrItemText();
                    itemText.setValue(descItemText.getValue());
                    itemData = itemText;
                } else if (descItem instanceof DescItemUnitDate) {
                    DescItemUnitDate descItemUnitDate = (DescItemUnitDate) descItem;
                    ArrItemUnitdate itemUnitdate = new ArrItemUnitdate();

                    String calendarTypeCode = descItemUnitDate.getCalendarTypeCode();
                    ArrCalendarType calendarType = calendarTypeRepository.findByCode(calendarTypeCode);

                    itemUnitdate.setCalendarType(calendarType);
                    itemUnitdate.setFormat(descItemUnitDate.getFormat());

                    itemUnitdate.setValueFrom(XmlImportUtils.dateToString(descItemUnitDate.getValueFrom()));
                    itemUnitdate.setValueTo(XmlImportUtils.dateToString(descItemUnitDate.getValueTo()));

                    itemUnitdate.setValueFromEstimated(descItemUnitDate.getValueFromEstimated());
                    itemUnitdate.setValueToEstimated(descItemUnitDate.getValueToEstimated());

                    CalendarType converterCalendarType = CalendarType.valueOf(calendarType.getCode());

                    if (itemUnitdate.getValueFrom() == null) {
                        itemUnitdate.setNormalizedFrom(Long.MIN_VALUE);
                    } else {
                        LocalDateTime fromDateTime = LocalDateTime.parse(itemUnitdate.getValueFrom());
                        long fromSeconds = CalendarConverter.toSeconds(converterCalendarType, fromDateTime);
                        itemUnitdate.setNormalizedFrom(fromSeconds);
                    }

                    if (itemUnitdate.getValueTo() == null) {
                        itemUnitdate.setNormalizedTo(Long.MAX_VALUE);
                    } else {
                        LocalDateTime toDateTime = LocalDateTime.parse(itemUnitdate.getValueTo());
                        long toSeconds = CalendarConverter.toSeconds(converterCalendarType, toDateTime);
                        itemUnitdate.setNormalizedTo(toSeconds);
                    }
                    itemData = itemUnitdate;
                } else if (descItem instanceof DescItemUnitId) {
                    DescItemUnitId descItemUnitId = (DescItemUnitId) descItem;
                    ArrItemUnitid itemUnitid = new ArrItemUnitid();
                    itemUnitid.setValue(XmlImportUtils.trimStringValue(descItemUnitId.getValue(), StringLength.LENGTH_250, stopOnError));
                    itemData = itemUnitid;
                } else if (descItem instanceof DescItemEnum) {
                    itemData = new ArrItemEnum();
                } else {
                    throw new NotImplementedException("Není implementován typ dat: " + descItem.getClass().getSimpleName());
                }

                ArrDescItem arrDescItem;
                try {
                    arrDescItem = createArrDescItem(change, node, descItem);
                    arrDescItem.setItem(itemData);
                } catch (NonFatalXmlImportException e) {
                    if (config.isStopOnError()) {
                        throw e;
                    }
                    continue;
                }

                descItemFactory.saveDescItemWithData(arrDescItem, true);
                arrangementCacheService.createDescItem(arrDescItem.getNodeId(), arrDescItem);
            }
        }
    }

    private ArrDescItem createArrDescItem(final ArrChange change, final ArrNode node, final AbstractDescItem descItem) throws LevelImportException {
        String descItemTypeCode = descItem.getDescItemTypeCode();
        RulItemType descItemType = null;
        if (descItemTypeCode !=  null) {
            descItemType = itemTypeRepository.findOneByCode(descItemTypeCode);
            if (descItemType == null) {
                throw new LevelImportException("Chybí desc item type");
            }
        } else {
            throw new LevelImportException("Chybí desc item type code");
        }

        String descItemSpecCode = descItem.getDescItemSpecCode();
        RulItemSpec descItemSpec = null;
        if (descItemSpecCode !=  null) {
            descItemSpec = itemSpecRepository.findByItemTypeAndCode(descItemType, descItemSpecCode);
            if (descItemSpec == null) {
                throw new LevelImportException("Neexistuje specifikace s kódem " + descItemSpecCode
                        + " pro desc item type s kódem " + descItemTypeCode);
            }

        }

        ArrDescItem arrDescItem = new ArrDescItem(descItemFactory.createItemByType(descItemType.getDataType()));

        arrDescItem.setCreateChange(change);
        arrDescItem.setNode(node);
        arrDescItem.setPosition(descItem.getPosition());
        arrDescItem.setDescItemObjectId(arrangementService.getNextDescItemObjectId());
        arrDescItem.setItemSpec(descItemSpec);
        arrDescItem.setItemType(descItemType);

        return arrDescItem;
    }

    private ArrFund createFund(final Fund fund, final ArrChange change, final XmlImportConfig config, final boolean stopOnError) throws FatalXmlImportException, InvalidDataException {
        RulRuleSet ruleSet;
        if (StringUtils.isBlank(config.getTransformationName())) {
            String ruleSetCode = fund.getRuleSetCode();
            ruleSet = ruleSetRepository.findByCode(ruleSetCode);
            if (ruleSet == null) {
                throw new FatalXmlImportException("Nebyla nalezena pravidla s kódem " + ruleSetCode);
            }
        } else {
            ruleSet = ruleSetRepository.findOne(config.getRuleSetId());
        }

        ParInstitution institution = getInstitution(fund.getInstitutionCode());
        String uuid = XmlImportUtils.trimStringValue(fund.getRootLevel().getUuid(), StringLength.LENGTH_36, stopOnError);
        ArrFund arrFund = arrangementService.createFund(fund.getName(), ruleSet, change, uuid, "TST", institution, null); // TODO: dateRange zatím nevyplněn, internalCode TST, instituce
        arrangementService.addScopeToFund(arrFund, config.getRegScope());

        return arrFund;
    }

    /**
     * Najde instituci podle kódu.
     *
     * @param institutionCode kód instituce
     *
     * @return pokud instituce existuje tak ji vrátí jinak vyhodí výjimku
     * @throws FatalXmlImportException kódje prázdný nebo instituce s kódem neexistuje
     */
    private ParInstitution getInstitution(final String institutionCode) throws FatalXmlImportException {
        if (StringUtils.isBlank(institutionCode)) {
            throw new FatalXmlImportException("Kód instituce musí být vyplněn.");
        }

        ParInstitution institution = institutionRepository.findByInternalCode(institutionCode);
        if (institution == null) {
            throw new FatalXmlImportException("Instituce s kódem " + institutionCode + " neexistuje.");
        }

        return institution;
    }

    private void checkData(final XmlImport xmlImport, final Map<String, RecordVO> usedRecords, final Set<String> usedParties,
            final boolean importAllRecords, final boolean importAllParties, final boolean importFund) throws FatalXmlImportException {
        Fund fund = xmlImport.getFund();
        List<AbstractParty> parties = xmlImport.getParties();

        if (importFund) {
            if (fund == null) {
                throw new FatalXmlImportException("V datech chybí archivní pomůcka.");
            }

            Level rootLevel = fund.getRootLevel();
            checkLevel(rootLevel, usedRecords, usedParties);
        }

        if (importAllParties) {
            if (parties != null) {
                for (AbstractParty party: parties) {
                    usedParties.add(party.getPartyId());
                    addPartyRecords(usedRecords, party);
                }
            }
        }

        if (importAllRecords || !usedRecords.isEmpty()) { // přidání všech rejstříků nebo doplnění parentů k rejstříkům použitým ve stromu a u osob
            List<Record> records = xmlImport.getRecords();
            if (records != null) {
                records.forEach(r -> addUsedRecord(r, importAllRecords, usedRecords));
            }
        }
    }

    private void addPartyRecords(final Map<String, RecordVO> usedRecords, final AbstractParty party) throws FatalXmlImportException {
        Record partyRecord = party.getRecord();
        if (partyRecord == null) {
            throw new FatalXmlImportException("Osoba s id " + party.getPartyId() + " nemá rejstřík.");
        }
        putRecordIntoMap(partyRecord, usedRecords);
        List<Relation> events = party.getEvents();
        if (events != null) {
            events.forEach(event -> {
                List<RoleType> roleTypes = event.getRoleTypes();
                if (roleTypes != null) {
                    roleTypes.forEach(roleType -> {
                        Record record = roleType.getRecord();
                        if (record != null) {
                            putRecordIntoMap(record, usedRecords);
                        }
                    });
                }
            });
        }
    }

    private boolean addUsedRecord(final Record record, final boolean importAllRecords, final Map<String, RecordVO> usedRecords) {
        boolean usedChild = false;

        if (record.getRecords() != null) {
            for (Record r : record.getRecords()) {
                if (addUsedRecord(r, importAllRecords, usedRecords) && !usedChild) {
                    usedChild = true;
                }
            }
        }

        if (usedChild || importAllRecords) {
            putRecordIntoMap(record, usedRecords);

            return true;
        } else if (usedRecords.containsKey(record.getRecordId())) { // je použitý v FA - v uzlu nebo v atributu
            return true;
        }

        return false;
    }

    private void putRecordIntoMap(final Record record, final Map<String, RecordVO> usedRecords) {
        if (usedRecords.containsKey(record.getRecordId())) {
            return;
        }

        RecordVO recordVO = new RecordVO();
        recordVO.setExternId(record.getExternalId());
        recordVO.setExternSystemCode(record.getExternalSystemCode());
        recordVO.setUuid(record.getUuid());

        usedRecords.put(record.getRecordId(), recordVO);
    }

    private void checkLevel(final Level level, final Map<String, RecordVO> usedRecords, final Set<String> usedParties) throws FatalXmlImportException {
        if (level.getRecords() != null) {
            level.getRecords().forEach(record -> {
                putRecordIntoMap(record, usedRecords);
            });
        }

        if (level.getDescItems() != null) {
            for (AbstractDescItem descItem : level.getDescItems()) {
                if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef recordRefItem = (DescItemRecordRef) descItem;
                    putRecordIntoMap(recordRefItem.getRecord(), usedRecords);
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef partyRefItem = (DescItemPartyRef) descItem;
                    usedParties.add(partyRefItem.getParty().getPartyId());
                    addPartyRecords(usedRecords, partyRefItem.getParty());
                }
            }
        }

        if (level.getSubLevels() != null) {
            for (Level l : level.getSubLevels()) {
                checkLevel(l, usedRecords, usedParties);
            }
        }
    }

    private Map<String, ArrPacket> importPackets(final List<Packet> packets, final ArrFund fund,
            final boolean stopOnError) throws InvalidDataException {
        Map<String, ArrPacket> xmlIdIntIdPacketMap = new HashMap<>();
        if (CollectionUtils.isEmpty(packets)) {
            return xmlIdIntIdPacketMap;
        }

        for (Packet packet : packets) {
            try {
                ArrPacket arrPacket = importPacket(packet, fund, stopOnError);
                xmlIdIntIdPacketMap.put(packet.getStorageNumber(), arrPacket);
            } catch (NonFatalXmlImportException e) {
                if (stopOnError) {
                    throw e;
                }
            }
        }

        return xmlIdIntIdPacketMap;
    }

    private ArrPacket importPacket(final Packet packet, final ArrFund fund, final boolean stopOnError) throws InvalidDataException {
        ArrPacket arrPacket = new ArrPacket();
        arrPacket.setFund(fund);

        PacketState packetState = packet.getState();
        arrPacket.setState(ArrPacket.State.valueOf(packetState.name()));

        String packetTypeCode = packet.getPacketTypeCode();
        if (packetTypeCode != null) {
            RulPacketType arrPacketType = packetTypeRepository.findByCode(packetTypeCode);
            arrPacket.setPacketType(arrPacketType);
        }
        arrPacket.setStorageNumber(XmlImportUtils.trimStringValue(packet.getStorageNumber(), StringLength.LENGTH_50, stopOnError));

        return packetRepository.save(arrPacket);
    }

    private Map<String, ParParty> importParties(final List<AbstractParty> parties, final Set<String> usedParties, final boolean stopOnError,
            final Map<String, RegRecord> xmlIdIntIdRecordMap) throws NonFatalXmlImportException {
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

    private void importCreators(final AbstractParty party, final Map<String, ParParty> xmlIdIntIdPartyMap) throws PartyImportException {
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

    private ParParty importParty(final AbstractParty party, final boolean stopOnError, final Map<String, RegRecord> xmlIdIntIdRecordMap)
        throws NonFatalXmlImportException {
        String recordId = party.getRecord().getRecordId();
        RegRecord regRecord = xmlIdIntIdRecordMap.get(recordId);
        if (regRecord == null) {
            throw new IllegalStateException("Rejsříkové heslo s identifikátorem " + recordId + " nebylo nalezeno.");
        }

        boolean update = false;
        ParParty parParty = partyService.findParPartyByRecord(regRecord);
        if (parParty != null) {
            update = true;
            List<ParRelation> relations = new ArrayList<>(parParty.getRelations());
            if (CollectionUtils.isNotEmpty(relations)) {
                for (ParRelation relation : relations) {
                    partyService.deleteRelation(relation);
                }
            }
            parParty.setRelations(null);
            partyService.saveParty(parParty);
        } else {
            if (party instanceof Dynasty) {
                parParty = new ParDynasty();
            } else if (party instanceof Event) {
                parParty = new ParEvent();
            } else if (party instanceof PartyGroup) {
                parParty = new ParPartyGroup();
            } else if (party instanceof Person) {
                parParty = new ParPerson();
            } else {
                throw new PartyImportException("Neznámý typ osoby " + party);
            }
        }

        if (party instanceof Dynasty) {
            ParDynasty parDynasty = ElzaTools.unproxyEntity(parParty, ParDynasty.class);
            Dynasty dynasty = (Dynasty) party;

            parDynasty.setGenealogy(dynasty.getGenealogy());
        } else if (party instanceof PartyGroup) {
            ParPartyGroup parPartyGroup = ElzaTools.unproxyEntity(parParty, ParPartyGroup.class);
            PartyGroup partyGroup = (PartyGroup) party;

            parPartyGroup.setFoundingNorm(XmlImportUtils.trimStringValue(partyGroup.getFoundingNorm(), StringLength.LENGTH_50, stopOnError));
            parPartyGroup.setOrganization(XmlImportUtils.trimStringValue(partyGroup.getOrganization(), StringLength.LENGTH_1000, stopOnError));
            parPartyGroup.setScope(XmlImportUtils.trimStringValue(partyGroup.getScope(), StringLength.LENGTH_1000, stopOnError));
            parPartyGroup.setScopeNorm(XmlImportUtils.trimStringValue(partyGroup.getScopeNorm(), StringLength.LENGTH_250, stopOnError));
        }

        fillCommonAttributes(parParty, party, xmlIdIntIdRecordMap);

        EventType type = parParty.getPartyId() == null ? EventType.PARTY_CREATE : EventType.PARTY_UPDATE;
        parParty = partyRepository.save(parParty);

        importPartyNames(party, parParty, stopOnError, update);

        importEvents(party.getEvents(), parParty, stopOnError, xmlIdIntIdRecordMap);

        if (party instanceof PartyGroup) {
            PartyGroup partyGroup = (PartyGroup) party;
            ParPartyGroup parPartyGroup = ElzaTools.unproxyEntity(parParty, ParPartyGroup.class);

            importPartyGroupIdentifiers(partyGroup, parPartyGroup, stopOnError, update);
        }

        importPartyInstitution(party, parParty, stopOnError);

        partyRepository.save(parParty);

        eventNotificationService.publishEvent(new EventId(type, parParty.getPartyId()));

        return parParty;
    }

    private void importPartyInstitution(final AbstractParty party, final ParParty parParty, final boolean stopOnError) throws PartyImportException {
        Institution institution = party.getInstitution();
        if (institution == null) {
            return;
        }

        String typeCode = institution.getTypeCode();
        ParInstitutionType parInstitutionType = institutionTypeRepository.findByCode(typeCode);
        if (parInstitutionType == null) {
            if (stopOnError) {
                throw new PartyImportException("Neexistuje typ instituce s kódem " + typeCode);
            }

            return;
        }

        ParInstitution parInstitution = partyService.createInstitution(institution.getInternalCode(), parInstitutionType, parParty);
        partyService.saveInstitution(parInstitution);
    }

    private void importEvents(final List<Relation> events, final ParParty parParty, final boolean stopOnError, final Map<String, RegRecord> xmlIdIntIdRecordMap)
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

    private ParRelation importRelation(final ParParty parParty, final Relation relation, final Map<String, RegRecord> xmlIdIntIdRecordMap,
            final boolean stopOnError)
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

    private ParRelationEntity createRoleType(final Map<String, RegRecord> xmlIdIntIdRecordMap, final ParRelation parRelation,
            final RoleType roleType) throws PartyImportException {
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
        parRelationEntity.setNote(roleType.getNote());

        return parRelationEntity;
    }

    private ParRelation createRelation(final ParParty parParty, final Relation relation, final boolean stopOnError)
            throws PartyImportException, InvalidDataException {
        ParRelation parRelation = new ParRelation();


        String relationTypeCode = relation.getRelationTypeCode();
        String classTypeCode = relation.getClassTypeCode();

        ParRelationType parRelationType = relationTypeRepository.findByCodeAndClassTypeCode(relationTypeCode, classTypeCode);
        if (parRelationType ==  null) {
            throw new PartyImportException("Nebyl nalezen typ vztahu s kódem " + relationTypeCode
                    + " a s třídou " + classTypeCode);
        }

        parRelation.setRelationType(parRelationType);

        parRelation.setFrom(importComplexDate(relation.getFromDate()));
        parRelation.setNote(XmlImportUtils.trimStringValue(relation.getNote(), StringLength.LENGTH_1000, stopOnError));
        parRelation.setParty(parParty);
        parRelation.setTo(importComplexDate(relation.getToDate()));
        parRelation.setSource(relation.getSource());

        return parRelation;
    }

    private void importPartyGroupIdentifiers(final PartyGroup partyGroup, final ParPartyGroup parPartyGroup, final boolean stopOnError, final boolean update) throws InvalidDataException {
        if (update) {
            List<ParPartyGroupIdentifier> partyGroupIdentifiers = parPartyGroup.getPartyGroupIdentifiers();
            partyGroupIdentifierRepository.delete(partyGroupIdentifiers);

            parPartyGroup.setPartyGroupIdentifiers(null);
            partyRepository.save(parPartyGroup);
        }

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

    private ParPartyGroupIdentifier createPartyGroupIdentifier(final ParPartyGroup parPartyGroup, final PartyGroupId partyGroupId, final boolean stopOnError)
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

    private ParUnitdate importComplexDate(final ComplexDate complexDate) throws InvalidDataException {
        ParUnitdate parUnitdate = XmlImportUtils.convertComplexDateToUnitdate(complexDate);

        if (parUnitdate == null) {
            return null;
        }

        return unitdateRepository.save(parUnitdate);
    }

    private void importPartyNames(final AbstractParty party, final ParParty parParty, final boolean stopOnError, final boolean update) throws InvalidDataException, PartyImportException {
        List<ParPartyName> oldNames = null;
        if (update) {
            oldNames = parParty.getPartyNames();
        }

        List<ParComplementType> partyComplementTypes = complementTypeRepository.findComplementTypesByPartyType(parParty.getPartyType());

        ParPartyName parPartyName = importPartyName(party.getPreferredName(), parParty, stopOnError, partyComplementTypes);
        parParty.setPreferredName(parPartyName);


        List<PartyName> variantNames = party.getVariantNames();
        if (variantNames != null) {
            List<ParPartyName> partyNames = new ArrayList<ParPartyName>(variantNames.size());
            for (PartyName variantName : variantNames) {
                ParPartyName newPartyName = importPartyName(variantName, parParty, stopOnError, partyComplementTypes);
                partyNames.add(newPartyName);
            }

            if (!partyNames.isEmpty()) {
                parParty.setPartyNames(partyNames);
            }
        }
        partyRepository.save(parParty);

        if (update && !oldNames.isEmpty()) {
            partyNameRepository.delete(oldNames);
        }
    }

    private void fillCommonAttributes(final ParParty parParty, final AbstractParty party,
            final Map<String, RegRecord> xmlIdIntIdRecordMap)
        throws PartyImportException, InvalidDataException {
        String partyTypeCode = party.getPartyTypeCode();
        ParPartyType partyType = partyTypeRepository.findPartyTypeByCode(partyTypeCode);
        if (partyType == null) {
            throw new PartyImportException("Typ osoby s kódem " + partyTypeCode + " neexistuje.");
        }
        parParty.setPartyType(partyType);

        if (parParty.getRecord() == null) {
            String recordId = party.getRecord().getRecordId();
            RegRecord regRecord = xmlIdIntIdRecordMap.get(recordId);

            ParPartyType registerPartyType = regRecord.getRegisterType().getPartyType();
            if (registerPartyType != null && !registerPartyType.equals(partyType)) {
                throw new IllegalStateException("Typ osoby " + partyType.getCode()
                + " se neshoduje s typem osoby na rejstříku osoby " + registerPartyType.getCode());
            }
            parParty.setRecord(regRecord);
        }

        String characteristics = party.getCharacteristics();
        if (characteristics != null && characteristics.length() > StringLength.LENGTH_1000) {
            characteristics = characteristics.substring(0, StringLength.LENGTH_1000);
        }
        parParty.setCharacteristics(characteristics);

        parParty.setHistory(party.getHistory());
        parParty.setSourceInformation(party.getSourceInformations());
    }

    private ParPartyName importPartyName(final PartyName partyName, final ParParty parParty, final boolean stopOnError, final List<ParComplementType> partyComplementTypes) throws InvalidDataException, PartyImportException {
        ParPartyName parPartyName = new ParPartyName();

        parPartyName.setNote(partyName.getNote());
        parPartyName.setDegreeAfter(XmlImportUtils.trimStringValue(partyName.getDegreeAfter(), StringLength.LENGTH_50, stopOnError));
        parPartyName.setDegreeBefore(XmlImportUtils.trimStringValue(partyName.getDegreeBefore(), StringLength.LENGTH_50, stopOnError));
        parPartyName.setMainPart(XmlImportUtils.trimStringValue(partyName.getMainPart(), StringLength.LENGTH_250, stopOnError));
        parPartyName.setOtherPart(XmlImportUtils.trimStringValue(partyName.getOtherPart(), StringLength.LENGTH_250, stopOnError));
        parPartyName.setParty(parParty);
        parPartyName.setValidFrom(importComplexDate(partyName.getValidFrom()));
        parPartyName.setValidTo(importComplexDate(partyName.getValidTo()));

        ParPartyNameFormType partyNameFormType = null;
        String partyNameFormTypeCode = partyName.getPartyNameFormTypeCode();
        if (StringUtils.isNotBlank(partyNameFormTypeCode)) {
            partyNameFormType = partyNameFormTypeRepository.findByCode(partyNameFormTypeCode);
            if (partyNameFormType == null) {
                throw new PartyImportException("Neexistuje typ formy jména s kódem " + partyNameFormTypeCode);
            }
        }
        parPartyName.setNameFormType(partyNameFormType);

        parPartyName = partyNameRepository.save(parPartyName);

        List<PartyNameComplement> partyNameComplements = partyName.getPartyNameComplements();
        if (partyNameComplements != null) {
            List<ParPartyNameComplement> parPartyNameComplements = new ArrayList<ParPartyNameComplement>(partyNameComplements.size());
            for (PartyNameComplement partyNameComplement : partyNameComplements) {
                try {
                    ParPartyNameComplement parPartyNameComplement = createPartyNameComplement(parPartyName, partyNameComplement, stopOnError, partyComplementTypes);

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

    private ParPartyNameComplement createPartyNameComplement(final ParPartyName parPartyName,
            final PartyNameComplement partyNameComplement, final boolean stopOnError,
            final List<ParComplementType> partyComplementTypes) throws PartyImportException, InvalidDataException {
        ParPartyNameComplement parPartyNameComplement = new ParPartyNameComplement();
        parPartyNameComplement.setComplement(XmlImportUtils.trimStringValue(partyNameComplement.getComplement(), StringLength.LENGTH_1000, stopOnError));

        String partyNameComplementTypeCode = partyNameComplement.getPartyNameComplementTypeCode();
        ParComplementType parComplementType = partyComplementTypes.stream().
                filter(ct -> ct.getCode().equals(partyNameComplementTypeCode)).
                findFirst().
                orElseThrow(() ->
                    new PartyImportException("Neexistuje typ doplňku jména s kódem " + partyNameComplementTypeCode)
                );
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
    private Map<String, RegRecord> importRecords(final List<Record> records, final Map<String, RecordVO> usedRecords, final boolean stopOnError,
            final RegScope regScope) throws NonFatalXmlImportException {
        Map<String, RegRecord> xmlIdIntIdRecordMap = new HashMap<>();
        if (CollectionUtils.isEmpty(records)) {
            return xmlIdIntIdRecordMap;
        }

        for (Record record : records) {
            if (usedRecords.containsKey(record.getRecordId())) {
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

    private void importRecord(final Record record, final RegRecord parent, final boolean stopOnError, final Map<String, RecordVO> usedRecords,
            final Map<String, RegRecord> xmlIdIntIdRecordMap, final RegScope regScope) throws RecordImportException, InvalidDataException {
        String uuid = record.getUuid();
        String externalId = record.getExternalId();
        String externalSystemCode = record.getExternalSystemCode();
        boolean isNew = false;
        boolean update = false;

        RecordVO recordVO = usedRecords.get(record.getRecordId());
        RegRecord regRecord = findExistingRecord(recordVO);

        if (regRecord == null) { // nebyl nalezen podle uuid nebo externalId a externalSourceCode nebo nejsou vyplněné
            if (stopOnError) {
                checkRequiredAttributes(record);
            }
            regRecord = createRecord(externalId, externalSystemCode, regScope, stopOnError, uuid);
            isNew = true;
            update = true;
        } else {
            update = isRecordInXmlNewer(record, regRecord);
        }

        if (update) {
            updateRecord(record, regRecord, parent, stopOnError);
            boolean partySave = false;
            if (isNew) {
                partySave = regRecord.getRegisterType().getPartyType() != null;
            }
            regRecord = registryServiceService.saveRecord(regRecord, partySave);
            syncVariantRecords(record, regRecord, isNew, stopOnError);

            if (record.getRecords() != null) {
                for (Record subRecord : record.getRecords()) {
                    if (usedRecords.containsKey(record.getRecordId())) {
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
        xmlIdIntIdRecordMap.put(record.getRecordId(), regRecord);
    }

    /**
     * Zjistí zda je importovaný rejstřík novější než ten v db.
     *
     * @param record
     * @param regRecord
     *
     * @return příznak zda je importovaný rejstřík novější než ten v db
     */
    private boolean isRecordInXmlNewer(final Record record, final RegRecord regRecord) {
        boolean isRecordInXmlNewer = false;

        Date lastUpdate = record.getLastUpdate();
        if (lastUpdate == null) { // last update je aktuální čas a  datum
            isRecordInXmlNewer = true;
        } else {
            LocalDateTime xmlLastUpdate = LocalDateTime.ofInstant(lastUpdate.toInstant(), ZoneId.systemDefault());

            isRecordInXmlNewer = xmlLastUpdate.isAfter(regRecord.getLastUpdate());
        }

        return isRecordInXmlNewer;
    }

    private RegRecord findExistingRecord(final RecordVO recordVO) {
        Integer internalId = recordVO.getInternalId();
        if (internalId != null) {
            return recordRepository.getOneCheckExist(internalId);
        }

        return null;
    }

    private void syncVariantRecords(final Record record, final RegRecord regRecord, final boolean isNew, final boolean stopOnError)
        throws InvalidDataException {
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

    private void updateRecord(final Record record, final RegRecord regRecord, final RegRecord parent, final boolean stopOnError) throws InvalidDataException {
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

    private RegRecord createRecord(final String externalId, final String externalSystemCode, final RegScope regScope,
            final boolean stopOnError, final String uuid) throws InvalidDataException {
        RegRecord regRecord = new RegRecord();
        regRecord.setExternalId(XmlImportUtils.trimStringValue(externalId, StringLength.LENGTH_250, stopOnError));
        regRecord.setScope(regScope);
        RegExternalSystem externalSystem = regExternalSystemRepository.findExternalSystemByCode(externalSystemCode);
        regRecord.setExternalSystem(externalSystem);
        regRecord.setUuid(uuid);

        return regRecord;
    }

    /**
     * Kontrola povinných atributů rejstříku.
     *
     * @param record rejstřík
     * @throws IllegalStateException pokud nejsou vyplněny povinné atributy
     */
    private void checkRequiredAttributes(final Record record) {
        if (StringUtils.isBlank(record.getPreferredName())) {
            throw new IllegalStateException("Rejstřík s identifikátorem " + record.getRecordId() + " nemá vyplněné heslo.");
        }

        if (StringUtils.isBlank(record.getRegisterTypeCode())) {
            throw new IllegalStateException("Rejstřík s identifikátorem " + record.getRecordId() + " nemá vyplněný typ.");
        }
    }

    private XmlImport readData(final XmlImportConfig config) {
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
            File transformationFile = getTransformationFileByName(transformationName);
            is = XmlUtils.transformXml(xmlFile, transformationFile);
        }

        try {
            XmlImport data = XmlUtils.unmarshallData(is, XmlImport.class);
            updateReferences(data);

            return data;
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

    /** Podle identifikátorů dosadí do entit objekty. */
    private void updateReferences(final XmlImport data) {
        Assert.notNull(data);

        Map<String, Packet> packetMap = getPacketMap(data);
        Map<String, AbstractParty> partyMap = getPartyMap(data);
        Map<String, Record> recordMap = getRecordMap(data);

        // uzly a hodnoty
        updateReferencesInTree(data, packetMap, partyMap, recordMap);
        // party a RoleType
        updateParties(data, partyMap, recordMap);

    }

    private void updateParties(final XmlImport data, final Map<String, AbstractParty> partyMap, final Map<String, Record> recordMap) {
        List<AbstractParty> parties = data.getParties();
        if (parties == null) {
            return;
        }

        for (AbstractParty party : parties) {
            String recordId = party.getRecordId();
            Record record = recordMap.get(recordId);
            if (record == null) {
                throw new IllegalStateException("Nebyl nalezen rejstřík s identifikátorem " + recordId);
            }
            party.setRecord(record);

            List<String> creatorIds = party.getCreatorIds();
            if (creatorIds != null) {
                List<AbstractParty> creators = new ArrayList<>(creatorIds.size());
                party.setCreators(creators);

                for (String creatorId : creatorIds) {
                    AbstractParty creator = partyMap.get(creatorId);
                    if (creator == null) {
                        throw new IllegalStateException("Nebyl nalezens osoba s identifikátorem " + creatorId);
                    }
                    creators.add(creator);
                }
            }

            List<Relation> events = party.getEvents();
            if (events == null) {
                continue;
            }

            for (Relation event : events) {
                List<RoleType> roleTypes = event.getRoleTypes();
                if (roleTypes == null) {
                    continue;
                }

                for (RoleType roleType : roleTypes) {
                    String recordId2 = roleType.getRecordId();
                    Record record2 = recordMap.get(recordId2);
                    if (record2 == null) {
                        throw new IllegalStateException("Nebyl nalezen rejstřík s identifikátorem " + recordId);
                    }
                    roleType.setRecord(record2);
                }
            }
        }
    }

    private void updateReferencesInTree(final XmlImport data, final Map<String, Packet> packetMap,
            final Map<String, AbstractParty> partyMap, final Map<String, Record> recordMap) {
        Fund fund = data.getFund();
        if (fund == null) {
            return;
        }

        Level rootLevel = fund.getRootLevel();
        if (rootLevel == null) {
            return;
        }

        updateLevel(rootLevel, packetMap, partyMap, recordMap);

    }

    private void updateLevel(final Level level, final Map<String, Packet> packetMap, final Map<String, AbstractParty> partyMap,
            final Map<String, Record> recordMap) {
        if (level.getRecordIds() != null) {
            List<Record> records = new ArrayList<>(level.getRecordIds().size());
            level.setRecords(records);

            for (String recordId : level.getRecordIds()) {
                Record record = recordMap.get(recordId);
                if (record == null) {
                    throw new IllegalStateException("Nebyl nalezen rejstřík s identifikátorem " + recordId);
                }
                records.add(record);
            }
        }

        if (level.getDescItems() != null) {
            for (AbstractDescItem descItem : level.getDescItems()) {
                if (descItem instanceof DescItemRecordRef) {
                    DescItemRecordRef recordRefItem = (DescItemRecordRef) descItem;
                    String recordId = recordRefItem.getRecordId();
                    Record record = recordMap.get(recordId);
                    if (record == null) {
                        throw new IllegalStateException("Nebyl nalezen rejstřík s identifikátorem " + recordId);
                    }
                    recordRefItem.setRecord(record);
                } else if (descItem instanceof DescItemPartyRef) {
                    DescItemPartyRef partyRefItem = (DescItemPartyRef) descItem;
                    String partyId = partyRefItem.getPartyId();
                    AbstractParty party = partyMap.get(partyId);
                    if (party == null) {
                        throw new IllegalStateException("Nebyla nalezena osoba s identifikátorem " + partyId);
                    }
                    partyRefItem.setParty(party);
                } else if (descItem instanceof DescItemPacketRef) {
                    DescItemPacketRef packetRefItem = (DescItemPacketRef) descItem;
                    String packetId = packetRefItem.getPacketId();
                    Packet packet = packetMap.get(packetId);
                    if (packet == null) {
                        throw new IllegalStateException("Nebyl nalezen obal s identifikátorem " + packetId);
                    }
                    packetRefItem.setPacket(packet);
                }
            }
        }

        if (level.getSubLevels() != null) {
            for (Level l : level.getSubLevels()) {
                updateLevel(l, packetMap, partyMap, recordMap);
            }
        }
    }

    private Map<String, Record> getRecordMap(final XmlImport data) {
        List<Record> records = data.getRecords();

        if (records == null) {
            return Collections.emptyMap();
        }

        return records.stream().collect(
                Collectors.toMap(Record::getRecordId, Function.identity()));
    }

    private Map<String, AbstractParty> getPartyMap(final XmlImport data) {
        List<AbstractParty> parties = data.getParties();

        if (parties == null) {
            return Collections.emptyMap();
        }

        return parties.stream().collect(
                Collectors.toMap(AbstractParty::getPartyId, Function.identity()));
    }

    private Map<String, Packet> getPacketMap(final XmlImport data) {
        List<Packet> packets = data.getPackets();

        if (packets == null) {
            return Collections.emptyMap();
        }

        return packets.stream().collect(
                Collectors.toMap(Packet::getStorageNumber, Function.identity()));
    }

    private File getTransformationFileByName(final String transformationName) {
        return new File(transformationsDirectory + File.separator + transformationName + XmlUtils.XSLT_EXTENSION);
    }

    /**
     * Vrátí názvy šablon.
     *
     * @return názvy šablon
     */
    public List<String> getTransformationNames() {
        return XmlUtils.getTransformationNames(transformationsDirectory);
    }

    public static class RecordVO {

        private String uuid;

        private String externId;

        private String externSystemCode;

        /** Inerní id v ELZA. */
        private Integer internalId;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(final String uuid) {
            this.uuid = uuid;
        }

        public String getExternId() {
            return externId;
        }

        public void setExternId(final String externId) {
            this.externId = externId;
        }

        public String getExternSystemCode() {
            return externSystemCode;
        }

        public void setExternSystemCode(final String externSystemCode) {
            this.externSystemCode = externSystemCode;
        }

        public Integer getInternalId() {
            return internalId;
        }

        public void setInternalId(final Integer internalId) {
            this.internalId = internalId;
        }
    }
}
