package cz.tacr.elza.controller.config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cz.tacr.elza.bulkaction.generator.PersistentSortRunConfig;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.service.attachment.AttachmentService;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.repository.StructuredObjectRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.vo.ApVariantRecordVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.DescItemSpecLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFileRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemJsonTableVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStructureVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemPartyRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemUnitidVO;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.service.RuleService;
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
    private FundRepository fundRepository;
    @Autowired
    private FundFileRepository fundFileRepository;
    @Autowired
    private OutputResultRepository outputResultRepository;
    @Autowired
    private StructuredObjectRepository structureDataRepository;
    @Autowired
    private CalendarTypeRepository calendarTypeRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private ApRecordRepository recordRepository;
    @Autowired
    private RuleService ruleService;
    @Autowired
    private AttachmentService attachmentService;

    /**
     * @return Tovární třída.
     */
    @Bean(name = "configVOMapper")
    public MapperFactory configVOMapper() {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        initSimpleVO(mapperFactory);

        mapperFactory.getConverterFactory().registerConverter(new LocalDateTimeConverter());
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
                                calendarTypeRepository.findOne(arrItemUnitdateVO.getCalendarTypeId()));
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
                        itemStructureRef.setStructuredObject(itemStructureVO.getValue() == null ? null : structureDataRepository.findOne(itemStructureVO.getValue()));
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
                        arrItemFileRef.setFile(fundFileRepository.findOne(arrItemFileRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemPartyRef.class, ArrItemPartyRefVO.class).customize(
                new CustomMapper<ArrItemPartyRef, ArrItemPartyRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemPartyRef partyRef,
                                        final ArrItemPartyRefVO patryRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(partyRef, patryRefVO, context);
                        patryRefVO.setValue(partyRef.getParty() == null ? null : partyRef.getParty().getPartyId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemPartyRefVO partyRefVO,
                                        final ArrItemPartyRef partyRef,
                                        final MappingContext context) {
                        super.mapBtoA(partyRefVO, partyRef, context);
                        partyRef.setParty(partyRefVO.getValue() == null ? null : partyRepository.findOne(partyRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemRecordRef.class, ArrItemRecordRefVO.class).customize(
                new CustomMapper<ArrItemRecordRef, ArrItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemRecordRef recordRef,
                                        final ArrItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef == null || recordRef.getRecord() == null ? null : recordRef.getRecord().getRecordId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemRecordRefVO recordRefVO,
                                        final ArrItemRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setRecord(recordRefVO.getValue() == null ? null : recordRepository.findOne(recordRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemString.class, ArrItemStringVO.class).byDefault().register();

        mapperFactory.classMap(ArrNodeRegister.class, ArrNodeRegisterVO.class).byDefault().field(
                "nodeRegisterId", "id").register();

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
        mapperFactory.classMap(ArrFile.class, ArrFileVO.class).field("fileId", "id").exclude("file").byDefault().customize(new CustomMapper<ArrFile, ArrFileVO>() {
            @Override
            public void mapAtoB(final ArrFile arrFile, final ArrFileVO arrFileVO, final MappingContext mappingContext) {

                arrFileVO.setFundId(arrFile.getFund().getFundId());
                arrFileVO.setEditable(attachmentService.isEditable(arrFile.getMimeType()));
                arrFileVO.setGeneratePdf(attachmentService.supportGenerateTo(arrFile, "application/pdf"));
            }

            @Override
            public void mapBtoA(final ArrFileVO arrFileVO, final ArrFile arrFile, final MappingContext mappingContext) {
                if (arrFileVO.getFundId() != null) {
                    ArrFund fund = fundRepository.findOne(arrFileVO.getFundId());
                    Assert.notNull(fund, "Archivní pomůcka neexistuje (ID=" + arrFileVO.getFundId() + ")");
                    arrFile.setFund(fund);
                }
            }
        }).register();
        mapperFactory.classMap(ArrOutputFile.class, ArrOutputFileVO.class).field("fileId", "id").exclude("file").byDefault().customize(new CustomMapper<ArrOutputFile, ArrOutputFileVO>() {
            @Override
            public void mapAtoB(final ArrOutputFile arrOutputFile, final ArrOutputFileVO arrOutputFileVO, final MappingContext mappingContext) {
                arrOutputFileVO.setOutputResultId(arrOutputFile.getOutputResult().getOutputResultId());
            }

            @Override
            public void mapBtoA(final ArrOutputFileVO arrOutputFileVO, final ArrOutputFile arrOutputFile, final MappingContext mappingContext) {
                ArrOutputResult result = outputResultRepository.findOne(arrOutputFileVO.getOutputResultId());
                Assert.notNull(result, "Archivní pomůcka neexistuje (ID=" + arrOutputFileVO.getOutputResultId() + ")");
                arrOutputFile.setOutputResult(result);
            }
        }).register();
        mapperFactory.classMap(DmsFile.class, DmsFileVO.class).field("fileId", "id").exclude("file").byDefault().register();
        mapperFactory.classMap(ParComplementType.class, ParComplementTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParDynasty.class, ParDynastyVO.class).byDefault().register();
        mapperFactory.classMap(ParParty.class, ParPartyVO.class)
                .field("partyId", "id")
                .exclude("preferredName")
                .exclude("partyNames")
                .exclude("partyCreators")
                .exclude("relations")
                .customize(new CustomMapper<ParParty, ParPartyVO>() {

                    @Override
                    public void mapBtoA(final ParPartyVO parPartyVO,
                                        final ParParty party,
                                        final MappingContext context) {


                        if (CollectionUtils.isNotEmpty(parPartyVO.getCreators())) {
                            List<ParCreator> creators = new ArrayList<>(parPartyVO.getCreators().size());
                            for (ParPartyVO creator : parPartyVO.getCreators()) {
                                ParCreator parCreator = new ParCreator();
                                ParParty creatorParty = new ParParty();
                                creatorParty.setPartyId(creator.getId());
                                parCreator.setCreatorParty(creatorParty);
                                creators.add(parCreator);
                            }
                            party.setPartyCreators(creators);
                        }
                    }
                })
                .byDefault().register();


        mapperFactory.classMap(ParEvent.class, ParEventVO.class).byDefault().register();
        mapperFactory.classMap(ParInstitution.class, ParInstitutionVO.class).byDefault()
                .field("institutionId", "id").register();
        mapperFactory.classMap(ParInstitutionType.class, ParInstitutionTypeVO.class).byDefault()
                .field("institutionTypeId", "id").register();

        mapperFactory.classMap(ApRecord.class, ApRecord.class)
                .exclude(ApRecord.RECORD_ID)
            .byDefault().register();


        mapperFactory.classMap(ParPartyGroup.class, ParPartyGroupVO.class).byDefault().register();
        mapperFactory.classMap(ParPartyGroupIdentifier.class, ParPartyGroupIdentifierVO.class)
                .field("partyGroupIdentifierId", "id")
                .customize(
                new CustomMapper<ParPartyGroupIdentifier, ParPartyGroupIdentifierVO>() {
                    @Override
                    public void mapAtoB(final ParPartyGroupIdentifier parPartyGroupIdentifier,
                                        final ParPartyGroupIdentifierVO parPartyGroupIdentifierVO,
                                        final MappingContext context) {
                        ParPartyGroup parPartyGroup = parPartyGroupIdentifier.getPartyGroup();
                        parPartyGroupIdentifierVO.setPartyId(parPartyGroup.getPartyId());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParPartyName.class, ParPartyNameVO.class)
                .field("partyNameId", "id")
                .customize(
                new CustomMapper<ParPartyName, ParPartyNameVO>() {
                    @Override
                    public void mapAtoB(final ParPartyName parPartyName,
                                        final ParPartyNameVO parPartyNameVO,
                                        final MappingContext context) {
                        ParParty party = parPartyName.getParty();
                        if (party != null) {
                            parPartyNameVO.setPartyId(party.getPartyId());
                        }

                        String displayName = StringUtils
                                .join(Arrays.asList(parPartyName.getMainPart(), parPartyName.getOtherPart(),
                                        parPartyName.getDegreeBefore(), parPartyName.getDegreeAfter()), " ");
                        displayName = displayName.replaceAll("\\s+", " ");
                        parPartyNameVO.setDisplayName(displayName.trim());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParPartyNameComplement.class, ParPartyNameComplementVO.class)
                .field("partyNameComplementId", "id")
                .exclude("partyName").customize(new CustomMapper<ParPartyNameComplement, ParPartyNameComplementVO>() {
            @Override
            public void mapAtoB(final ParPartyNameComplement complement,
                                final ParPartyNameComplementVO complementVO,
                                final MappingContext context) {
                complementVO.setComplementTypeId(complement.getComplementType() == null
                                                 ? null : complement.getComplementType().getComplementTypeId());
            }

            @Override
            public void mapBtoA(final ParPartyNameComplementVO complementVO,
                                final ParPartyNameComplement complement,
                                final MappingContext context) {
                if (complementVO.getComplementTypeId() != null) {
                    ParComplementType complementType = new ParComplementType();
                    complementType.setComplementTypeId(complementVO.getComplementTypeId());
                    complement.setComplementType(complementType);
                }
            }
        }).byDefault().register();

        mapperFactory.classMap(ParPartyNameFormType.class, ParPartyNameFormTypeVO.class).field("nameFormTypeId", "id").byDefault().register();

        mapperFactory.classMap(ParPartyType.class, ParPartyTypeVO.class).field("partyTypeId", "id").byDefault().register();


        mapperFactory.classMap(ParPerson.class, ParPersonVO.class).byDefault().register();

        mapperFactory.classMap(ParRelation.class, ParRelationVO.class).field("relationId", "id").exclude("relationEntities").customize(
                new CustomMapper<ParRelation, ParRelationVO>() {
                    @Override
                    public void mapAtoB(final ParRelation parRelation,
                                        final ParRelationVO parRelationVO,
                                        final MappingContext context) {
                        ParParty party = parRelation.getParty();
                        parRelationVO.setPartyId(party.getPartyId());
                        parRelationVO.setDisplayName(parRelation.getNote());
                        parRelationVO.setRelationTypeId(parRelation.getRelationType().getRelationTypeId());
                    }

                    @Override
                    public void mapBtoA(final ParRelationVO relationVO,
                                        final ParRelation parRelation,
                                        final MappingContext context) {
                        if (relationVO.getPartyId() != null) {
                            ParParty party = new ParParty();
                            party.setPartyId(relationVO.getPartyId());
                            parRelation.setParty(party);
                        }
                        ParRelationType parRelationType = new ParRelationType();
                        parRelationType.setRelationTypeId(relationVO.getRelationTypeId());
                        parRelation.setRelationType(parRelationType);
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParRelationEntity.class, ParRelationEntityVO.class).field("relationEntityId", "id").customize(
                new CustomMapper<ParRelationEntity, ParRelationEntityVO>() {
                    @Override
                    public void mapAtoB(final ParRelationEntity parRelationEntity,
                                        final ParRelationEntityVO parRelationEntityVO,
                                        final MappingContext context) {
                        ParRelation relation = parRelationEntity.getRelation();
                        parRelationEntityVO.setRelationId(relation.getRelationId());
                    }

                    @Override
                    public void mapBtoA(final ParRelationEntityVO relationEntityVO,
                                        final ParRelationEntity parRelationEntity,
                                        final MappingContext context) {

                        if (relationEntityVO.getRoleType() != null) {
                            ParRelationRoleType roleType = new ParRelationRoleType();
                            roleType.setRoleTypeId(relationEntityVO.getRoleType().getId());
                            parRelationEntity.setRoleType(roleType);
                        }

                        if (relationEntityVO.getRecord() != null) {
                            ApRecord record = new ApRecord();
                            record.setRecordId(relationEntityVO.getRecord().getId());
                            parRelationEntity.setRecord(record);
                        }
                    }
                }).byDefault().register();


        mapperFactory.classMap(ParRelationRoleType.class, ParRelationRoleTypeVO.class).field("roleTypeId", "id").byDefault().register();
        mapperFactory.classMap(ParRelationType.class, ParRelationTypeVO.class).field("relationTypeId", "id").byDefault().register();
        mapperFactory.classMap(ParUnitdate.class, ParUnitdateVO.class).field("unitdateId", "id").customize(
            new CustomMapper<ParUnitdate, ParUnitdateVO>() {
                @Override
                public void mapAtoB(final ParUnitdate parUnitdate,
                                    final ParUnitdateVO unitdateVO,
                                    final MappingContext context) {
                    if (parUnitdate.getCalendarType() != null) {
                        unitdateVO.setCalendarTypeId(parUnitdate.getCalendarType().getCalendarTypeId());
                    }
                    if (parUnitdate.getFormat() != null) {
                        unitdateVO.setValue(UnitDateConvertor.convertToString(parUnitdate));
                    }
                }

                @Override
                public void mapBtoA(final ParUnitdateVO unitdateVO,
                                    final ParUnitdate parUnitdate,
                                    final MappingContext context) {
                    if (unitdateVO.getValue() != null) {
                        UnitDateConvertor.convertToUnitDate(unitdateVO.getValue(), parUnitdate);
                    }
                    Integer calendarTypeId = unitdateVO.getCalendarTypeId();
                    parUnitdate.setCalendarType(calendarTypeId != null ? calendarTypeRepository.getOneCheckExist(calendarTypeId) : null);
                }
            }).byDefault().register();

        mapperFactory.classMap(ApRecord.class, ApRecordVO.class)
                .exclude("apType")
                .exclude("scope")
                .exclude("variantRecordList")
                .field("recordId", "id")
                .customize(new CustomMapper<ApRecord, ApRecordVO>() {
                    @Override
                    public void mapAtoB(final ApRecord apRecord,
                                        final ApRecordVO apRecordVO,
                                        final MappingContext context) {
                        apRecordVO.setApTypeId(apRecord.getApType().getApTypeId());
                        apRecordVO.setAddRecord(apRecord.getApType().getAddRecord());
                        apRecordVO.setScopeId(apRecord.getScope().getScopeId());
                        apRecordVO.setInvalid(apRecord.isInvalid());
                    }

                    @Override
                    public void mapBtoA(final ApRecordVO apRecordVO,
                                        final ApRecord apRecord,
                                        final MappingContext context) {

                        if (apRecordVO.getApTypeId() != null) {
                            ApType apType = new ApType();
                            apType.setApTypeId(apRecordVO.getApTypeId());
                            apRecord.setApType(apType);
                        }

                        if (apRecordVO.getScopeId() != null) {
                            ApScope scope = new ApScope();
                            scope.setScopeId(apRecordVO.getScopeId());
                            apRecord.setScope(scope);
                        }
                    }
                }).byDefault().register();
        mapperFactory.classMap(ApRecord.class, ApRecordSimple.class).field("recordId", "id").byDefault().register();

        mapperFactory.classMap(ApExternalSystem.class, ApExternalSystemVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitizationFrontdesk.class, ArrDigitizationFrontdeskVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitalRepository.class, ArrDigitalRepositoryVO.class).field("externalSystemId", "id").byDefault().register();

        mapperFactory.classMap(ApExternalSystem.class, ApExternalSystemSimpleVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitizationFrontdesk.class, ArrDigitizationFrontdeskSimpleVO.class).field("externalSystemId", "id").byDefault().register();
        mapperFactory.classMap(ArrDigitalRepository.class, ArrDigitalRepositorySimpleVO.class).field("externalSystemId", "id").byDefault().register();


        mapperFactory.classMap(ApType.class, ApTypeVO.class).customize(
                new CustomMapper<ApType, ApTypeVO>() {
                    @Override
                    public void mapAtoB(final ApType apType,
                                        final ApTypeVO apTypeVO,
                                        final MappingContext context) {
                        ApType parentType = apType.getParentApType();
                        if (parentType != null) {
                            apTypeVO.setParentApTypeId(parentType.getApTypeId());
                        }

                        if (apType.getPartyType() != null) {
                            apTypeVO.setPartyTypeId(apType.getPartyType().getPartyTypeId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ApTypeVO apTypeVO,
                                        final ApType apType,
                                        final MappingContext context) {
                        if (apTypeVO.getPartyTypeId() != null) {
                            ParPartyType partyType = new ParPartyType();
                            partyType.setPartyTypeId(apTypeVO.getPartyTypeId());
                            apType.setPartyType(partyType);
                        }
                    }
                }).field("apTypeId", "id").byDefault()
                .register();
        mapperFactory.classMap(ApScope.class, ApScopeVO.class).field("scopeId", "id").byDefault().register();
        mapperFactory.classMap(RulDataType.class, RulDataTypeVO.class).byDefault().field("dataTypeId", "id").register();


        mapperFactory.classMap(RulItemType.class, RulDescItemTypeDescItemsVO.class).byDefault().field(
                "itemTypeId",
                "id").register();
        mapperFactory.classMap(RulItemType.class, ItemTypeDescItemsLiteVO.class).byDefault()
                .field("itemTypeId", "id")
                .register();
        mapperFactory.classMap(RulItemTypeExt.class, RulDescItemTypeExtVO.class).byDefault()
                .field("itemTypeId", "id")
                .field("rulItemSpecList", "descItemSpecs")
                .field("structuredTypeId", "structureTypeId")
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
                        List<SettingGridView.ItemType> itemTypes = ruleService.getGridView(rulRuleSet);
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
                        rulRuleSetVO.setItemTypeCodes(ruleService.getItemTypeCodesByRuleSet(rulRuleSet));
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
        mapperFactory.classMap(ArrOutputDefinition.class, ArrOutputDefinitionVO.class)
//                .exclude("outputs")
                .exclude("nodes")
                .byDefault()
                .field("outputDefinitionId", "id")
                .customize(new CustomMapper<ArrOutputDefinition, ArrOutputDefinitionVO>() {
                    @Override
                    public void mapAtoB(final ArrOutputDefinition outputDefinition,
                                        final ArrOutputDefinitionVO outputDefinitionVO,
                                        final MappingContext context) {
                        outputDefinitionVO.setOutputTypeId(outputDefinition.getOutputType().getOutputTypeId());
                        outputDefinitionVO.setTemplateId(outputDefinition.getTemplate() != null ? outputDefinition.getTemplate().getTemplateId() : null);
                        if (outputDefinition.getOutputResult() != null) {
                            outputDefinitionVO.setOutputResultId(outputDefinition.getOutputResult().getOutputResultId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrOutputDefinitionVO outputDefinitionVO,
                                        final ArrOutputDefinition outputDefinition,
                                        final MappingContext context) {
                        RulOutputType rulOutputType = new RulOutputType();
                        rulOutputType.setOutputTypeId(outputDefinitionVO.getOutputTypeId());
                        outputDefinition.setOutputType(rulOutputType);

                        if (outputDefinitionVO.getOutputResultId() != null) {
                            ArrOutputResult outputResult = new ArrOutputResult();
                            outputResult.setOutputResultId(outputDefinitionVO.getOutputResultId());
                            outputDefinition.setOutputResult(outputResult);
                        }
                    }
                }).register();
        mapperFactory.classMap(ArrOutput.class, ArrOutputVO.class).byDefault().field("outputId", "id").register();
        mapperFactory.getConverterFactory().registerConverter(new PassThroughConverter(LocalDateTime.class));

        mapperFactory.classMap(ApVariantRecord.class, ApVariantRecordVO.class)
                .field("variantRecordId", "id")
                .customize(
                new CustomMapper<ApVariantRecord, ApVariantRecordVO>() {
                    @Override
                    public void mapAtoB(final ApVariantRecord apVariantRecord,
                                        final ApVariantRecordVO apVariantRecordVO,
                                        final MappingContext context) {
                        ApRecord apRecord = apVariantRecord.getApRecord();
                        apVariantRecordVO.setApRecordId(apRecord.getRecordId());
                    }

                    @Override
                    public void mapBtoA(final ApVariantRecordVO apVariantRecordVO,
                                        final ApVariantRecord apVariantRecord,
                                        final MappingContext context) {
                        if (apVariantRecordVO.getApRecordId() != null) {
                            ApRecord apRecord = new ApRecord();
                            apRecord.setRecordId(apVariantRecordVO.getApRecordId());
                            apVariantRecord.setApRecord(apRecord);
                        }
                    }
                }).byDefault().register();

        mapperFactory.classMap(UsrUser.class, UsrUserVO.class)
                .byDefault()
                .field("userId", "id")
                .register();
        mapperFactory.classMap(UsrGroup.class, UsrGroupVO.class)
                .byDefault()
                .field("groupId", "id")
                .register();
        mapperFactory.classMap(UsrPermission.class, UsrPermissionVO.class)
                .byDefault()
                .field("permissionId", "id")
                .customize(new CustomMapper<UsrPermission, UsrPermissionVO>() {
                    @Override
                    public void mapAtoB(final UsrPermission usrPermission, final UsrPermissionVO usrPermissionVO, final MappingContext context) {
                        Class targetEntity = (Class) context.getProperty("targetEntity");
                        final boolean inherited;
                        if (targetEntity == UsrUser.class) {
                            inherited = usrPermission.getGroup() != null;
                        } else if (targetEntity == UsrGroup.class) {
                            inherited = false;
                        } else {
                            throw new IllegalStateException("Neznámý typ entity " + targetEntity);
                        }
                        usrPermissionVO.setInherited(inherited);
                        if (inherited) {
                            usrPermissionVO.setGroupId(usrPermission.getGroup().getGroupId());
                        }
                    }
                })
                .register();

        mapperFactory.classMap(RulTemplate.class, RulTemplateVO.class)
                .byDefault()
                .field("templateId", "id")
                .register();

        mapperFactory.classMap(UISettings.class, UISettingsVO.class)
                .byDefault()
                .field("settingsId", "id")
                .register();

        mapperFactory.classMap(ParRelationClassType.class, ParRelationClassTypeVO.class)
                .byDefault()
                .field("relationClassTypeId", "id")
                .register();

        mapperFactory.classMap(UIPartyGroup.class, UIPartyGroupVO.class)
                .byDefault()
                .field("partyGroupId", "id")
                .register();

        mapperFactory.classMap(RulStructuredType.class, RulStructureTypeVO.class)
                .byDefault()
                .field("structuredTypeId", "id")
                .register();

        mapperFactory.classMap(ArrDao.class, ArrDaoVO.class)
                .field("daoId", "id")
                .byDefault()
                .register();

        mapperFactory.classMap(ArrDaoFile.class, ArrDaoFileVO.class)
                .field("daoFileId", "id")
                .exclude("dao")
                .byDefault()
                .register();

        mapperFactory.classMap(ArrDaoFileGroup.class, ArrDaoFileGroupVO.class)
                .field("daoFileGroupId", "id")
                .exclude("fileList")
                .exclude("fileCount")
                .exclude("dao")
                .byDefault()
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
        mapperFactory.classMap(ArrDataJsonTable.class, ArrItemJsonTableVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataDecimal.class, ArrItemDecimalVO.class).byDefault().register();
        mapperFactory.classMap(ArrDataUnitid.class, ArrItemUnitidVO.class).byDefault().register();
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
                                calendarTypeRepository.findOne(arrItemUnitdateVO.getCalendarTypeId()));
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
                        if (itemStructureDataRef.getStructuredObject() != null) {
                            itemStructureVO.setValue(itemStructureDataRef.getStructuredObjectId());
                        }
                    }

                    @Override
                    public void mapBtoA(final ArrItemStructureVO itemStructureVO,
                                        final ArrDataStructureRef itemStructureRef,
                                        final MappingContext context) {
                        super.mapBtoA(itemStructureVO, itemStructureRef, context);
                        itemStructureRef.setStructuredObject(itemStructureVO.getValue() == null ? null : structureDataRepository.findOne(itemStructureVO.getValue()));
                    }
                })
                .field("structuredObject", "structureData")
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
                        arrItemFileRef.setFile(fundFileRepository.findOne(arrItemFileRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrDataPartyRef.class, ArrItemPartyRefVO.class).customize(
                new CustomMapper<ArrDataPartyRef, ArrItemPartyRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataPartyRef partyRef,
                                        final ArrItemPartyRefVO patryRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(partyRef, patryRefVO, context);
                        patryRefVO.setValue(partyRef.getParty() == null ? null : partyRef.getParty().getPartyId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemPartyRefVO partyRefVO,
                                        final ArrDataPartyRef partyRef,
                                        final MappingContext context) {
                        super.mapBtoA(partyRefVO, partyRef, context);
                        partyRef.setParty(partyRefVO.getValue() == null ? null : partyRepository.findOne(partyRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrDataRecordRef.class, ArrItemRecordRefVO.class).customize(
                new CustomMapper<ArrDataRecordRef, ArrItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDataRecordRef recordRef,
                                        final ArrItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef == null || recordRef.getRecord() == null ? null : recordRef.getRecord().getRecordId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemRecordRefVO recordRefVO,
                                        final ArrDataRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setRecord(recordRefVO.getValue() == null ? null : recordRepository.findOne(recordRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrDataString.class, ArrItemStringVO.class).byDefault().register();

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

    public class DescItemTypeEnumConverter extends BidirectionalConverter<RulItemType.Type, Integer> {


        @Override
        public Integer convertTo(final RulItemType.Type type,
                                                 final Type<Integer> type2) {
            switch (type) {
                case REQUIRED:
                    return 3;
                case RECOMMENDED:
                    return 2;
                case POSSIBLE:
                    return 1;
                case IMPOSSIBLE:
                    return 0;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
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
            switch (type) {
                case REQUIRED:
                    return 3;
                case RECOMMENDED:
                    return 2;
                case POSSIBLE:
                    return 1;
                case IMPOSSIBLE:
                    return 0;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
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
