package cz.tacr.elza.controller;

import com.jayway.restassured.RestAssured;
import cz.tacr.elza.ElzaCore;
import cz.tacr.elza.domain.ArrArrangementType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFaChange;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ParAbstractParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.ArrangementTypeRepository;
import cz.tacr.elza.repository.ChangeRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.DescItemConstraintRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.ExternalSourceRepository;
import cz.tacr.elza.repository.FaViewRepository;
import cz.tacr.elza.repository.FindingAidRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import cz.tacr.elza.repository.VersionRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

/**
 * Abstraktní předek pro testy. Nastavuje REST prostředí.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 31. 7. 2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ElzaCore.class)
@IntegrationTest("server.port:0") // zvoli volny port, lze spustit i s aktivni Elzou
@WebAppConfiguration
public abstract class AbstractRestTest {

    protected static final String ARRANGEMENT_MANAGER_URL = "/api/arrangementManager";
    protected static final String RULE_MANAGER_URL = "/api/ruleSetManager";
    protected static final String REGISTRY_MANAGER_URL = "/api/registryManager";
    protected static final String PARTY_MANAGER_URL = "/api/partyManager";

    protected static final String TEST_CODE = "Tcode";
    protected static final String TEST_NAME = "Test name";
    protected static final String TEST_UPDATE_NAME = "Update name";

    protected static final String CONTENT_TYPE_HEADER = "content-type";
    protected static final String JSON_CONTENT_TYPE = "application/json";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ArrangementManager arrangementManager;
    @Autowired
    private ArrangementTypeRepository arrangementTypeRepository;
    @Autowired
    private RuleSetRepository ruleSetRepository;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    protected ChangeRepository changeRepository;
    @Autowired
    protected LevelRepository levelRepository;
    @Autowired
    private FindingAidRepository findingAidRepository;
    @Autowired
    protected DescItemRepository descItemRepository;
    @Autowired
    private DescItemTypeRepository descItemTypeRepository;
    @Autowired
    private DescItemSpecRepository descItemSpecRepository;
    @Autowired
    private DescItemConstraintRepository descItemConstraintRepository;
    @Autowired
    private DataTypeRepository dataTypeRepository;
    @Autowired
    private FaViewRepository faViewRepository;
    @Autowired
    private DataStringRepository arrDataStringRepository;
    @Autowired
    private DataRepository arrDataRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private ExternalSourceRepository externalSourceRepository;
    @Autowired
    protected AbstractPartyRepository abstractPartyRepository;
    @Autowired
    private VariantRecordRepository variantRecordRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;

    @Before
    public void setUp() {
        // nastavi default port pro REST-assured
        RestAssured.port = port;

        // nastavi default URI pro REST-assured. Nejcasteni localhost
        RestAssured.baseURI = RestAssured.DEFAULT_URI;
    }

    @After
    public void setDown() {
        abstractPartyRepository.deleteAll();
        variantRecordRepository.deleteAll();
        recordRepository.deleteAll();
        externalSourceRepository.deleteAll();
        registerTypeRepository.deleteAll();
        descItemConstraintRepository.deleteAll();
        faViewRepository.deleteAll();
        versionRepository.deleteAll();
        arrangementTypeRepository.deleteAll();
        ruleSetRepository.deleteAll();
        findingAidRepository.deleteAll();
        levelRepository.deleteAll();
        arrDataRepository.deleteAll();
        arrDataStringRepository.deleteAll();
        descItemRepository.deleteAll();
        descItemSpecRepository.deleteAll();
        descItemTypeRepository.deleteAll();
        changeRepository.deleteAll();
    }

    protected ArrArrangementType createArrangementType() {
        ArrArrangementType arrangementType = new ArrArrangementType();
        arrangementType.setName(TEST_NAME);
        arrangementType.setCode(TEST_CODE);
        arrangementTypeRepository.save(arrangementType);
        return arrangementType;
    }

    protected RulRuleSet createRuleSet() {
        RulRuleSet ruleSet = new RulRuleSet();
        ruleSet.setName(TEST_NAME);
        ruleSet.setCode(TEST_CODE);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    protected ArrFindingAid createFindingAid(final String name) {
        RulRuleSet ruleSet = createRuleSet();
        ArrArrangementType arrangementType = createArrangementType();

        return arrangementManager.createFindingAid(name, arrangementType.getId(), ruleSet.getId());
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, boolean isLock) {
        ArrFaLevel root = levelRepository.findAll().iterator().next();
        return createFindingAidVersion(findingAid, root, isLock);
    }

    protected ArrFaChange createFaChange(final LocalDateTime changeDate) {
        ArrFaChange resultChange = new ArrFaChange();
        resultChange.setChangeDate(changeDate);
        changeRepository.save(resultChange);
        return resultChange;
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrFaLevel root, boolean isLock) {
        RulRuleSet ruleSet = ruleSetRepository.findAll().iterator().next();
        ArrArrangementType arrangementType = arrangementTypeRepository.findAll().iterator().next();

        return createFindingAidVersion(findingAid, root, ruleSet, arrangementType, isLock);
    }

    protected ArrFaVersion createFindingAidVersion(final ArrFindingAid findingAid, final ArrFaLevel root,
                                                   RulRuleSet ruleSet, ArrArrangementType arrangementType, boolean isLock) {

        ArrFaChange createChange = createFaChange(LocalDateTime.now());

        ArrFaChange lockChange = null;
        if (isLock) {
            lockChange = createFaChange(LocalDateTime.now());
        }

        ArrFaVersion version = new ArrFaVersion();
        version.setArrangementType(arrangementType);
        version.setCreateChange(createChange);
        version.setLockChange(lockChange);
        version.setFindingAid(findingAid);
        version.setRootNode(root);
        version.setRuleSet(ruleSet);

        return versionRepository.save(version);
    }

    protected ArrFaLevel createLevel(final Integer position, final ArrFaLevel parent, final ArrFaChange change) {
        ArrFaLevel level = new ArrFaLevel();
        level.setPosition(position);
        if (parent != null) {
            level.setParentNodeId(parent.getNodeId());
        }
        level.setCreateChange(change);
        Integer maxNodeId = levelRepository.findMaxNodeId();
        if (maxNodeId == null) {
            maxNodeId = 0;
        }
        level.setNodeId(maxNodeId + 1);

        return levelRepository.save(level);
    }

    @Transactional
    protected ArrDescItem createAttributs(final Integer nodeId, final Integer position,
                                          final ArrFaChange change, final int index) {
        RulDescItemType descItemType = createDescItemType(index);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);

        ArrDescItem item = new ArrDescItem();
        item.setNodeId(nodeId);
        item.setPosition(position);
        item.setCreateChange(change);
        item.setDescItemObjectId(1);
        item.setDescItemType(descItemType);
        item.setDescItemSpec(rulDescItemSpec);
        descItemRepository.save(item);
        createData(item, index);
        return item;
    }

    @Transactional
    protected RulDescItemConstraint createConstrain(final int index) {
        RulDescItemType descItemType = createDescItemType(index);
        RulDescItemSpec rulDescItemSpec = createDescItemSpec(descItemType, index);
        RulDescItemConstraint itemConstraint = createDescItemConstrain(descItemType, rulDescItemSpec, index);
        return itemConstraint;
    }

    private ArrData createData(final ArrDescItem item, final int index) {
        ArrDataString dataStr = new ArrDataString();
        dataStr.setDescItem(item);
        RulDataType dataType = dataTypeRepository.getOne(2);
        dataStr.setDataType(dataType);
        dataStr.setValue("str data " + index);
        arrDataStringRepository.save(dataStr);
        return dataStr;
    }

    private RulDescItemType createDescItemType(final int index) {
        RulDescItemType itemType = new RulDescItemType();
        RulDataType dataType = createDataType(index);
        itemType.setSys(true);
        itemType.setDataType(dataType);
        itemType.setCode("DI" + index);
        itemType.setName("Desc Item " + index);
        itemType.setShortcut("DItem " + index);
        itemType.setDescription("popis");
        itemType.setCanBeOrdered(false);
        itemType.setIsValueUnique(false);
        itemType.setUseSpecification(false);
        itemType.setViewOrder(index);
        descItemTypeRepository.save(itemType);
        return itemType;
    }

    private RulDescItemConstraint createDescItemConstrain(final RulDescItemType itemType,
                                                          RulDescItemSpec rulDescItemSpec, final int index) {
        RulDescItemConstraint itemConstrain = new RulDescItemConstraint();
        itemConstrain.setDescItemSpec(rulDescItemSpec);
        itemConstrain.setDescItemType(itemType);
        descItemConstraintRepository.save(itemConstrain);
        return itemConstrain;
    }

    private RulDescItemSpec createDescItemSpec(final RulDescItemType itemType, final int index) {
        RulDescItemSpec rulDescItemSpec = new RulDescItemSpec();
        rulDescItemSpec.setCode("IS" + index);
        rulDescItemSpec.setDescItemType(itemType);
        rulDescItemSpec.setName("Item Spec " + index);
        rulDescItemSpec.setShortcut("ISpec " + index);
        rulDescItemSpec.setDescription("popis");
        rulDescItemSpec.setViewOrder(index);
        descItemSpecRepository.save(rulDescItemSpec);
        return rulDescItemSpec;
    }

    private RulDataType createDataType(final int index) {
        RulDataType dataType = new RulDataType();
        dataType.setCode("DT" + index);
        dataType.setName("Data type " + index);
        dataType.setRegexpUse(false);
        dataType.setTextLenghtLimitUse((index > 1) ? true : false);
        dataType.setDescription("popis");
        dataType.setStorageTable("arr_data_integer");
        dataTypeRepository.save(dataType);
        return dataType;
    }

    protected RulDescItemType createDescItemType(RulDataType rulDataType, Boolean sys, String code, String name, String shortcut, String description, Boolean isValueUnique, Boolean canBeOrdered, Boolean useSpecification, Integer viewOrder) {
        RulDescItemType dataTypeItem = new RulDescItemType();
        dataTypeItem.setDataType(rulDataType);
        dataTypeItem.setSys(sys);
        dataTypeItem.setCode(code);
        dataTypeItem.setName(name);
        dataTypeItem.setShortcut(shortcut);
        dataTypeItem.setDescription(description);
        dataTypeItem.setIsValueUnique(isValueUnique);
        dataTypeItem.setCanBeOrdered(canBeOrdered);
        dataTypeItem.setUseSpecification(useSpecification);
        dataTypeItem.setViewOrder(viewOrder);
        descItemTypeRepository.save(dataTypeItem);
        return dataTypeItem;
    }

    protected RulDataType createDataType(String code, String name, String description, Boolean regexUse, Boolean textLenghtLimitUse, String storageTable) {
        RulDataType dataType = new RulDataType();
        dataType.setCode(code);
        dataType.setName(name);
        dataType.setDescription(description);
        dataType.setRegexpUse(regexUse);
        dataType.setTextLenghtLimitUse(textLenghtLimitUse);
        dataType.setStorageTable(storageTable);
        dataTypeRepository.save(dataType);
        return dataType;
    }

    protected RulDataType getDataType(Integer dataTypeId) {
        return dataTypeRepository.findOne(dataTypeId);
    }

    protected RulDescItemSpec createDescItemSpec(RulDescItemType rulDescItemType, String code, String name, String shortcut, String description, Integer viewOrder) {
        RulDescItemSpec dataSpecItem = new RulDescItemSpec();
        dataSpecItem.setDescItemType(rulDescItemType);
        dataSpecItem.setCode(code);
        dataSpecItem.setName(name);
        dataSpecItem.setShortcut(shortcut);
        dataSpecItem.setDescription(description);
        dataSpecItem.setViewOrder(viewOrder);
        descItemSpecRepository.save(dataSpecItem);
        return dataSpecItem;
    }

    protected RulDescItemConstraint createDescItemConstrain(RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec, ArrFaVersion faVersion, Boolean repeatable, String regexp, Integer textLengthLimit) {
        RulDescItemConstraint itemConstraint = new RulDescItemConstraint();
        itemConstraint.setDescItemType(rulDescItemType);
        itemConstraint.setDescItemSpec(rulDescItemSpec);
        itemConstraint.setVersion(faVersion);
        itemConstraint.setRepeatable(repeatable);
        itemConstraint.setRegexp(regexp);
        itemConstraint.setTextLenghtLimit(textLengthLimit);
        descItemConstraintRepository.save(itemConstraint);
        return itemConstraint;
    }

    protected ArrDescItem createArrDescItem(ArrFaChange createFaChange, ArrFaChange deleteFaChange, Integer descItemObjectId, RulDescItemType rulDescItemType, RulDescItemSpec rulDescItemSpec, Integer nodeId, Integer position) {
        ArrDescItem descItem = new ArrDescItem();
        descItem.setCreateChange(createFaChange);
        descItem.setDeleteChange(deleteFaChange);

        // pokud není vyplněno, vybere další možné
        if(descItemObjectId == null) {
            Integer maxDescItemObjectId = descItemRepository.findMaxDescItemObjectId();
            if (maxDescItemObjectId == null) {
                maxDescItemObjectId = 0;
            }
            descItemObjectId = maxDescItemObjectId+1;
        }

        descItem.setDescItemObjectId(descItemObjectId);
        descItem.setDescItemType(rulDescItemType);
        descItem.setDescItemSpec(rulDescItemSpec);
        descItem.setNodeId(nodeId);
        descItem.setPosition(position);
        descItemRepository.save(descItem);
        return descItem;
    }

    protected RegRecord createRecord(int index) {
        RegRegisterType registerType = new RegRegisterType();
        registerType.setCode("RT" + index);
        registerType.setName("Reg type " + index);
        registerTypeRepository.save(registerType);
        RegRecord record = new RegRecord();
        record.setCharacteristics(" dobrovolný hasičský sbor");
        record.setLocal(Boolean.TRUE);
        record.setRegisterType(registerType);
        record.setRecord("Sbor dobrovolných hasičů Topol");
        recordRepository.save(record);
        return record;
    }

    protected ParPartySubtype findPartySubtype() {
        return partySubtypeRepository.findOne(1);
    }

    protected ParAbstractParty createParAbstractParty() {
        final ParPartySubtype partySubtype = findPartySubtype();
        final RegRecord record = createRecord(1);
        return createParAbstractParty(partySubtype, record);
    }

    protected ParAbstractParty createParAbstractParty(final ParPartySubtype partySubtype, final RegRecord record) {
        ParAbstractParty party = new ParAbstractParty();
        party.setPartySubtype(partySubtype);
        party.setRecord(record);
        abstractPartyRepository.save(party);
        return party;
    }
}
