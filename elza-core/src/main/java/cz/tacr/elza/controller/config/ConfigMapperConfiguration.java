package cz.tacr.elza.controller.config;

import static cz.tacr.elza.repository.ExceptionThrow.ap;
import static cz.tacr.elza.repository.ExceptionThrow.file;
import static cz.tacr.elza.repository.ExceptionThrow.partType;
import static cz.tacr.elza.repository.ExceptionThrow.refTemplate;
import static cz.tacr.elza.repository.ExceptionThrow.structureData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.factory.RuleFactory;
import cz.tacr.elza.controller.vo.ArrChangeVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositorySimpleVO;
import cz.tacr.elza.controller.vo.ArrDigitalRepositoryVO;
import cz.tacr.elza.controller.vo.ArrDigitizationFrontdeskSimpleVO;
import cz.tacr.elza.controller.vo.ArrDigitizationFrontdeskVO;
import cz.tacr.elza.controller.vo.ArrFundBaseVO;
import cz.tacr.elza.controller.vo.ArrFundVO;
import cz.tacr.elza.controller.vo.ArrFundVersionVO;
import cz.tacr.elza.controller.vo.ArrStructureDataVO;
import cz.tacr.elza.controller.vo.BulkActionRunVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.NodeConformityErrorVO;
import cz.tacr.elza.controller.vo.NodeConformityMissingVO;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.ParInstitutionTypeVO;
import cz.tacr.elza.controller.vo.ParInstitutionVO;
import cz.tacr.elza.controller.vo.PersistentSortConfigVO;
import cz.tacr.elza.controller.vo.RulArrangementExtensionVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulExportFilterVO;
import cz.tacr.elza.controller.vo.RulOutputFilterVO;
import cz.tacr.elza.controller.vo.RulOutputTypeVO;
import cz.tacr.elza.controller.vo.RulPartTypeVO;
import cz.tacr.elza.controller.vo.RulPolicyTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.RulStructureTypeVO;
import cz.tacr.elza.controller.vo.RulTemplateVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.UsrGroupVO;
import cz.tacr.elza.controller.vo.UsrUserVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.DescItemSpecLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemBitVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFileRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUriRefVO;
import cz.tacr.elza.core.data.StringNormalize;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemBit;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemDate;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemStructureRef;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.ArrItemUriRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulExportFilter;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecExt;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulOutputFilter;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ArrRefTemplateRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.SettingsService;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;

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
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private PartTypeRepository partTypeRepository;

    @Autowired
    private ArrRefTemplateRepository refTemplateRepository;
    
    @Autowired
    private SettingsService settingsService;    

    @Autowired
    private ItemService itemService;

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
                        unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
                    }

                    @Override
                    public void mapBtoA(final ArrItemUnitdateVO arrItemUnitdateVO,
                                        final ArrItemUnitdate unitdate,
                                        final MappingContext context) {
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
                        List<SettingGridView.ItemType> itemTypes = settingsService.getGridView();
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
                        str = itemService.normalizeCoordinates(str);
                        Geometry value = GeometryConvertor.convert(str);
                        coordinates.setValue(value);
                    }
                }).exclude("value").byDefault().register();
        mapperFactory.classMap(ArrDataNull.class, ArrItemEnumVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataInteger.class, ArrItemIntVO.class).customize(
                new CustomMapper<ArrDataInteger, ArrItemIntVO>() {
                    @Override
                    public void mapAtoB(final ArrDataInteger integer,
                                        final ArrItemIntVO intVO,
                                        final MappingContext context) {
                        super.mapAtoB(integer, intVO, context);
                        intVO.setValue(integer.getIntegerValue());
                    }

                    @Override
                    public void mapBtoA(final ArrItemIntVO intVO,
                                        final ArrDataInteger integer,
                                        final MappingContext context) {
                        super.mapBtoA(intVO, integer, context);
                        integer.setIntegerValue(intVO.getValue());
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrDataDate.class, ArrItemDateVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataJsonTable.class, ArrItemJsonTableVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataDecimal.class, ArrItemDecimalVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataBit.class, ArrItemBitVO.class).customize(
                new CustomMapper<ArrDataBit, ArrItemBitVO>() {
                    @Override
                    public void mapAtoB(final ArrDataBit bit,
                                        final ArrItemBitVO bitVO,
                                        final MappingContext context) {
                        super.mapAtoB(bit, bitVO, context);
                        bitVO.setValue(bit.isBitValue());
                    }

                    @Override
                    public void mapBtoA(final ArrItemBitVO bitVO,
                                        final ArrDataBit bit,
                                        final MappingContext context) {
                        super.mapBtoA(bitVO, bit, context);
                        bit.setBitValue(bitVO.isValue());
                    }
                }
        ).byDefault().register();
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
                        unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
                    }

                    @Override
                    public void mapBtoA(final ArrItemUnitdateVO arrItemUnitdateVO,
                                        final ArrDataUnitdate unitdate,
                                        final MappingContext context) {
                        UnitDateConvertor.convertToUnitDate(arrItemUnitdateVO.getValue(), unitdate);

                        String value = unitdate.getValueFrom();
                        if (value != null) {
                            unitdate.setNormalizedFrom(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                        } else {
                            unitdate.setNormalizedFrom(Long.MIN_VALUE);
                        }

                        // I like mappers, this code is nice joke :-)
                        unitdate.setNormalizedFrom(unitdate.getNormalizedFrom());

                        value = unitdate.getValueTo();
                        if (value != null) {
                            unitdate.setNormalizedTo(CalendarConverter.toSeconds(LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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
        mapperFactory.classMap(ArrDataString.class, ArrItemStringVO.class).customize(
                new CustomMapper<ArrDataString, ArrItemStringVO>() {
                    @Override
                    public void mapAtoB(final ArrDataString string,
                                        final ArrItemStringVO stringVO,
                                        final MappingContext context) {
                        super.mapAtoB(string, stringVO, context);
                        stringVO.setValue(string.getStringValue());
                    }

                    @Override
                    public void mapBtoA(final ArrItemStringVO stringVO,
                                        final ArrDataString string,
                                        final MappingContext context) {
                        super.mapBtoA(stringVO, string, context);
                        string.setStringValue(StringNormalize.normalizeString(stringVO.getValue()));
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrDataUriRef.class, ArrItemUriRefVO.class).customize(
                new CustomMapper<ArrDataUriRef, ArrItemUriRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataUriRef uriRef,
                                        final ArrItemUriRefVO uriRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(uriRef, uriRefVO, context);
                        uriRefVO.setValue(uriRef.getUriRefValue());
                        uriRefVO.setDescription(uriRef.getDescription());
                        uriRefVO.setRefTemplateId(uriRef.getRefTemplate() != null ? uriRef.getRefTemplate().getRefTemplateId() : null);
                        uriRefVO.setNodeId(uriRef.getNodeId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemUriRefVO uriRefVO,
                                        final ArrDataUriRef uriRef,
                                        final MappingContext context) {
                        super.mapBtoA(uriRefVO, uriRef, context);
                        uriRef.setUriRefValue(uriRefVO.getValue());
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

        mapperFactory.classMap(ArrDataText.class, ArrItemTextVO.class).customize(
                new CustomMapper<ArrDataText, ArrItemTextVO>() {
                    @Override
                    public void mapAtoB(final ArrDataText text,
                                        final ArrItemTextVO textVO,
                                        final MappingContext context) {
                        super.mapAtoB(text, textVO, context);
                        textVO.setValue(text.getTextValue());
                    }

                    @Override
                    public void mapBtoA(final ArrItemTextVO textVO,
                                        final ArrDataText text,
                                        final MappingContext context) {
                        super.mapBtoA(textVO, text, context);
                        text.setTextValue(StringNormalize.normalizeText(textVO.getValue()));
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrDataText.class, ArrItemFormattedTextVO.class).customize(
                new CustomMapper<ArrDataText, ArrItemFormattedTextVO>() {
                    @Override
                    public void mapAtoB(final ArrDataText text,
                                        final ArrItemFormattedTextVO formattedTextVO,
                                        final MappingContext context) {
                        super.mapAtoB(text, formattedTextVO, context);
                        formattedTextVO.setValue(text.getTextValue());
                    }

                    @Override
                    public void mapBtoA(final ArrItemFormattedTextVO formattedTextVO,
                                        final ArrDataText text,
                                        final MappingContext context) {
                        super.mapBtoA(formattedTextVO, text, context);
                        text.setTextValue(formattedTextVO.getValue());
                    }
                }
        ).byDefault().register();

        mapperFactory.classMap(PersistentSortRunConfig.class, PersistentSortConfigVO.class).
                byDefault().
                exclude("nodeIds").
                register();

        mapperFactory.classMap(RulOutputFilter.class, RulOutputFilterVO.class).byDefault().field("outputFilterId", "id").register();
        mapperFactory.classMap(RulExportFilter.class, RulExportFilterVO.class).byDefault().field("exportFilterId", "id").register();
    }

    /**
     * Konvertor mezi LocalDateTime a Date.
     */
    public class LocalDateTimeConverter extends BidirectionalConverter<LocalDateTime, Date> {

        @Override
        public Date convertTo(final LocalDateTime localDateTime, final Type<Date> type, MappingContext mappingContext) {
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }

        @Override
        public LocalDateTime convertFrom(final Date date, final Type<LocalDateTime> type, MappingContext mappingContext) {
            return LocalDateTime.from(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }
    }

    /**
     * Konvertor mezi OffsetDateTime a Date.
     */
    public class OffsetDateTimeConverter extends BidirectionalConverter<OffsetDateTime, Date> {

        @Override
        public Date convertTo(final OffsetDateTime localDateTime, final Type<Date> type, MappingContext mappingContext) {
            return Date.from(localDateTime.toInstant());
        }

        @Override
        public OffsetDateTime convertFrom(final Date date, final Type<OffsetDateTime> type, MappingContext mappingContext) {
            return OffsetDateTime.from(date.toInstant());
        }
    }

    public class LocalDateConverter extends BidirectionalConverter<LocalDate, LocalDate> {

        @Override
        public LocalDate convertTo(LocalDate source, Type<LocalDate> destinationType, MappingContext mappingContext) {
            return (LocalDate.from(source));
        }

        @Override
        public LocalDate convertFrom(LocalDate source, Type<LocalDate> destinationType, MappingContext mappingContext) {
            return (LocalDate.from(source));
        }

    }

    public class DescItemTypeEnumConverter extends BidirectionalConverter<RulItemType.Type, Integer> {


        @Override
        public Integer convertTo(final RulItemType.Type type,
                                 final Type<Integer> type2, MappingContext mappingContext) {
            return RuleFactory.convertType(type);
        }

        @Override
        public RulItemType.Type convertFrom(final Integer type,
                                            final Type<RulItemType.Type> type2, MappingContext mappingContext) {
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
                                 final Type<Integer> type2, MappingContext mappingContext) {
            return RuleFactory.convertType(type);
        }

        @Override
        public RulItemSpec.Type convertFrom(final Integer type,
                                            final Type<RulItemSpec.Type> type2, MappingContext mappingContext) {
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
