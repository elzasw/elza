package cz.tacr.elza.controller.config;

import cz.tacr.elza.FilterTools;
import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrOutputFileVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.DmsFileVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegCoordinatesVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.UISettingsVO;
import cz.tacr.elza.controller.vo.UsrPermissionVO;
import cz.tacr.elza.controller.vo.XmlImportConfigVO;
import cz.tacr.elza.controller.vo.filter.Condition;
import cz.tacr.elza.controller.vo.filter.Filter;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.filter.ValuesTypes;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.CalendarConverter.CalendarType;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.filter.condition.BeginDescItemCondition;
import cz.tacr.elza.filter.condition.ContainDescItemCondition;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.filter.condition.EmptyPacketTypeDescItemCondition;
import cz.tacr.elza.filter.condition.EndDescItemCondition;
import cz.tacr.elza.filter.condition.EqDescItemCondition;
import cz.tacr.elza.filter.condition.EqIntervalDesCitemCondition;
import cz.tacr.elza.filter.condition.GeDescItemCondition;
import cz.tacr.elza.filter.condition.GtDescItemCondition;
import cz.tacr.elza.filter.condition.IntersectDescItemCondition;
import cz.tacr.elza.filter.condition.Interval;
import cz.tacr.elza.filter.condition.IntervalDescItemCondition;
import cz.tacr.elza.filter.condition.LeDescItemCondition;
import cz.tacr.elza.filter.condition.LtDescItemCondition;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import cz.tacr.elza.filter.condition.NeDescItemCondition;
import cz.tacr.elza.filter.condition.NoValuesCondition;
import cz.tacr.elza.filter.condition.NotContainDescItemCondition;
import cz.tacr.elza.filter.condition.NotEmptyDescItemCondition;
import cz.tacr.elza.filter.condition.NotEmptyPacketTypeDescItemCondition;
import cz.tacr.elza.filter.condition.NotIntervalDescItemCondition;
import cz.tacr.elza.filter.condition.SelectedAndEmptyPacketTypeDescItemCondition;
import cz.tacr.elza.filter.condition.SelectedSpecificationsDescItemEnumCondition;
import cz.tacr.elza.filter.condition.SelectedValuesDescItemEnumCondition;
import cz.tacr.elza.filter.condition.SelectsNothingCondition;
import cz.tacr.elza.filter.condition.SubsetDescItemCondition;
import cz.tacr.elza.filter.condition.UndefinedDescItemCondition;
import cz.tacr.elza.filter.condition.UnselectedSpecificationsDescItemEnumCondition;
import cz.tacr.elza.filter.condition.UnselectedValuesDescItemEnumCondition;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Továrna na vytváření DO objektů z VO objektů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 07.01.2016
 */
@Service
public class ClientFactoryDO {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    /**
     * Vytvoří node z VO.
     * @param nodeVO vo node
     * @return DO node
     */
    public ArrNode createNode(final ArrNodeVO nodeVO){
        Assert.notNull(nodeVO, "JP musí být vyplněna");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(nodeVO, ArrNode.class);
    }

    /**
     * Vytvoří seznam DO z VO.
     * @param nodeVoList VO seznam nodů
     * @return DO seznam nodů
     */
    public List<ArrNode> createNodes(final Collection<ArrNodeVO> nodeVoList){
        Assert.notNull(nodeVoList, "Seznam JP musí být vyplněn");

        List<ArrNode> result = new ArrayList<>(nodeVoList.size());
        for (ArrNodeVO arrNodeVO : nodeVoList) {
            result.add(createNode(arrNodeVO));
        }

        return result;
    }

    /**
     * Vytvoří objekt osoby z předaného VO.
     *
     * @param partyVO VO osoby
     * @return objekt osoby
     */
    public ParParty createParty(final ParPartyVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ParParty party = mapper.map(partyVO, ParParty.class);

        if (CollectionUtils.isNotEmpty(partyVO.getPartyNames())) {
            List<ParPartyName> partyNames = new ArrayList<>(partyVO.getPartyNames().size());
            for (ParPartyNameVO partyName : partyVO.getPartyNames()) {
                ParPartyName partyNameDo = mapper.map(partyName, ParPartyName.class);
                if (partyName.isPrefferedName()) {
                    party.setPreferredName(partyNameDo);
                }
                partyNames.add(partyNameDo);
            }
            party.setPartyNames(partyNames);
        }

        return party;
    }




    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param regRecordVO VO rejstříkové heslo
     * @return DO rejstříkové heslo
     */
    public RegRecord createRegRecord(final RegRecordVO regRecordVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regRecordVO, RegRecord.class);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param regVariantRecord VO variantní rejstříkové heslo
     * @return DO variantní rejstříkové heslo
     */
    public RegVariantRecord createRegVariantRecord(final RegVariantRecordVO regVariantRecord) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regVariantRecord, RegVariantRecord.class);
    }

    /**
     * Vytvoření hodnoty atributu.
     *
     * @param descItemVO     VO hodnoty atributu
     * @param descItemTypeId identiifkátor typu hodnoty atributu
     * @return
     */
    public ArrDescItem createDescItem(final ArrItemVO descItemVO, final Integer descItemTypeId) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrData data = mapper.map(descItemVO, ArrData.class);
        ArrDescItem descItem = new ArrDescItem();
        descItem.setData(data);

        RulItemType descItemType = itemTypeRepository.findOne(descItemTypeId);
        if (descItemType == null) {
            throw new SystemException("Typ s ID=" + descItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
        }
        descItem.setItemType(descItemType);

        if (descItemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new SystemException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
            }
            descItem.setItemSpec(descItemSpec);
        }

        return descItem;
    }

    /**
     * Vytvoří DO objektu vztahu.
     *
     * @param relationVO VO objekt vztahu
     * @return DO objekt vztahu
     */
    public ParRelation createRelation(final ParRelationVO relationVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ParRelation relation = mapper.map(relationVO, ParRelation.class);
        return relation;
    }

    /**
     * Vytvoří seznam DO relation entities z VO objektů.
     *
     * @param relationEntities seznam VO relation entities
     * @return seznam DO
     */
    public List<ParRelationEntity> createRelationEntities(@Nullable final Collection<ParRelationEntityVO> relationEntities) {
        if (relationEntities == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        List<ParRelationEntity> result = new ArrayList<>(relationEntities.size());

        for (ParRelationEntityVO relationEntity : relationEntities) {
            result.add(mapper.map(relationEntity, ParRelationEntity.class));
        }

        return result;
    }

    public ArrDescItem createDescItem(final ArrItemVO descItemVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrData data = mapper.map(descItemVO, ArrData.class);
        ArrDescItem descItem = new ArrDescItem();
        descItem.setData(data);
        BeanUtils.copyProperties(descItemVO, descItem);
        descItem.setItemId(descItemVO.getId());

        if (descItemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new SystemException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
            }
            descItem.setItemSpec(descItemSpec);
        }

        return descItem;
    }

    public ArrPacket createPacket(final ArrPacketVO packetVO, final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        Assert.notNull(packetVO, "Obal musí být vyplněn");

        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrPacket packet = mapper.map(packetVO, ArrPacket.class);

        ArrFund fund = fundRepository.findOne(fundId);
        Assert.notNull(fund, "Archivní pomůcka neexistuje (ID=" + fundId + ")");

        if (packetVO.getPacketTypeId() != null) {
            RulPacketType packetType = packetTypeRepository.findOne(packetVO.getPacketTypeId());
            Assert.notNull(packetType, "Typ obalu neexistuje (ID=" + packetVO.getPacketTypeId() + ")");
            packet.setPacketType(packetType);
        }

        packet.setFund(fund);
        packet.setState(ArrPacket.State.OPEN);

        return packet;
    }

    /**
     * Vytvoří souřadnice rejstříků
     *
     * @param coordinatesVO souřadnice VO
     * @return souřadnice
     */
    public RegCoordinates createRegCoordinates(final RegCoordinatesVO coordinatesVO) {
        Assert.notNull(coordinatesVO, "Koordináty musí být vyplněny");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        RegRecord regRecord = regRecordRepository.findOne(coordinatesVO.getRegRecordId());
        Assert.notNull(regRecord, "Rejstříkové heslo neexistuje (ID=" + coordinatesVO.getRegRecordId() + ")");
        RegCoordinates coordinates = mapper.map(coordinatesVO, RegCoordinates.class);
        coordinates.setRegRecord(regRecord);
        return coordinates;
    }

    /**
     * Vytvoří třídu rejstříku.
     *
     * @param scopeVO třída rejstříku
     * @return třída rejstříku
     */
    public RegScope createScope(final RegScopeVO scopeVO) {
        Assert.notNull(scopeVO, "Scope musí být vyplněn");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(scopeVO, RegScope.class);
    }


    /**
     * Vytvoří seznam rejstříků
     *
     * @param scopeVOs seznam VO rejstříků
     * @return seznam DO
     */
    public List<RegScope> createScopeList(@Nullable final Collection<RegScopeVO> scopeVOs) {
        if (scopeVOs == null) {
            return new ArrayList<>();
        }

        List<RegScope> result = new ArrayList<>(scopeVOs.size());

        for (RegScopeVO scopeVO : scopeVOs) {
            result.add(createScope(scopeVO));
        }

        return result;
    }

    /**
     * Vytvoří DO archivní pomůcky
     *
     * @param fundVO VO archivní pomůcka
     * @return DO
     */
    public ArrFund createFund(final ArrFundVO fundVO) {
        Assert.notNull(fundVO, "AS musí být vyplněno");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFund fund = mapper.map(fundVO, ArrFund.class);
        ParInstitution institution = institutionRepository.findOne(fundVO.getInstitutionId());
        fund.setInstitution(institution);
        return fund;
    }

    public ArrNodeRegister createRegisterLink(final ArrNodeRegisterVO nodeRegisterVO) {
        Assert.notNull(nodeRegisterVO, "Rejstříkové heslo musí být vyplněno");
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeRegister nodeRegister = mapper.map(nodeRegisterVO, ArrNodeRegister.class);

        if (nodeRegisterVO.getValue() != null) {
            nodeRegister.setRecord(regRecordRepository.findOne(nodeRegisterVO.getValue()));
        }

        return nodeRegister;
    }

    public XmlImportConfig createXmlImportConfig(final XmlImportConfigVO configVO) {
        Assert.notNull(configVO, "Nastavení musí být vyplněno");

        MapperFacade mapper = mapperFactory.getMapperFacade();

        return mapper.map(configVO, XmlImportConfig.class);
    }

    public List<DescItemTypeFilter> createFilters(final Filters filters) {
        if (filters == null || filters.getFilters() == null || filters.getFilters().isEmpty()) {
            return null;
        }

        Map<Integer, Filter> filtersMap = filters.getFilters();
        Set<Integer> descItemTypeIds = filtersMap.keySet();
        List<RulItemType> descItemTypes = itemTypeRepository.findAll(descItemTypeIds);


        List<DescItemTypeFilter> descItemTypeFilters = new ArrayList<>(descItemTypes.size());;
        descItemTypes.forEach(type -> {
            Filter filter = filtersMap.get(type.getItemTypeId());
            if (filter != null) {
                DescItemTypeFilter descItemTypeFilter = createDescItemFilter(type, filter);
                if (descItemTypeFilter != null) {
                    descItemTypeFilters.add(descItemTypeFilter);
                }
            }
        });

        return descItemTypeFilters;
    }

    /**
     * Převede VO filter na filtr se kterým pracuje BL.
     *
     * @param descItemType typ atributu
     * @param filter VO filtr
     *
     * @return filtr pro daný typ atributu
     */
    private DescItemTypeFilter createDescItemFilter(final RulItemType descItemType, final Filter filter) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        Assert.notNull(filter, "Filter musí být vyplněn");

        boolean isPacketRef = descItemType.getDataType().getCode().equalsIgnoreCase("PACKET_REF");

        List<DescItemCondition> valuesConditions = createValuesEnumCondition(filter.getValuesType(), filter.getValues(),
                LuceneDescItemCondition.FULLTEXT_ATT);
        List<DescItemCondition> specsConditions = createSpecificationsEnumCondition(filter.getSpecsType(), filter.getSpecs(),
                LuceneDescItemCondition.SPECIFICATION_ATT, isPacketRef);

        List<DescItemCondition> conditions = new LinkedList<>();
        Condition conditionType = filter.getConditionType();
        if (conditionType != null && conditionType != Condition.NONE) {
            conditionType.checkSupport(descItemType.getDataType().getCode());

            DescItemCondition condition;
            switch(conditionType) {
                case BEGIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new BeginDescItemCondition<>(conditionValue, LuceneDescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case CONTAIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new ContainDescItemCondition<>(conditionValue, LuceneDescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case EMPTY: {
                    condition = new NoValuesCondition();
                    break;
                }
                case END: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new EndDescItemCondition<>(conditionValue, LuceneDescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case EQ: {
                    if (descItemType.getDataType().getCode().equals("UNITDATE")) {
                        Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                        condition = new EqIntervalDesCitemCondition<>(conditionValue,
                                ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_FROM_ATT),
                                ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_TO_ATT));
                    } else {
                        String conditionValue = getConditionValueString(filter.getCondition());
                        condition = new EqDescItemCondition<>(conditionValue, LuceneDescItemCondition.FULLTEXT_ATT);
                    }

                    break;
                }
                case GE: {
                     if (descItemType.getDataType().getCode().equals("INT")) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new GeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new GeDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case GT: {
                    if (descItemType.getDataType().getCode().equals("UNITDATE")) {
                        ArrDataUnitdate unitDate = getConditionValueUnitdate(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_FROM_ATT);
                        condition = new GtDescItemCondition<>(unitDate.getNormalizedTo(), attributeName);
                    } else if (descItemType.getDataType().getCode().equals("INT")) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new GtDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new GtDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case INTERSECT: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                    condition = new IntersectDescItemCondition<>(conditionValue,
                            ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_FROM_ATT),
                            ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_TO_ATT));
                    break;
                }
                case INTERVAL: {
                    if (descItemType.getDataType().getCode().equals("INT")) {
                        Interval<Integer> conditionValue = getConditionValueIntervalInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new IntervalDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Interval<Double> conditionValue = getConditionValueIntervalDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new IntervalDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case LE: {
                    if (descItemType.getDataType().getCode().equals("INT")) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new LeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new LeDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case LT: {
                    if (descItemType.getDataType().getCode().equals("UNITDATE")) {
                        ArrDataUnitdate unitDate = getConditionValueUnitdate(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_TO_ATT);
                        condition = new LtDescItemCondition<>(unitDate.getNormalizedFrom(), attributeName);
                    } else if (descItemType.getDataType().getCode().equals("INT")) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new LtDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new LtDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case NE: {
                    if (descItemType.getDataType().getCode().equals("INT")) {
                        Integer conditionValue = getConditionValueInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new NeDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Double conditionValue = getConditionValueDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new NeDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case NOT_CONTAIN: {
                    String conditionValue = getConditionValueString(filter.getCondition());
                    condition = new NotContainDescItemCondition<>(conditionValue, LuceneDescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case NOT_EMPTY:
                    condition = new NotEmptyDescItemCondition(); // fulltextValue
                    break;
                case UNDEFINED:
                    condition = new UndefinedDescItemCondition();
                    break;
                case NOT_INTERVAL: {
                    if (descItemType.getDataType().getCode().equals("INT")) {
                        Interval<Integer> conditionValue = getConditionValueIntervalInteger(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.INTGER_ATT);
                        condition = new NotIntervalDescItemCondition<>(conditionValue, attributeName);
                    } else {
                        Interval<Double> conditionValue = getConditionValueIntervalDouble(filter.getCondition());
                        String attributeName = ArrDescItem.concatDataAttribute(LuceneDescItemCondition.DECIMAL_ATT);
                        condition = new NotIntervalDescItemCondition<>(conditionValue, attributeName);
                    }
                    break;
                }
                case SUBSET: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getCondition());
                    condition = new SubsetDescItemCondition<>(conditionValue,
                            ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_FROM_ATT),
                            ArrDescItem.concatDataAttribute(LuceneDescItemCondition.NORMALIZED_TO_ATT));
                    break;
                }
                default:
                    throw new IllegalArgumentException("Neznámý typ podmínky " + conditionType);
            }

            conditions.add(condition);
        }

        if (!valuesConditions.isEmpty() || !specsConditions.isEmpty() || !conditions.isEmpty()) {
            return new DescItemTypeFilter(descItemType, valuesConditions, specsConditions, conditions);
        }

        return null;
    }

    private <T> T getConditionValue(final List<String> conditions, final Class<T> cls) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID).set("property", "conditions");
        }

        String value = conditions.iterator().next();

        return getConditionValue(value, cls);
    }

    private <T> Interval<T> getConditionValueInterval(final List<String> conditions, final Class<T> cls) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID).set("property", "conditions");
        }

        Iterator<String> iterator = conditions.iterator();

        String fromString = iterator.next();
        T from = getConditionValue(fromString, cls);

        String toString = iterator.next();
        if (StringUtils.isBlank(toString)) {
            throw new BusinessException("Není předána druhá hodnota intervalu.", BaseCode.PROPERTY_IS_INVALID).set("property", "toString");
        }

        T to = getConditionValue(toString, cls);

        return new Interval<T>(from, to);
    }

    @SuppressWarnings("unchecked")
    private <T> T getConditionValue(final String value, final Class<T> cls) {
        T result;
        if (Double.class.equals(cls)) {
            result = (T) Double.valueOf(value.replace(',', '.'));
        } else if (Integer.class.equals(cls)) {
            result = (T) Integer.valueOf(value);
        } else if (Long.class.equals(cls)) {
            result = (T) Long.valueOf(value);
        } else if (ArrDataUnitdate.class.equals(cls)) {
            result = (T) createUnitdate(value);
        } else { // String
            result = (T) value.toLowerCase();
        }

        return result;
    }

    /**
     * Převede textovou hodnotu na {@link ArrDataUnitdate} a doplní mezní hodnoty.
     *
     * @param value textová datace
     *
     * @return {@link ArrDataUnitdate}
     */
    private ArrDataUnitdate createUnitdate(final String value) {
        String[] split = StringUtils.split(value, '|');
        Integer calendarId = Integer.valueOf(split[0]);
        ArrCalendarType arrCalendarType = calendarTypeRepository.findOne(calendarId);
        CalendarType calendarType = CalendarType.valueOf(arrCalendarType.getCode());

        ArrDataUnitdate unitdate = new ArrDataUnitdate();
        UnitDateConvertor.convertToUnitDate(split[1], unitdate);

        String valueFrom = unitdate.getValueFrom();
        if (valueFrom == null) {
            unitdate.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            unitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, fromDate));
        }

        String valueTo = unitdate.getValueTo();
        if (valueTo == null) {
            unitdate.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            unitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, toDate));
        }

        return unitdate;
    }

    private String getConditionValueString(final List<String> conditions) {
        return getConditionValue(conditions, String.class);
    }

    private Double getConditionValueDouble(final List<String> conditions) {
        return getConditionValue(conditions, Double.class);
    }

    private Interval<Double> getConditionValueIntervalDouble(final List<String> conditions) {
        return getConditionValueInterval(conditions, Double.class);
    }

    private Integer getConditionValueInteger(final List<String> conditions) {
        return getConditionValue(conditions, Integer.class);
    }

    private Interval<Integer> getConditionValueIntervalInteger(final List<String> conditions) {
        return getConditionValueInterval(conditions, Integer.class);
    }

    private ArrDataUnitdate getConditionValueUnitdate(final List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())
                || conditions.size() < 2) {
            throw new BusinessException("Není předána hodnota podmínky.", BaseCode.PROPERTY_IS_INVALID).set("property", "conditions");
        }

        Iterator<String> iterator = conditions.iterator();

        String calendar = iterator.next();
        String date = iterator.next();
        List<String> dateConditions = new ArrayList<>(1);
        dateConditions.add(calendar + "|" + date);
        return getConditionValue(dateConditions, ArrDataUnitdate.class);
    }

    private Interval<Long> getConditionValueIntervalLong(final List<String> conditions) {
        ArrDataUnitdate unitdate = getConditionValueUnitdate(conditions);

        return new Interval<Long>(unitdate.getNormalizedFrom(), unitdate.getNormalizedTo());
    }

    /**
     * Vytvoří podmínku pro odškrtlé/zaškrtlé položky hodnot.
     *
     * @param valuesTypes typ výběru - zaškrtnutí/odškrtnutí
     * @param values hodnoty
     * @param attName název atributu na který se podmínka aplikuje
     *
     * @return seznam podmínek
     */
    private List<DescItemCondition> createValuesEnumCondition(final ValuesTypes valuesTypes, final List<String> values,
            final String attName) {
        if (valuesTypes == null && values == null) {
            return Collections.emptyList();
        }
        Assert.notNull(valuesTypes, "Typ vybraných hodnot musí být vyplněno");
        Assert.notNull(values, "Hodnoty musí být vyplněny");

        boolean noValues = CollectionUtils.isEmpty(values);
        boolean containsNull = FilterTools.removeNullValues(values);

        List<DescItemCondition> conditions = new LinkedList<>();
        if (valuesTypes == ValuesTypes.SELECTED) {
            if (noValues) { // nehledat nic
                conditions.add(new SelectsNothingCondition());
            } else if (containsNull && !values.isEmpty()) { // vybrané hodnoty i "Prázdné"
                conditions.add(new SelectedValuesDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (!values.isEmpty()) { // vybrané jen hodnoty
                conditions.add(new SelectedValuesDescItemEnumCondition(values, attName));
            } else { // vybrané jen "Prázdné"
                conditions.add(new NoValuesCondition());
            }
        } else {
            if (containsNull && !values.isEmpty()) { // odškrtlé hodnoty i "Prázdné" = hodnoty které neobsahují proškrtlé položky
                conditions.add(new UnselectedValuesDescItemEnumCondition(values, attName));
            } else if (!values.isEmpty()) { // odškrtlé jen hodnoty = hodnoty které neobsahují proškrtlé položky + nody bez hodnot
                conditions.add(new UnselectedValuesDescItemEnumCondition(values, attName));
                conditions.add(new NoValuesCondition());
            } else if (containsNull) { // odškrtlé jen "Prázdné" = vše s hodnotou
                conditions.add(new NotEmptyDescItemCondition());
            } else {
                // není potřeba vkládat podmínku, pokud vznikne ještě jiná podmínka tak by se udělal průnik výsledků a když bude seznam podmínek prázdný tak se vrátí všechna data
            }
        }

        return conditions;
    }

    /**
     * Vytvoří podmínku pro odškrtlé/zaškrtlé položky specifikací.
     *
     * @param valuesTypes typ výběru - zaškrtnutí/odškrtnutí
     * @param values id specifikací
     * @param attName název atributu na který se podmínka aplikuje
     * @param isPacketRef
     */
    private List<DescItemCondition> createSpecificationsEnumCondition(final ValuesTypes valuesTypes, final List<Integer> values,
            final String attName, final boolean isPacketRef) {
        if (valuesTypes == null && values == null) {
            return Collections.emptyList();
        }
        Assert.notNull(valuesTypes, "Typ vybraných hodnot musí být vyplněno");
        Assert.notNull(values, "Hodnoty musí být vyplněny");

        boolean noValues = CollectionUtils.isEmpty(values);
        boolean containsNull = FilterTools.removeNullValues(values);

        List<DescItemCondition> conditions = new LinkedList<>();
        if (valuesTypes == ValuesTypes.SELECTED) {
            if (noValues) { // nehledat nic
                conditions.add(new SelectsNothingCondition());
            } else if (containsNull && !values.isEmpty()) { // vybrané hodnoty i "Prázdné"
                if (isPacketRef) {
                    conditions.add(new SelectedAndEmptyPacketTypeDescItemCondition(values));
                } else {
                    conditions.add(new SelectedSpecificationsDescItemEnumCondition(values, attName));
                    conditions.add(new NoValuesCondition());
                }
            } else if (!values.isEmpty()) { // vybrané jen hodnoty
                conditions.add(new SelectedSpecificationsDescItemEnumCondition(values, attName));
            } else { // vybrané jen "Prázdné"
                if (isPacketRef) {
                    conditions.add(new EmptyPacketTypeDescItemCondition());
                } else {
                    conditions.add(new NoValuesCondition());
                }
            }
        } else {
            if (containsNull && !values.isEmpty()) { // odškrtlé hodnoty i "Prázdné" = hodnoty které neobsahují proškrtlé položky
                if (isPacketRef) {
                    conditions.add(new NotEmptyPacketTypeDescItemCondition());
                }
                conditions.add(new UnselectedSpecificationsDescItemEnumCondition(values, attName));
            } else if (!values.isEmpty()) { // odškrtlé jen hodnoty = hodnoty které neobsahují proškrtlé položky + nody bez hodnot
                if (isPacketRef) {
                    conditions.add(new UnselectedSpecificationsDescItemEnumCondition(values, attName));
                } else {
                    conditions.add(new UnselectedSpecificationsDescItemEnumCondition(values, attName));
                    conditions.add(new NoValuesCondition());
                }
            } else if (containsNull) { // odškrtlé jen "Prázdné" = vše s hodnotou
                if (isPacketRef) {
                    conditions.add(new NotEmptyPacketTypeDescItemCondition());
                } else {
                    conditions.add(new NotEmptyDescItemCondition());
                }
            } else {
                // není potřeba vkládat podmínku, pokud vznikne ještě jiná podmínka tak by se udělal průnik výsledků a když bude seznam podmínek prázdný tak se vrátí všechna data
            }
        }

        return conditions;
    }

    /**
     * Převod DMS soubor VO na DO
     *
     * @param fileVO soubor VO
     * @return soubor DO
     */
    public DmsFile createDmsFile(final DmsFileVO fileVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(fileVO, DmsFile.class);
    }

    public ArrOutputItem createOutputItem(final ArrItemVO outputItemVO, final Integer itemTypeId) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrData data = mapper.map(outputItemVO, ArrData.class);
        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setData(data);

        RulItemType descItemType = itemTypeRepository.findOne(itemTypeId);
        if (descItemType == null) {
            throw new SystemException("Typ s ID=" + outputItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
        }
        outputItem.setItemType(descItemType);

        if (outputItemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemSpecRepository.findOne(outputItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new SystemException("Specifikace s ID=" + outputItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
            }
            outputItem.setItemSpec(descItemSpec);
        }

        return outputItem;
    }

    public ArrOutputItem createOutputItem(final ArrItemVO descItemVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrData data = mapper.map(descItemVO, ArrData.class);
        ArrOutputItem outputItem = new ArrOutputItem();
        outputItem.setData(data);
        BeanUtils.copyProperties(descItemVO, outputItem);
        outputItem.setItemId(descItemVO.getId());

        if (descItemVO.getDescItemSpecId() != null) {
            RulItemSpec descItemSpec = itemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new SystemException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje", BaseCode.ID_NOT_EXIST);
            }
            outputItem.setItemSpec(descItemSpec);
        }

        return outputItem;
    }


    /**
     * Převod ArrFile soubor VO na DO
     *
     * @param fileVO soubor VO
     * @return soubor DO
     */
    public ArrFile createArrFile(final ArrFileVO fileVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(fileVO, ArrFile.class);
    }


    /**
     * Převod ArrOutputFile soubor VO na DO
     *
     * @param fileVO soubor VO
     * @return soubor DO
     */
    public ArrOutputFile createArrOutputFile(final ArrOutputFileVO fileVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(fileVO, ArrOutputFile.class);
    }

    /**
     * Převod seznamu oprávnávnění VO na DO.
     *
     * @param permissions seznam oprávnění
     * @return seznam DO
     */
    public List<UsrPermission> createPermissionList(final List<UsrPermissionVO> permissions) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(permissions, UsrPermission.class);
    }

    /**
     * Převod seznamu nastavení VO na DO.
     *
     * @param settings seznam nastavení
     * @return seznam DO
     */
    public List<UISettings> createSettingsList(final List<UISettingsVO> settings) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(settings, UISettings.class);
    }

    public <T, R> R createSimpleEntity(final T entity, final Class<R> clazz) {
        if (entity == null) {
            return null;
        }
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(entity, clazz);
    }

    public <T, R> List<R> createSimpleEntity(final Collection<T> entity, final Class<R> clazz) {
        if (entity == null) {
            return null;
        }
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.mapAsList(entity, clazz);
    }
}
