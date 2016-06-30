package cz.tacr.elza.controller.config;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.*;
import cz.tacr.elza.controller.vo.nodes.descitems.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.controller.vo.DmsFileVO;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import ma.glasnost.orika.*;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


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
    private PacketRepository packetRepository;
    @Autowired
    private CalendarTypeRepository calendarTypeRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private RuleService ruleService;

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


       mapperFactory.classMap(ArrItemCoordinates.class, ArrItemCoordinatesVO.class).customize(
                new CustomMapper<ArrItemCoordinates, ArrItemCoordinatesVO>() {
            @Override
            public void mapAtoB(final ArrItemCoordinates coordinates,
                                final ArrItemCoordinatesVO coordinatesVO,
                                final MappingContext context) {
                String type = coordinates.getValue().getGeometryType().toUpperCase();
                if (type.equals("POINT")) {
                    coordinatesVO.setValue(new WKTWriter().writeFormatted(coordinates.getValue()));
                } else {
                    coordinatesVO.setValue(type + "( " + coordinates.getValue().getCoordinates().length + " )");
                }
            }

            @Override
            public void mapBtoA(final ArrItemCoordinatesVO coordinatesVO,
                                final ArrItemCoordinates coordinates,
                                final MappingContext context) {
                WKTReader reader = new WKTReader();
                try {
                    coordinates.setValue(reader.read(coordinatesVO.getValue()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
                        unitdateVO.setCalendarTypeId(unitdate.getCalendarType().getCalendarTypeId());
                        unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
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

        mapperFactory.classMap(ArrItemPacketRef.class, ArrItemPacketVO.class).customize(
                new CustomMapper<ArrItemPacketRef, ArrItemPacketVO>() {
                    @Override
                    public void mapAtoB(final ArrItemPacketRef descItemPacketRef,
                                        final ArrItemPacketVO descItemPacketVO,
                                        final MappingContext context) {
                        super.mapAtoB(descItemPacketRef, descItemPacketVO, context);
                        descItemPacketVO.setValue(descItemPacketRef.getPacket().getPacketId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemPacketVO descItemPacketVO,
                                        final ArrItemPacketRef descItemPacketRef,
                                        final MappingContext context) {
                        super.mapBtoA(descItemPacketVO, descItemPacketRef, context);
                        descItemPacketRef.setPacket(packetRepository.findOne(descItemPacketVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemFileRef.class, ArrItemFileRefVO.class).customize(
                new CustomMapper<ArrItemFileRef, ArrItemFileRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemFileRef arrItemFileRef,
                                        final ArrItemFileRefVO arrItemFileRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(arrItemFileRef, arrItemFileRefVO, context);
                        arrItemFileRefVO.setValue(arrItemFileRef.getFile().getFileId());
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
                        patryRefVO.setValue(partyRef.getParty().getPartyId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemPartyRefVO partyRefVO,
                                        final ArrItemPartyRef partyRef,
                                        final MappingContext context) {
                        super.mapBtoA(partyRefVO, partyRef, context);
                        partyRef.setParty(partyRepository.findOne(partyRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemRecordRef.class, ArrItemRecordRefVO.class).customize(
                new CustomMapper<ArrItemRecordRef, ArrItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrItemRecordRef recordRef,
                                        final ArrItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef.getRecord().getRecordId());
                    }

                    @Override
                    public void mapBtoA(final ArrItemRecordRefVO recordRefVO,
                                        final ArrItemRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setRecord(recordRepository.findOne(recordRefVO.getValue()));
                    }
                }).byDefault().register();
        mapperFactory.classMap(ArrItemString.class, ArrItemStringVO.class).byDefault().register();

        mapperFactory.classMap(ArrNodeRegister.class, ArrNodeRegisterVO.class).byDefault().field(
                "nodeRegisterId", "id").register();

        mapperFactory.classMap(ArrNode.class, ArrNodeVO.class).byDefault().field("nodeId", "id").register();

        mapperFactory.classMap(ArrPacket.class, ArrPacketVO.class).customize(
                new CustomMapper<ArrPacket, ArrPacketVO>() {
                    @Override
                    public void mapAtoB(final ArrPacket packet,
                                        final ArrPacketVO packetVO,
                                        final MappingContext context) {
                        packetVO.setId(packet.getPacketId());
                        packetVO.setState(packet.getState());
                        if (packet.getPacketType() != null) {
                            packetVO.setPacketTypeId(packet.getPacketType().getPacketTypeId());
                        }
                        packetVO.setStorageNumber(packet.getStorageNumber());
                    }

                    @Override
                    public void mapBtoA(final ArrPacketVO packetVO,
                                        final ArrPacket packet,
                                        final MappingContext context) {
                        packet.setPacketId(packetVO.getId());
                        packet.setState(packetVO.getState());
                        packet.setStorageNumber(packetVO.getStorageNumber());
                    }
                }
        ).byDefault().register();

        mapperFactory.classMap(ArrChange.class, ArrChangeVO.class).byDefault().field("changeId", "id").register();
        mapperFactory.classMap(BulkActionConfig.class, BulkActionVO.class).customize(
                new CustomMapper<BulkActionConfig, BulkActionVO>() {
                    @Override
                    public void mapAtoB(final BulkActionConfig bulkActionConfig,
                                        final BulkActionVO bulkActionVO,
                                        final MappingContext context) {
                        bulkActionVO.setName((String) bulkActionConfig.getProperty("name"));
                        bulkActionVO.setDescription((String) bulkActionConfig.getProperty("description"));
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(ArrBulkActionRun.class, BulkActionRunVO.class).field("bulkActionRunId", "id").field("bulkActionCode", "code").byDefault().register();
        mapperFactory.classMap(ArrFile.class, ArrFileVO.class).field("fileId", "id").exclude("file").byDefault().customize(new CustomMapper<ArrFile, ArrFileVO>() {
            @Override
            public void mapAtoB(ArrFile arrFile, ArrFileVO arrFileVO, MappingContext mappingContext) {
                arrFileVO.setFundId(arrFile.getFund().getFundId());
            }

            @Override
            public void mapBtoA(ArrFileVO arrFileVO, ArrFile arrFile, MappingContext mappingContext) {
                if (arrFileVO.getFundId() != null) {
                    ArrFund fund = fundRepository.findOne(arrFileVO.getFundId());
                    Assert.notNull(fund, "Archivní pomůcka neexistuje (ID=" + arrFileVO.getFundId() + ")");
                    arrFile.setFund(fund);
                }
            }
        }).register();
        mapperFactory.classMap(ArrOutputFile.class, ArrOutputFileVO.class).field("fileId", "id").exclude("file").byDefault().customize(new CustomMapper<ArrOutputFile, ArrOutputFileVO>() {
            @Override
            public void mapAtoB(ArrOutputFile arrOutputFile, ArrOutputFileVO arrOutputFileVO, MappingContext mappingContext) {
                arrOutputFileVO.setOutputResultId(arrOutputFile.getOutputResult().getOutputResultId());
            }

            @Override
            public void mapBtoA(ArrOutputFileVO arrOutputFileVO, ArrOutputFile arrOutputFile, MappingContext mappingContext) {
                ArrOutputResult result = outputResultRepository.findOne(arrOutputFileVO.getOutputResultId());
                Assert.notNull(result, "Archivní pomůcka neexistuje (ID=" + arrOutputFileVO.getOutputResultId() + ")");
                arrOutputFile.setOutputResult(result);
            }
        }).register();
        mapperFactory.classMap(DmsFile.class, DmsFileVO.class).field("fileId", "id").exclude("file").byDefault().register();
        mapperFactory.classMap(ParComplementType.class, ParComplementTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParDynasty.class, ParDynastyVO.class).byDefault().register();
        mapperFactory.classMap(ParParty.class, ParPartyVO.class)
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
                                creatorParty.setPartyId(creator.getPartyId());
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

        mapperFactory.classMap(RegRecord.class, RegRecord.class)
                .exclude(RegRecord.RECORD_ID)
                .exclude(RegRecord.PARENT_RECORD)
            .byDefault().register();


        mapperFactory.classMap(ParPartyGroup.class, ParPartyGroupVO.class).byDefault().register();
        mapperFactory.classMap(ParPartyGroupIdentifier.class, ParPartyGroupIdentifierVO.class).customize(
                new CustomMapper<ParPartyGroupIdentifier, ParPartyGroupIdentifierVO>() {
                    @Override
                    public void mapAtoB(final ParPartyGroupIdentifier parPartyGroupIdentifier,
                                        final ParPartyGroupIdentifierVO parPartyGroupIdentifierVO,
                                        final MappingContext context) {
                        ParPartyGroup parPartyGroup = parPartyGroupIdentifier.getPartyGroup();
                        parPartyGroupIdentifierVO.setPartyId(parPartyGroup.getPartyId());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParPartyName.class, ParPartyNameVO.class).customize(
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

        mapperFactory.classMap(ParPartyNameFormType.class, ParPartyNameFormTypeVO.class).byDefault().register();

        mapperFactory.classMap(ParPartyType.class, ParPartyTypeVO.class).byDefault().register();


        mapperFactory.classMap(ParPerson.class, ParPersonVO.class).byDefault().register();

        mapperFactory.classMap(ParRelation.class, ParRelationVO.class).customize(
                new CustomMapper<ParRelation, ParRelationVO>() {
                    @Override
                    public void mapAtoB(final ParRelation parRelation,
                                        final ParRelationVO parRelationVO,
                                        final MappingContext context) {
                        ParParty party = parRelation.getParty();
                        parRelationVO.setPartyId(party.getPartyId());
                        parRelationVO.setDisplayName(parRelation.getNote());
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
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParRelationEntity.class, ParRelationEntityVO.class).customize(
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
                            roleType.setRoleTypeId(relationEntityVO.getRoleType().getRoleTypeId());
                            parRelationEntity.setRoleType(roleType);
                        }

                        if (relationEntityVO.getRecord() != null) {
                            RegRecord record = new RegRecord();
                            record.setRecordId(relationEntityVO.getRecord().getRecordId());
                            parRelationEntity.setRecord(record);
                        }
                    }
                }).byDefault().register();


        mapperFactory.classMap(ParRelationRoleType.class, ParRelationRoleTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParRelationType.class, ParRelationTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParUnitdate.class, ParUnitdateVO.class)
                .exclude("valueFrom").exclude("valueFromEstimated")
                .exclude("valueTo").exclude("valueToEstimated")
                .customize(
                        new CustomMapper<ParUnitdate, ParUnitdateVO>() {
                            @Override
                            public void mapAtoB(final ParUnitdate parUnitdate,
                                                final ParUnitdateVO unitdateVO,
                                                final MappingContext context) {
                                if (parUnitdate.getCalendarType() != null) {
                                    unitdateVO.setCalendarTypeId(parUnitdate.getCalendarType().getCalendarTypeId());
                                }
                                unitdateVO.setUnitdateId(parUnitdate.getUnitdateId());
                                String textDate = UnitDateConvertor.convertParUnitDateToString(parUnitdate);
                                unitdateVO.setTextDate(textDate);
                            }

                            @Override
                            public void mapBtoA(final ParUnitdateVO unitdateVO,
                                                final ParUnitdate parUnitdate,
                                                final MappingContext context) {
                                parUnitdate.setUnitdateId(unitdateVO.getUnitdateId());
                                try {
                                    UnitDateConvertor.convertToUnitDate(unitdateVO.getTextDate(), parUnitdate);
                                    parUnitdate.setTextDate(null);
                                } catch (Exception e) {
                                    parUnitdate.setTextDate(unitdateVO.getTextDate());
                                }
                                ArrCalendarType calendarType = new ArrCalendarType();
                                calendarType.setCalendarTypeId(unitdateVO.getCalendarTypeId());
                                parUnitdate.setCalendarType(calendarType);

                            }
                        }).byDefault().register();

        mapperFactory.classMap(RegRecord.class, RegRecordVO.class)
                .exclude("registerType")
                .exclude("scope")
                .exclude("variantRecordList")
                .customize(new CustomMapper<RegRecord, RegRecordVO>() {
                    @Override
                    public void mapAtoB(final RegRecord regRecord,
                                        final RegRecordVO regRecordVO,
                                        final MappingContext context) {
                        RegRecord parentRecord = regRecord.getParentRecord();
                        if (parentRecord != null) {
                            regRecordVO.setParentRecordId(parentRecord.getRecordId());
                        }

                        regRecordVO.setRegisterTypeId(regRecord.getRegisterType().getRegisterTypeId());
                        regRecordVO.setAddRecord(regRecord.getRegisterType().getAddRecord());
                        regRecordVO.setHierarchical(regRecord.getRegisterType().getHierarchical());
                        regRecordVO.setScopeId(regRecord.getScope().getScopeId());
                    }

                    @Override
                    public void mapBtoA(final RegRecordVO regRecordVO,
                                        final RegRecord regRecord,
                                        final MappingContext context) {
                        if (regRecordVO.getParentRecordId() != null) {
                            RegRecord parent = new RegRecord();
                            parent.setRecordId(regRecordVO.getParentRecordId());
                            regRecord.setParentRecord(parent);
                        }

                        if (regRecordVO.getRegisterTypeId() != null) {
                            RegRegisterType regRegisterType = new RegRegisterType();
                            regRegisterType.setRegisterTypeId(regRecordVO.getRegisterTypeId());
                            regRecord.setRegisterType(regRegisterType);
                        }

                        if (regRecordVO.getScopeId() != null) {
                            RegScope scope = new RegScope();
                            scope.setScopeId(regRecordVO.getScopeId());
                            regRecord.setScope(scope);
                        }
                    }
                }).byDefault().register();
        mapperFactory.classMap(RegRecord.class, RegRecordSimple.class).field("recordId", "id").byDefault().register();

        mapperFactory.classMap(RegExternalSource.class, RegExternalSourceVO.class).byDefault().register();


        mapperFactory.classMap(RegRegisterType.class, RegRegisterTypeVO.class).customize(
                new CustomMapper<RegRegisterType, RegRegisterTypeVO>() {
                    @Override
                    public void mapAtoB(final RegRegisterType regRegisterType,
                                        final RegRegisterTypeVO regRegisterTypeVO,
                                        final MappingContext context) {
                        RegRegisterType parentType = regRegisterType.getParentRegisterType();
                        if (parentType != null) {
                            regRegisterTypeVO.setParentRegisterTypeId(parentType.getRegisterTypeId());
                        }

                        if (regRegisterType.getPartyType() != null) {
                            regRegisterTypeVO.setPartyTypeId(regRegisterType.getPartyType().getPartyTypeId());
                        }
                    }

                    @Override
                    public void mapBtoA(final RegRegisterTypeVO registerTypeVO,
                                        final RegRegisterType regRegisterType,
                                        final MappingContext context) {
                        if (registerTypeVO.getPartyTypeId() != null) {
                            ParPartyType partyType = new ParPartyType();
                            partyType.setPartyTypeId(registerTypeVO.getPartyTypeId());
                            regRegisterType.setPartyType(partyType);
                        }
                    }
                }).field("registerTypeId", "id").byDefault()
                .register();
        mapperFactory.classMap(RegScope.class, RegScopeVO.class).field("scopeId", "id").byDefault().register();
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

        mapperFactory.classMap(RulPacketType.class, RulPacketTypeVO.class).byDefault().field("packetTypeId", "id").register();
        mapperFactory.classMap(RulOutputType.class, RulOutputTypeVO.class).byDefault().field("outputTypeId", "id").register();

        mapperFactory.classMap(RulRuleSet.class, RulRuleSetVO.class)
                .byDefault()
                .field("ruleSetId", "id")
                .customize(new CustomMapper<RulRuleSet, RulRuleSetVO>() {
                    @Override
                    public void mapAtoB(final RulRuleSet rulRuleSet, final RulRuleSetVO rulRuleSetVO, final MappingContext context) {
                        super.mapAtoB(rulRuleSet, rulRuleSetVO, context);
                        rulRuleSetVO.setItemTypeCodes(ruleService.getDefaultItemTypeCodes(rulRuleSet));
                    }
                })
                .register();

        mapperFactory.classMap(RulPolicyType.class, RulPolicyTypeVO.class).byDefault().field("policyTypeId", "id").register();

        mapperFactory.classMap(ScenarioOfNewLevel.class, ScenarioOfNewLevelVO.class).byDefault().register();

        mapperFactory.classMap(ArrFund.class, ArrFundVO.class).byDefault().field("fundId", "id").register();
        mapperFactory.classMap(ArrFundVersion.class, ArrFundVersionVO.class).byDefault().field(
                "fundVersionId", "id").
                exclude("arrangementType").register();
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

        mapperFactory.classMap(RegVariantRecord.class, RegVariantRecordVO.class).customize(
                new CustomMapper<RegVariantRecord, RegVariantRecordVO>() {
                    @Override
                    public void mapAtoB(final RegVariantRecord regVariantRecord,
                                        final RegVariantRecordVO regVariantRecordVO,
                                        final MappingContext context) {
                        RegRecord regRecord = regVariantRecord.getRegRecord();
                        regVariantRecordVO.setRegRecordId(regRecord.getRecordId());
                    }

                    @Override
                    public void mapBtoA(final RegVariantRecordVO regVariantRecordVO,
                                        final RegVariantRecord regVariantRecord,
                                        final MappingContext context) {
                        if (regVariantRecordVO.getRegRecordId() != null) {
                            RegRecord regRecord = new RegRecord();
                            regRecord.setRecordId(regVariantRecordVO.getRegRecordId());
                            regVariantRecord.setRegRecord(regRecord);
                        }
                    }
                }).byDefault().register();

        mapperFactory.classMap(RegCoordinates.class, RegCoordinatesVO.class).customize(
                new CustomMapper<RegCoordinates, RegCoordinatesVO>() {
                    @Override
                    public void mapAtoB(final RegCoordinates coordinates,
                                        final RegCoordinatesVO coordinatesVO,
                                        final MappingContext context) {
                        String type = coordinates.getValue().getGeometryType().toUpperCase();
                        if (type.equals("POINT")) {
                            coordinatesVO.setValue(new WKTWriter().writeFormatted(coordinates.getValue()));
                        } else {
                            coordinatesVO.setValue(type + "( " + coordinates.getValue().getCoordinates().length + " )");
                        }
                        coordinatesVO.setRegRecordId(coordinates.getRegRecord().getRecordId());
                    }

                    @Override
                    public void mapBtoA(final RegCoordinatesVO coordinatesVO,
                                        final RegCoordinates coordinates,
                                        final MappingContext context) {
                        WKTReader reader = new WKTReader();
                        try {
                            coordinates.setValue(reader.read(coordinatesVO.getValue()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }).exclude("value").byDefault().register();

        mapperFactory.classMap(XmlImportConfig.class, XmlImportConfigVO.class).byDefault().register();

        mapperFactory.classMap(UserDetail.class, UserInfoVO.class)
                .byDefault()
                .field("userPermission", "userPermissions")
                .register();
        mapperFactory.classMap(UserPermission.class, UserPermissionInfoVO.class)
                .byDefault()
                .register();

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
                .register();

        mapperFactory.classMap(RulTemplate.class, RulTemplateVO.class)
                .byDefault()
                .field("templateId", "id")
                .register();
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
