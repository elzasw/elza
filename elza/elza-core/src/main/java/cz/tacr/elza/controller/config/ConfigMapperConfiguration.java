package cz.tacr.elza.controller.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.descitems.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.factory.RuleFactory;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.DescItemSpecLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.service.RuleService;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;

import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.calendarType;
import static cz.tacr.elza.repository.ExceptionThrow.file;
import static cz.tacr.elza.repository.ExceptionThrow.partType;
import static cz.tacr.elza.repository.ExceptionThrow.refTemplate;
import static cz.tacr.elza.repository.ExceptionThrow.structureData;


/**
 * Konfigurace továrny na VO objekty.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */

@Configuration
public class ConfigMapperConfiguration {

    @Autowired
    private FundFileRepository fundFileRepository;
    @Autowired
    private StructuredObjectRepository structureDataRepository;
    @Autowired
    private CalendarTypeRepository calendarTypeRepository;
    @Autowired
    private ApAccessPointRepository apAccessPointRepository;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private PartTypeRepository partTypeRepository;
    @Autowired
    private ArrRefTemplateRepository refTemplateRepository;

    /**
     * @return Tovární třída.
     */
    @Bean(name = "configVOMapper")
    public MapperFactory configVOMapper() {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        initSimpleVO(mapperFactory);

        mapperFactory.getConverterFactory().registerConverter(new LocalDateTimeConverter());
        mapperFactory.getConverterFactory().registerConverter(new OffsetDateTimeConverter());
        mapperFactory.getConverterFactory().registerConverter(new LocalDateConverter());
        mapperFactory.getConverterFactory().registerConverter(new DescItemTypeEnumConverter());
        mapperFactory.getConverterFactory().registerConverter(new DescItemSpecEnumConverter());

        return mapperFactory;
    }

    /**
     * Vytvoří mapování jednoduchý objektů.
     *
     * @param mapperFactory tovární třída
     */
    private void initSimpleVO(final MapperFactory mapperFactory) {
        mapperFactory.classMap(ArrCalendarType.class, ArrCalendarTypeVO.class).byDefault().field(
                "calendarTypeId", "id").register();

        mapperFactory.classMap(ArrNodeConformityExt.class, NodeConformityVO.class).customize(
                new CustomMapper<ArrNodeConformityExt, NodeConformityVO>() {
                    @Override
                    public void mapAtoB(final ArrNodeConformityExt arrNodeConformityExt,
                                        final NodeConformityVO nodeConformityVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrNodeConformityExt, nodeConformityVO, context);
                        nodeConformityVO.setNodeId(arrNodeConformityExt.getNode().getNodeId());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrNodeConformityError.class, NodeConformityErrorVO.class).customize(
                new CustomMapper<ArrNodeConformityError, NodeConformityErrorVO>() {
                    @Override
                    public void mapAtoB(final ArrNodeConformityError arrNodeConformityError,
                                        final NodeConformityErrorVO nodeConformityErrorVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrNodeConformityError, nodeConformityErrorVO, context);
                        nodeConformityErrorVO
                                .setDescItemObjectId(arrNodeConformityError.getDescItem().getDescItemObjectId());
                        Integer policyTypeId = arrNodeConformityError.getPolicyType() == null ?
                                null : arrNodeConformityError.getPolicyType().getPolicyTypeId();
                        nodeConformityErrorVO.setPolicyTypeId(policyTypeId);
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrNodeConformityMissing.class, NodeConformityMissingVO.class).customize(
                new CustomMapper<ArrNodeConformityMissing, NodeConformityMissingVO>() {
                    @Override
                    public void mapAtoB(final ArrNodeConformityMissing arrNodeConformityMissing,
                                        final NodeConformityMissingVO nodeConformityMissingVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrNodeConformityMissing, nodeConformityMissingVO, context);
                        nodeConformityMissingVO.setDescItemTypeId(
                                arrNodeConformityMissing.getItemType().getItemTypeId());
                        nodeConformityMissingVO.setDescItemSpecId(
                                arrNodeConformityMissing.getItemSpec() == null ? null : arrNodeConformityMissing
                                        .getItemSpec().getItemSpecId());
                        Integer policyTypeId = arrNodeConformityMissing.getPolicyType() == null ?
                                null : arrNodeConformityMissing.getPolicyType().getPolicyTypeId();
                        nodeConformityMissingVO.setPolicyTypeId(policyTypeId);
                    }
                }).byDefault().register();


        mapperFactory.classMap(ArrItemCoordinates.class, ArrItemCoordinatesVO.class)
                .customize(new CustomMapper<ArrItemCoordinates, ArrItemCoordinatesVO>() {
                    @Override
                    public void mapAtoB(final ArrItemCoordinates coordinates,
                                        final ArrItemCoordinatesVO coordinatesVO,
                                        final MappingContext context) {
                        Geometry geo = coordinates.getValue();
                        String value = GeometryConvertor.convert(geo);
                        coordinatesVO.setValue(value);
                    }

                    @Override
                    public void mapBtoA(final ArrItemCoordinatesVO coordinatesVO,
                                        final ArrItemCoordinates coordinates,
                                        final MappingContext context) {
                        String str = coordinatesVO.getValue();
                        Geometry value = GeometryConvertor.convert(str);
                        coordinates.setValue(value);
                    }
                }).exclude("value").byDefault().register();
        mapperFactory.classMap(ArrItemEnum.class, ArrItemEnumVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemFormattedText.class, ArrItemFormattedTextVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemInt.class, ArrItemIntVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemJsonTable.class, ArrItemJsonTableVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemText.class, ArrItemTextVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemDate.class, ArrItemDateVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemDecimal.class, ArrItemDecimalVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemUnitid.class, ArrItemUnitidVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemUnitdate.class, ArrItemUnitdateVO.class).customize(
                new CustomMapper<ArrItemUnitdate, ArrItemUnitdateVO>() {
                    @Override
                    public void mapAtoB(final ArrItemUnitdate unitdate,
                                        final ArrItemUnitdateVO unitdateVO,
                                        final MappingContext context) {
                        if (unitdate.getCalendarType() != null) {
                            unitdateVO.setCalendarTypeId(unitdate.getCalendarType().getCalendarTypeId());
                            unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemUnitdateVO arrItemUnitdateVO,
                                        final ArrItemUnitdate unitdate,
                                        final MappingContext context) {
                        unitdate.setCalendarType(
                                calendarTypeRepository.findById(arrItemUnitdateVO.getCalendarTypeId())
                        .orElseThrow(calendarType(arrItemUnitdateVO.getCalendarTypeId())));
                        UnitDateConvertor.convertToUnitDate(arrItemUnitdateVO.getValue(), unitdate);
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrItemStructureRef.class, ArrItemStructureVO.class).customize(
                new CustomMapper<ArrItemStructureRef, ArrItemStructureVO>() {
                    @Override
                    public void mapAtoB(final ArrItemStructureRef itemStructureRef,
                                        final ArrItemStructureVO temStructureVO,
                                        final MappingContext context) {
                        super.mapAtoB(itemStructureRef, temStructureVO, context);
                        if (itemStructureRef.getStructuredObject() != null) {
                            temStructureVO.setValue(itemStructureRef.getStructuredObjectId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemStructureVO itemStructureVO,
                                        final ArrItemStructureRef itemStructureRef,
                                        final MappingContext context) {
                        super.mapBtoA(itemStructureVO, itemStructureRef, context);
                        itemStructureRef.setStructuredObject(itemStructureVO.getValue() == null ? null : structureDataRepository.findById(itemStructureVO.getValue())
                        .orElseThrow(structureData(itemStructureVO.getValue())));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemFileRef.class, ArrItemFileRefVO.class).customize(
                new CustomMapper<ArrItemFileRef, ArrItemFileRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemFileRef arrItemFileRef,
                                        final ArrItemFileRefVO arrItemFileRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrItemFileRef, arrItemFileRefVO, context);
                        if (arrItemFileRef.getFile() != null) {
                            arrItemFileRefVO.setValue(arrItemFileRef.getFile().getFileId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemFileRefVO arrItemFileRefVO,
                                        final ArrItemFileRef arrItemFileRef,
                                        final MappingContext context) {
                        super.mapBtoA(arrItemFileRefVO, arrItemFileRef, context);
                        arrItemFileRef.setFile(fundFileRepository.findById(arrItemFileRefVO.getValue())
                            .orElseThrow(file(arrItemFileRefVO.getValue())));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemRecordRef.class, ArrItemRecordRefVO.class).customize(
                new CustomMapper<ArrItemRecordRef, ArrItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemRecordRef recordRef,
                                        final ArrItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef == null || recordRef.getAccessPoint() == null ? null : recordRef.getAccessPoint().getAccessPointId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemRecordRefVO recordRefVO,
                                        final ArrItemRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setAccessPoint(recordRefVO.getValue() == null ? null : apAccessPointRepository.findById(recordRefVO.getValue())
                            .orElseThrow(ap(recordRefVO.getValue())));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemUriRef.class, ArrItemUriRefVO.class).customize(
                new CustomMapper<ArrItemUriRef, ArrItemUriRefVO>() {
                    @Override
                    public void mapAtoB(ArrItemUriRef arrItemUriRef, ArrItemUriRefVO arrItemUriRefVO, MappingContext context) {
                        super.mapAtoB(arrItemUriRef, arrItemUriRefVO, context);
                        arrItemUriRefVO.setValue(arrItemUriRef.getValue());
                        arrItemUriRefVO.setDescription(arrItemUriRef.getDescription());

                    }

                    @Override
                    public void mapBtoA(ArrItemUriRefVO arrItemUriRefVO, ArrItemUriRef arrItemUriRef, MappingContext context) {
                        super.mapBtoA(arrItemUriRefVO, arrItemUriRef, context);
                        arrItemUriRef.setValue(arrItemUriRefVO.getValue());
                        arrItemUriRef.setDescription(arrItemUriRefVO.getDescription());
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrItemString.class, ArrItemStringVO.class).byDefault().register();
        mapperFactory.classMap(ArrItemBit.class, ArrItemBitVO.class).byDefault().register();

        mapperFactory.classMap(ArrNode.class, ArrNodeVO.class).byDefault().field("nodeId", "id").register();

        mapperFactory.classMap(ArrChange.class, ArrChangeVO.class).byDefault().field("changeId", "id").register();
        mapperFactory.classMap(BulkActionConfig.class, BulkActionVO.class).customize(
                new CustomMapper<BulkActionConfig, BulkActionVO>() {
                    @Override
                    public void mapAtoB(final BulkActionConfig bulkActionConfig,
                                        final BulkActionVO bulkActionVO,
                                        final MappingContext context) {
                        bulkActionVO.setName(bulkActionConfig.getName());
                        bulkActionVO.setDescription(bulkActionConfig.getDescription());
                        bulkActionVO.setFastAction(bulkActionConfig.isFastAction());
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrBulkActionRun.class, BulkActionRunVO.class).field("bulkActionRunId", "id").field("bulkActionCode", "code").byDefault().register();
        mapperFactory.classMap(ParInstitution.class, ParInstitutionVO.class).byDefault()
                .field("institutionId", "id").register();
        mapperFactory.classMap(ParInstitutionType.class, ParInstitutionTypeVO.class).byDefault()
                .field("institutionTypeId", "id").register();
        mapperFactory.classMap(ArrDigitizationFrontdesk.class, ArrDigitizationFrontdeskVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitalRepository.class, ArrDigitalRepositoryVO.class).field("externalSystemId", "id").byDefault().register();

        mapperFactory.classMap(ArrDigitizationFrontdesk.class, ArrDigitizationFrontdeskSimpleVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitalRepository.class, ArrDigitalRepositorySimpleVO.class).field("externalSystemId", "id").byDefault().register();

        mapperFactory.classMap(RulDataType.class, RulDataTypeVO.class).byDefault().field("dataTypeId", "id").register();
        mapperFactory.classMap(RulItemType.class, RulDescItemTypeDescItemsVO.class).byDefault().field(
                "itemTypeId",
                "id").register();
        mapperFactory.classMap(RulItemType.class, ItemTypeDescItemsLiteVO.class).byDefault()
                .field("itemTypeId", "id")
                .register();
        mapperFactory.classMap(RulItemTypeExt.class, RulDescItemTypeExtVO.class)
                .field("itemTypeId", "id")
                .field("rulItemSpecList", "descItemSpecs")
                .field("structuredTypeId", "structureTypeId")
                .exclude("viewDefinition")
                .byDefault()
                .customize(new CustomMapper<RulItemTypeExt, RulDescItemTypeExtVO>() {
                    @Override
                    public void mapAtoB(RulItemTypeExt rulItemTypeExt, RulDescItemTypeExtVO rulDescItemTypeExtVO, MappingContext context) {
                        super.mapAtoB(rulItemTypeExt, rulDescItemTypeExtVO, context);
                        rulDescItemTypeExtVO.setViewDefinition(rulItemTypeExt.getViewDefinition());
                    }
                })
                .register();
        mapperFactory.classMap(RulItemTypeExt.class, ItemTypeLiteVO.class).byDefault()
                .field("itemTypeId", "id")
                .field("rulItemSpecList", "specs")
                .customize(new CustomMapper<RulItemTypeExt, ItemTypeLiteVO>() {
                    @Override
                    public void mapAtoB(final RulItemTypeExt rulDescItemTypeExt,
                                        final ItemTypeLiteVO itemTypeLiteVO,
                                        final MappingContext context) {
                        super.mapAtoB(rulDescItemTypeExt, itemTypeLiteVO, context);
                        itemTypeLiteVO.setRep(rulDescItemTypeExt.getRepeatable() ? 1 : 0);
                        itemTypeLiteVO.setCal(rulDescItemTypeExt.getCalculable() ? 1 : 0);
                        itemTypeLiteVO.setCalSt(rulDescItemTypeExt.getCalculableState() ? 1 : 0);
                        itemTypeLiteVO.setInd(rulDescItemTypeExt.getIndefinable() ? 1 : 0);
                    }
                })
                .register();
        mapperFactory.classMap(RulItemSpec.class, RulDescItemSpecVO.class).byDefault().field("itemSpecId", "id").register();
        mapperFactory.classMap(RulItemSpecExt.class, RulDescItemSpecExtVO.class).byDefault().field("itemSpecId",
                "id").register();
        mapperFactory.classMap(RulItemSpecExt.class, DescItemSpecLiteVO.class).byDefault()
                .field("itemSpecId", "id")
                .customize(new CustomMapper<RulItemSpecExt, DescItemSpecLiteVO>() {
                    @Override
                    public void mapAtoB(final RulItemSpecExt rulDescItemSpecExt,
                                        final DescItemSpecLiteVO descItemSpecLiteVO,
                                        final MappingContext context) {
                        super.mapAtoB(rulDescItemSpecExt, descItemSpecLiteVO, context);
                        descItemSpecLiteVO.setRep(rulDescItemSpecExt.getRepeatable() ? 1 : 0);
                    }
                })
                .register();

        mapperFactory.classMap(RulOutputType.class, RulOutputTypeVO.class).byDefault().field("outputTypeId", "id").register();

        mapperFactory.classMap(RulRuleSet.class, RulRuleSetVO.class)
                .byDefault()
                .field("ruleSetId", "id")
                .customize(new CustomMapper<RulRuleSet, RulRuleSetVO>() {
                    @Override
                    public void mapAtoB(final RulRuleSet rulRuleSet, final RulRuleSetVO rulRuleSetVO, final MappingContext context) {
                        super.mapAtoB(rulRuleSet, rulRuleSetVO, context);
                        List<SettingGridView.ItemType> itemTypes = ruleService.getGridView();
                        if (itemTypes != null) {
                            List<RulRuleSetVO.GridView> gridViews = new ArrayList<>(itemTypes.size());
                            for (SettingGridView.ItemType itemType : itemTypes) {
                                RulRuleSetVO.GridView gridView = new RulRuleSetVO.GridView();
                                gridView.setCode(itemType.getCode());
                                gridView.setShowDefault(itemType.getShowDefault());
                                gridView.setWidth(itemType.getWidth());
                                gridViews.add(gridView);
                            }
                            rulRuleSetVO.setGridViews(gridViews);
                        }
                    }
                })
                .register();

        mapperFactory.classMap(RulPolicyType.class, RulPolicyTypeVO.class).byDefault().field("policyTypeId", "id").register();
        mapperFactory.classMap(RulArrangementExtension.class, RulArrangementExtensionVO.class).byDefault().field("arrangementExtensionId", "id").register();

        mapperFactory.classMap(ScenarioOfNewLevel.class, ScenarioOfNewLevelVO.class).byDefault().register();

        mapperFactory.classMap(ArrFund.class, ArrFundVO.class).byDefault().field("fundId", "id").register();
        mapperFactory.classMap(ArrFund.class, ArrFundBaseVO.class).byDefault().field("fundId", "id").register();
        mapperFactory.classMap(ArrFund.class, Fund.class).byDefault().field("fundId", "id").register();
        mapperFactory.classMap(ArrFund.class, FundDetail.class).byDefault().field("fundId", "id").register();

        mapperFactory.classMap(ArrFundVersion.class, ArrFundVersionVO.class).byDefault().field(
                "fundVersionId", "id").exclude("arrangementType")
                .customize(new CustomMapper<ArrFundVersion, ArrFundVersionVO>() {
                    @Override
                    public void mapAtoB(final ArrFundVersion arrFundVersion, final ArrFundVersionVO arrFundVersionVO, final MappingContext context) {
                        super.mapAtoB(arrFundVersion, arrFundVersionVO, context);
                        arrFundVersionVO.setPackageId(arrFundVersion.getRuleSet().getPackage().getPackageId());
                    }
                })
                .register();
        mapperFactory.getConverterFactory().registerConverter(new PassThroughConverter(LocalDateTime.class));

        mapperFactory.classMap(UsrUser.class, UsrUserVO.class)
                .byDefault()
                .field("userId", "id")
                .register();
        mapperFactory.classMap(UsrGroup.class, UsrGroupVO.class)
                .byDefault()
                .field("groupId", "id")
                .register();

        mapperFactory.classMap(RulTemplate.class, RulTemplateVO.class)
                .byDefault()
                .field("templateId", "id")
                .register();

        mapperFactory.classMap(UISettings.class, UISettingsVO.class)
                .byDefault()
                .field("settingsId", "id")
                .register();

        mapperFactory.classMap(RulStructuredType.class, RulStructureTypeVO.class)
                .byDefault()
                .field("structuredTypeId", "id")
                .register();

        mapperFactory.classMap(RulPartType.class, RulPartTypeVO.class)
                .byDefault()
                .customize(new CustomMapper<RulPartType, RulPartTypeVO>() {
                    @Override
                    public void mapAtoB(final RulPartType rulPartType,
                                        final RulPartTypeVO rulPartTypeVO,
                                        final MappingContext context) {
                        rulPartTypeVO.setChildPartId(rulPartType.getChildPart() != null ? rulPartType.getChildPart().getPartTypeId() : null);
                    }

                    @Override
                    public void mapBtoA(final RulPartTypeVO rulPartTypeVO,
                                        final RulPartType rulPartType,
                                        final MappingContext context) {
                        rulPartType.setChildPart(rulPartTypeVO.getChildPartId() != null ? partTypeRepository.findById(rulPartTypeVO.getChildPartId())
                                .orElseThrow(partType(rulPartTypeVO.getChildPartId())): null);
                    }
                })
                .field("partTypeId", "id")
                .register();

        mapperFactory.classMap(ArrDataCoordinates.class, ArrItemCoordinatesVO.class)
                .customize(new CustomMapper<ArrDataCoordinates, ArrItemCoordinatesVO>() {
                    @Override
                    public void mapAtoB(final ArrDataCoordinates coordinates,
                                        final ArrItemCoordinatesVO coordinatesVO,
                                        final MappingContext context) {
                        Geometry geo = coordinates.getValue();
                        String value = GeometryConvertor.convert(geo);
                        coordinatesVO.setValue(value);
                    }

                    @Override
                    public void mapBtoA(final ArrItemCoordinatesVO coordinatesVO,
                                        final ArrDataCoordinates coordinates,
                                        final MappingContext context) {
                        String str = coordinatesVO.getValue();
                        Geometry value = GeometryConvertor.convert(str);
                        coordinates.setValue(value);
                    }
                }).exclude("value").byDefault().register();
        mapperFactory.classMap(ArrDataNull.class, ArrItemEnumVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataInteger.class, ArrItemIntVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataDate.class, ArrItemDateVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataJsonTable.class, ArrItemJsonTableVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataDecimal.class, ArrItemDecimalVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataBit.class, ArrItemBitVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataUnitid.class, ArrItemUnitidVO.class).customize(
                new CustomMapper<ArrDataUnitid, ArrItemUnitidVO>() {
                    @Override
                    public void mapAtoB(final ArrDataUnitid unitId,
                                        final ArrItemUnitidVO unitIdVo,
                                        final MappingContext context) {
                        unitIdVo.setValue(unitId
                                .getUnitId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemUnitidVO unitIdVo,
                                        final ArrDataUnitid unitId,
                                        final MappingContext context) {
                        unitId.setUnitId(unitIdVo
                                .getValue());
                    }

                })
                .byDefault().register();
        mapperFactory.classMap(ArrDataUnitdate.class, ArrItemUnitdateVO.class).customize(
                new CustomMapper<ArrDataUnitdate, ArrItemUnitdateVO>() {
                    @Override
                    public void mapAtoB(final ArrDataUnitdate unitdate,
                                        final ArrItemUnitdateVO unitdateVO,
                                        final MappingContext context) {
                        if (unitdate.getCalendarType() != null) {
                            unitdateVO.setCalendarTypeId(unitdate.getCalendarType().getCalendarTypeId());
                            unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemUnitdateVO arrItemUnitdateVO,
                                        final ArrDataUnitdate unitdate,
                                        final MappingContext context) {
                        unitdate.setCalendarType(
                                calendarTypeRepository.findById(arrItemUnitdateVO.getCalendarTypeId())
                                    .orElseThrow(calendarType(arrItemUnitdateVO.getCalendarTypeId())));
                        UnitDateConvertor.convertToUnitDate(arrItemUnitdateVO.getValue(), unitdate);


                        String codeCalendar = unitdate.getCalendarType().getCode();
                        CalendarType calendarType = CalendarType.valueOf(codeCalendar);

                        String value;

                        value = unitdate.getValueFrom();
                        if (value != null) {
                            unitdate.setNormalizedFrom(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            unitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }

                        // I like mappers, this code is nice joke :-)
                        unitdate.setNormalizedFrom(unitdate.getNormalizedFrom());

                        value = unitdate.getValueTo();
                        if (value != null) {
                            unitdate.setNormalizedTo(CalendarConverter.toSeconds(calendarType, LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            unitdate.setNormalizedTo(Long.MAX_VALUE);
                        }

                        // Look, the same joke twice !
                        unitdate.setNormalizedTo(unitdate.getNormalizedTo());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrDataStructureRef.class, ArrItemStructureVO.class).customize(
                new CustomMapper<ArrDataStructureRef, ArrItemStructureVO>() {
                    @Override
                    public void mapAtoB(final ArrDataStructureRef itemStructureDataRef,
                                        final ArrItemStructureVO itemStructureVO,
                                        final MappingContext context) {
                        super.mapAtoB(itemStructureDataRef, itemStructureVO, context);
                        itemStructureVO.setStructureData(ArrStructureDataVO.newInstance(itemStructureDataRef.getStructuredObject()));
                        if (itemStructureDataRef.getStructuredObject() != null) {
                            itemStructureVO.setValue(itemStructureDataRef.getStructuredObjectId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemStructureVO itemStructureVO,
                                        final ArrDataStructureRef itemStructureRef,
                                        final MappingContext context) {
                        super.mapBtoA(itemStructureVO, itemStructureRef, context);
                        itemStructureRef.setStructuredObject(itemStructureVO.getValue() == null ? null : structureDataRepository.findById(itemStructureVO.getValue())
                        .orElseThrow(structureData(itemStructureVO.getValue())));
                    }
                })
                .byDefault().register();
        mapperFactory.classMap(ArrDataFileRef.class, ArrItemFileRefVO.class).customize(
                new CustomMapper<ArrDataFileRef, ArrItemFileRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataFileRef arrItemFileRef,
                                        final ArrItemFileRefVO arrItemFileRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrItemFileRef, arrItemFileRefVO, context);
                        if (arrItemFileRef.getFile() != null) {
                            arrItemFileRefVO.setValue(arrItemFileRef.getFile().getFileId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemFileRefVO arrItemFileRefVO,
                                        final ArrDataFileRef arrItemFileRef,
                                        final MappingContext context) {
                        super.mapBtoA(arrItemFileRefVO, arrItemFileRef, context);
                        arrItemFileRef.setFile(fundFileRepository.findById(arrItemFileRefVO.getValue())
                            .orElseThrow(file(arrItemFileRefVO.getValue())));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrDataRecordRef.class, ArrItemRecordRefVO.class).customize(
                new CustomMapper<ArrDataRecordRef, ArrItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataRecordRef recordRef,
                                        final ArrItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef == null || recordRef.getRecord() == null ? null : recordRef.getRecord().getAccessPointId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemRecordRefVO recordRefVO,
                                        final ArrDataRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setRecord(recordRefVO.getValue() == null ? null : apAccessPointRepository.findById(recordRefVO.getValue())
                        .orElseThrow(ap(recordRefVO.getValue())));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrDataString.class, ArrItemStringVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataUriRef.class, ArrItemUriRefVO.class).customize(
                new CustomMapper<ArrDataUriRef, ArrItemUriRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataUriRef uriRef,
                                        final ArrItemUriRefVO uriRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(uriRef, uriRefVO, context);
                        uriRefVO.setValue(uriRef.getValue());
                        uriRefVO.setDescription(uriRef.getDescription());
                        uriRefVO.setRefTemplateId(uriRef.getRefTemplate() != null ? uriRef.getRefTemplate().getRefTemplateId() : null);
                        uriRefVO.setNodeId(uriRef.getNodeId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemUriRefVO uriRefVO,
                                        final ArrDataUriRef uriRef,
                                        final MappingContext context) {
                        super.mapBtoA(uriRefVO, uriRef, context);
                        uriRef.setValue(uriRefVO.getValue());
                        uriRef.setDescription(uriRefVO.getDescription());
                        uriRef.setRefTemplate(uriRefVO.getRefTemplateId() != null ? refTemplateRepository.findById(uriRefVO.getRefTemplateId())
                                .orElseThrow(refTemplate(uriRefVO.getRefTemplateId())): null);
                    }
                }).byDefault().register();
        /*mapperFactory.classMap(ArrDataText.class, ArrItemAbstractTextVO.class).customize(new CustomMapper<ArrDataText, ArrItemAbstractTextVO>() {
            @Override
            public void mapAtoB(final ArrDataText arrDataText, ArrItemAbstractTextVO arrItemAbstractTextVO, final MappingContext context) {
                switch (arrDataText.getDataType().getCode()) {
                    case "TEXT":
                        arrItemAbstractTextVO = new ArrItemTextVO();
                        ((ArrItemTextVO) arrItemAbstractTextVO).setValue(arrDataText.getValue());
                        break;
                    case "FORMATTED_TEXT":
                        arrItemAbstractTextVO = new ArrItemFormattedTextVO();
                        ((ArrItemFormattedTextVO) arrItemAbstractTextVO).setValue(arrDataText.getValue());
                        break;
                    default:
                        throw new NotImplementedException();
                }
            }

            @Override
            public void mapBtoA(final ArrItemAbstractTextVO arrItemAbstractTextVO, final ArrDataText arrDataText, final MappingContext context) {
                if (arrItemAbstractTextVO instanceof ArrItemTextVO) {
                    arrDataText.setValue(((ArrItemTextVO) arrItemAbstractTextVO).getValue());
                } else if (arrItemAbstractTextVO instanceof ArrItemFormattedTextVO) {
                    arrDataText.setValue(((ArrItemFormattedTextVO) arrItemAbstractTextVO).getValue());
                } else {
                    throw new NotImplementedException();
                }
            }
        }).register();*/

        mapperFactory.classMap(ArrDataText.class, ArrItemTextVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataText.class, ArrItemFormattedTextVO.class).byDefault().register();

        mapperFactory.classMap(PersistentSortRunConfig.class, PersistentSortConfigVO.class).
                byDefault().
                exclude("nodeIds").
                register();
    }

    /**
     * Konvertor mezi LocalDateTime a Date.
     */
    public class LocalDateTimeConverter extends BidirectionalConverter<LocalDateTime, Date> {

        @Override
        public Date convertTo(final LocalDateTime localDateTime, final Type<Date> type) {
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }

        @Override
        public LocalDateTime convertFrom(final Date date, final Type<LocalDateTime> type) {
            return LocalDateTime.from(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }
    }

    /**
     * Konvertor mezi OffsetDateTime a Date.
     */
    public class OffsetDateTimeConverter extends BidirectionalConverter<OffsetDateTime, Date> {

        @Override
        public Date convertTo(final OffsetDateTime localDateTime, final Type<Date> type) {
            return Date.from(localDateTime.toInstant());
        }

        @Override
        public OffsetDateTime convertFrom(final Date date, final Type<OffsetDateTime> type) {
            return OffsetDateTime.from(date.toInstant());
        }
    }

    public class LocalDateConverter extends BidirectionalConverter<LocalDate, LocalDate> {

        @Override
        public LocalDate convertTo(LocalDate source, Type<LocalDate> destinationType) {
            return (LocalDate.from(source));
        }

        @Override
        public LocalDate convertFrom(LocalDate source, Type<LocalDate> destinationType) {
            return (LocalDate.from(source));
        }

    }

    public class DescItemTypeEnumConverter extends BidirectionalConverter<RulItemType.Type, Integer> {


        @Override
        public Integer convertTo(final RulItemType.Type type,
                                 final Type<Integer> type2) {
            return RuleFactory.convertType(type);
        }

        @Override
        public RulItemType.Type convertFrom(final Integer type,
                                            final Type<RulItemType.Type> type2) {
            switch (type) {
                case 3:
                    return RulItemType.Type.REQUIRED;
                case 2:
                    return RulItemType.Type.RECOMMENDED;
                case 1:
                    return RulItemType.Type.POSSIBLE;
                case 0:
                    return RulItemType.Type.IMPOSSIBLE;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
        }

    }

    public class DescItemSpecEnumConverter extends BidirectionalConverter<RulItemSpec.Type, Integer> {


        @Override
        public Integer convertTo(final RulItemSpec.Type type,
                                 final Type<Integer> type2) {
            return RuleFactory.convertType(type);
        }

        @Override
        public RulItemSpec.Type convertFrom(final Integer type,
                                            final Type<RulItemSpec.Type> type2) {
            switch (type) {
                case 3:
                    return RulItemSpec.Type.REQUIRED;
                case 2:
                    return RulItemSpec.Type.RECOMMENDED;
                case 1:
                    return RulItemSpec.Type.POSSIBLE;
                case 0:
                    return RulItemSpec.Type.IMPOSSIBLE;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
        }

    }

}
