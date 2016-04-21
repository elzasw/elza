package cz.tacr.elza.controller.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.XmlImportConfigVO;
import cz.tacr.elza.controller.vo.filter.Condition;
import cz.tacr.elza.controller.vo.filter.Filter;
import cz.tacr.elza.controller.vo.filter.Filters;
import cz.tacr.elza.controller.vo.filter.ValuesTypes;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.filter.condition.BeginDescItemCondition;
import cz.tacr.elza.filter.condition.ContainDescItemCondition;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.filter.condition.EmptyDescItemCondition;
import cz.tacr.elza.filter.condition.EndDescItemCondition;
import cz.tacr.elza.filter.condition.EqDescItemCondition;
import cz.tacr.elza.filter.condition.GeDescItemCondition;
import cz.tacr.elza.filter.condition.GtDescItemCondition;
import cz.tacr.elza.filter.condition.IntersectDescItemCondition;
import cz.tacr.elza.filter.condition.Interval;
import cz.tacr.elza.filter.condition.IntervalDescItemCondition;
import cz.tacr.elza.filter.condition.LeDescItemCondition;
import cz.tacr.elza.filter.condition.LtDescItemCondition;
import cz.tacr.elza.filter.condition.NeDescItemCondition;
import cz.tacr.elza.filter.condition.NotContainDescItemCondition;
import cz.tacr.elza.filter.condition.NotEmptyDescItemCondition;
import cz.tacr.elza.filter.condition.NotIntervalDescItemCondition;
import cz.tacr.elza.filter.condition.SelectedValuesDescItemEnumCondition;
import cz.tacr.elza.filter.condition.SubsetDescItemCondition;
import cz.tacr.elza.filter.condition.UnselectedValuesDescItemEnumCondition;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;


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
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private PacketTypeRepository packetTypeRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    /**
     * Vytvoří node z VO.
     * @param nodeVO vo node
     * @return DO node
     */
    public ArrNode createNode(final ArrNodeVO nodeVO){
        Assert.notNull(nodeVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(nodeVO, ArrNode.class);
    }

    /**
     * Vytvoří seznam DO z VO.
     * @param nodeVoList VO seznam nodů
     * @return DO seznam nodů
     */
    public List<ArrNode> createNodes(final Collection<ArrNodeVO> nodeVoList){
        Assert.notNull(nodeVoList);

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
    public ArrDescItem createDescItem(final ArrDescItemVO descItemVO, final Integer descItemTypeId) {
        MapperFacade mapper = mapperFactory.getMapperFacade();

        ArrDescItem descItem = mapper.map(descItemVO, ArrDescItem.class);

        RulDescItemType descItemType = descItemTypeRepository.findOne(descItemTypeId);
        if (descItemType == null) {
            throw new IllegalStateException("Typ s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
        }
        descItem.setDescItemType(descItemType);

        if (descItemVO.getDescItemSpecId() != null) {
            RulDescItemSpec descItemSpec = descItemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new IllegalStateException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
            }
            descItem.setDescItemSpec(descItemSpec);
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

    public ArrDescItem createDescItem(final ArrDescItemVO descItemVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrDescItem descItem = mapper.map(descItemVO, ArrDescItem.class);

        if (descItemVO.getDescItemSpecId() != null) {
            RulDescItemSpec descItemSpec = descItemSpecRepository.findOne(descItemVO.getDescItemSpecId());
            if (descItemSpec == null) {
                throw new IllegalStateException("Specifikace s ID=" + descItemVO.getDescItemSpecId() + " neexistuje");
            }
            descItem.setDescItemSpec(descItemSpec);
        }

        return descItem;
    }

    public ArrPacket createPacket(final ArrPacketVO packetVO, final Integer fundId) {
        Assert.notNull(fundId);
        Assert.notNull(packetVO);

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
     * Vytvoří třídu rejstříku.
     *
     * @param scopeVO třída rejstříku
     * @return třída rejstříku
     */
    public RegScope createScope(final RegScopeVO scopeVO) {
        Assert.notNull(scopeVO);
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
    public ArrFund createFund(ArrFundVO fundVO) {
        Assert.notNull(fundVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrFund fund = mapper.map(fundVO, ArrFund.class);
        ParInstitution institution = institutionRepository.findOne(fundVO.getInstitutionId());
        fund.setInstitution(institution);
        return fund;
    }

    public ArrNodeRegister createRegisterLink(final ArrNodeRegisterVO nodeRegisterVO) {
        Assert.notNull(nodeRegisterVO);
        MapperFacade mapper = mapperFactory.getMapperFacade();
        ArrNodeRegister nodeRegister = mapper.map(nodeRegisterVO, ArrNodeRegister.class);

        if (nodeRegisterVO.getValue() != null) {
            nodeRegister.setRecord(regRecordRepository.findOne(nodeRegisterVO.getValue()));
        }

        return nodeRegister;
    }

    public XmlImportConfig createXmlImportConfig(final XmlImportConfigVO configVO) {
        Assert.notNull(configVO);

        MapperFacade mapper = mapperFactory.getMapperFacade();

        return mapper.map(configVO, XmlImportConfig.class);
    }

    public List<DescItemTypeFilter> createFilters(final Filters filters) {
        if (filters == null || filters.getFilters() == null || filters.getFilters().isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, Filter> filtersMap = filters.getFilters();
        Set<Integer> descItemTypeIds = filtersMap.keySet();
        List<RulDescItemType> descItemTypes = descItemTypeRepository.findAll(descItemTypeIds);

        List<DescItemTypeFilter> result = new ArrayList<>(descItemTypes.size());
        descItemTypes.forEach(type -> {
            Filter filter = filtersMap.get(type.getDescItemTypeId());
            if (filter != null) {
                DescItemTypeFilter descItemFilter = createDescItemFilter(type, filter);
                if (descItemFilter != null) {
                    result.add(descItemFilter);
                }
            }
        });

        return result;
    }

    /**
     * Převede VO filter na filtr se kterým pracuje BL.
     *
     * @param descItemType typ atributu
     * @param filter VO filtr
     *
     * @return filtr pto BL
     */
    private DescItemTypeFilter createDescItemFilter(RulDescItemType descItemType, final Filter filter) {
        Assert.notNull(descItemType);
        Assert.notNull(filter);

        List<DescItemCondition> conditions = new LinkedList<DescItemCondition>();
        createValuesEnumCondition(filter.getValuesTypes(), filter.getValues(), DescItemCondition.FULLTEXT_ATT, conditions);
        createValuesEnumCondition(filter.getSpecsTypes(), filter.getSpecs(), DescItemCondition.SPECIFICATION_ATT, conditions);

        Condition conditionType = filter.getConditionType();
        if (conditionType != null && conditionType != Condition.NONE) {
            conditionType.checkSupport(descItemType.getDataType().getCode());

            DescItemCondition condition;
            switch(conditionType) {
                case BEGIN: {
                    String conditionValue = getConditionValue(filter.getConditions());
                    condition = new BeginDescItemCondition<String>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case CONTAIN: {
                    String conditionValue = getConditionValue(filter.getConditions());
                    condition = new ContainDescItemCondition<String>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case EMPTY: {
                    condition = new EmptyDescItemCondition(); // fulltextValue
                    break;
                }
                case END: {
                    String conditionValue = getConditionValue(filter.getConditions());
                    condition = new EndDescItemCondition<String>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case EQ: {
                    String conditionValue = getConditionValue(filter.getConditions());
                    condition = new EqDescItemCondition<String>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case GE: {
                    BigDecimal conditionValue = getConditionValueBigDecimal(filter.getConditions());
                    condition = new GeDescItemCondition<BigDecimal>(conditionValue, DescItemCondition.NUMERIC_ATT);
                    break;
                }
                case GT: {
                    BigDecimal conditionValue = getConditionValueBigDecimal(filter.getConditions());
                    String attributeName;
                    if (descItemType.getCode().equals("UNIDATE")) {
                        attributeName = DescItemCondition.NORMALIZED_TO_ATT;
                    } else {
                        attributeName = DescItemCondition.NUMERIC_ATT;
                    }
                    condition = new GtDescItemCondition<BigDecimal>(conditionValue, attributeName);
                    break;
                }
                case INTERSECT: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getConditions());
                    condition = new IntersectDescItemCondition<Interval<Long>, Long>(conditionValue,
                            DescItemCondition.NORMALIZED_FROM_ATT,
                            DescItemCondition.NORMALIZED_TO_ATT);
                    break;
                }
                case INTERVAL: {
                    Interval<BigDecimal> conditionValue = getConditionValueIntervalBigDecimal(filter.getConditions());
                    condition = new IntervalDescItemCondition<Interval<BigDecimal>, BigDecimal>(conditionValue,
                            DescItemCondition.NUMERIC_ATT);
                    break;
                }
                case LE: {
                    BigDecimal conditionValue = getConditionValueBigDecimal(filter.getConditions());
                    condition = new LeDescItemCondition<BigDecimal>(conditionValue, DescItemCondition.NUMERIC_ATT);
                    break;
                }
                case LT: {
                    BigDecimal conditionValue = getConditionValueBigDecimal(filter.getConditions());
                    String attributeName;
                    if (descItemType.getCode().equals("UNIDATE")) {
                        attributeName = DescItemCondition.NORMALIZED_FROM_ATT;
                    } else {
                        attributeName = DescItemCondition.NUMERIC_ATT;
                    }
                    condition = new LtDescItemCondition<BigDecimal>(conditionValue, attributeName);
                    break;
                }
                case NE: {
                    BigDecimal conditionValue = getConditionValueBigDecimal(filter.getConditions());
                    condition = new NeDescItemCondition<BigDecimal>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case NOT_CONTAIN: {
                    String conditionValue = getConditionValue(filter.getConditions());
                    condition = new NotContainDescItemCondition<String>(conditionValue, DescItemCondition.FULLTEXT_ATT);
                    break;
                }
                case NOT_EMPTY:
                    condition = new NotEmptyDescItemCondition(); // fulltextValue
                    break;
                case NOT_INTERVAL: {
                    Interval<BigDecimal> conditionValue = getConditionValueIntervalBigDecimal(filter.getConditions());
                    condition = new NotIntervalDescItemCondition<Interval<BigDecimal>, BigDecimal>(conditionValue,
                            DescItemCondition.NUMERIC_ATT);
                    break;
                }
                case SUBSET: {
                    Interval<Long> conditionValue = getConditionValueIntervalLong(filter.getConditions());
                    condition = new SubsetDescItemCondition<Interval<Long>, Long>(conditionValue,
                            DescItemCondition.NORMALIZED_FROM_ATT,
                            DescItemCondition.NORMALIZED_TO_ATT);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Neznámý typ podmínky " + conditionType);
            }

            conditions.add(condition);
        }

        if (!conditions.isEmpty()) {
            Class<? extends ArrData> typeClass = descriptionItemService.getDescItemDataTypeClass(descItemType);
            return new DescItemTypeFilter(descItemType, typeClass, conditions);
        }

        return null;
    }

    private String getConditionValue(List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new IllegalArgumentException("Není předána hodnota podmínky.");
        }

        return conditions.iterator().next().toLowerCase();
    }

    private BigDecimal getConditionValueBigDecimal(List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new IllegalArgumentException("Není předána hodnota podmínky.");
        }

        return BigDecimal.valueOf(Double.valueOf(conditions.iterator().next()));
    }

    private Interval<BigDecimal> getConditionValueIntervalBigDecimal(List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new IllegalArgumentException("Není předána hodnota podmínky.");
        }

        Iterator<String> iterator = conditions.iterator();

        BigDecimal from = BigDecimal.valueOf(Double.valueOf(iterator.next()));

        String toString = iterator.next();
        if (StringUtils.isBlank(toString)) {
            throw new IllegalArgumentException("Není předána druhá hodnota intervalu.");
        }

        BigDecimal to = BigDecimal.valueOf(Double.valueOf(toString));

        return new Interval<BigDecimal>(from, to);
    }

    private Interval<Long> getConditionValueIntervalLong(List<String> conditions) {
        if (CollectionUtils.isEmpty(conditions) || StringUtils.isBlank(conditions.iterator().next())) {
            throw new IllegalArgumentException("Není předána hodnota podmínky.");
        }

        Iterator<String> iterator = conditions.iterator();

        Long from = Long.valueOf(iterator.next());

        String toString = iterator.next();
        if (StringUtils.isBlank(toString)) {
            throw new IllegalArgumentException("Není předána druhá hodnota intervalu.");
        }

        Long to = Long.valueOf(toString);

        return new Interval<Long>(from, to);
    }

    private void createValuesEnumCondition(ValuesTypes valuesTypes, List<String> values, String attName, List<DescItemCondition> conditions) {
        if (valuesTypes != null && CollectionUtils.isNotEmpty(values)) {
            if (valuesTypes == ValuesTypes.SELECTED) {
                conditions.add(new SelectedValuesDescItemEnumCondition(values, attName));
            } else {
                conditions.add(new UnselectedValuesDescItemEnumCondition(values, attName));
            }
        }
    }
}
