package cz.tacr.elza.controller.config;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.vo.*;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.*;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.PacketRepository;
import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    private PacketRepository packetRepository;
    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    /**
     * @return Tovární třída.
     */
    @Bean(name = "configVOMapper")
    public MapperFactory configVOMapper() {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
        initSimpleVO(mapperFactory);

        mapperFactory.getConverterFactory().registerConverter(new LocalDateTimeConverter());

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
                                arrNodeConformityMissing.getDescItemType().getDescItemTypeId());
                        nodeConformityMissingVO.setDescItemSpecId(
                                arrNodeConformityMissing.getDescItemSpec() == null ? null : arrNodeConformityMissing
                                        .getDescItemSpec().getDescItemSpecId());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrDescItemCoordinates.class, ArrDescItemCoordinatesVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemEnum.class, ArrDescItemEnumVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemFormattedText.class, ArrDescItemFormattedTextVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemInt.class, ArrDescItemIntVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemText.class, ArrDescItemTextVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemUnitid.class, ArrDescItemUnitidVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemUnitdate.class, ArrDescItemUnitdateVO.class).customize(
                new CustomMapper<ArrDescItemUnitdate, ArrDescItemUnitdateVO>() {
                    @Override
                    public void mapAtoB(final ArrDescItemUnitdate unitdate,
                                        final ArrDescItemUnitdateVO unitdateVO,
                                        final MappingContext context) {
                        unitdateVO.setCalendarTypeId(unitdate.getCalendarType().getCalendarTypeId());
                        unitdateVO.setId(unitdate.getDescItemId());
                        unitdateVO.setValue(UnitDateConvertor.convertToString(unitdate));
                    }

                    @Override
                    public void mapBtoA(final ArrDescItemUnitdateVO arrDescItemUnitdateVO,
                                        final ArrDescItemUnitdate unitdate,
                                        final MappingContext context) {
                        unitdate.setCalendarType(
                                calendarTypeRepository.findOne(arrDescItemUnitdateVO.getCalendarTypeId()));
                        unitdate.setDescItemId(arrDescItemUnitdateVO.getId());
                        UnitDateConvertor.convertToUnitDate(arrDescItemUnitdateVO.getValue(), unitdate);
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrDescItemPacketRef.class, ArrDescItemPacketVO.class).customize(
                new CustomMapper<ArrDescItemPacketRef, ArrDescItemPacketVO>() {
                    @Override
                    public void mapAtoB(final ArrDescItemPacketRef descItemPacketRef,
                                        final ArrDescItemPacketVO descItemPacketVO,
                                        final MappingContext context) {
                        super.mapAtoB(descItemPacketRef, descItemPacketVO, context);
                        descItemPacketVO.setValue(descItemPacketRef.getPacket().getPacketId());
                    }

                    @Override
                    public void mapBtoA(final ArrDescItemPacketVO descItemPacketVO,
                                        final ArrDescItemPacketRef descItemPacketRef,
                                        final MappingContext context) {
                        super.mapBtoA(descItemPacketVO, descItemPacketRef, context);
                        descItemPacketRef.setPacket(packetRepository.findOne(descItemPacketVO.getValue()));
                    }
                }).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemPartyRef.class, ArrDescItemPartyRefVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemRecordRef.class, ArrDescItemRecordRefVO.class).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemString.class, ArrDescItemStringVO.class).byDefault().field(
                "descItemId", "id").register();

        mapperFactory.classMap(ArrNode.class, ArrNodeVO.class).byDefault().field("nodeId", "id").register();

        mapperFactory.classMap(ArrPacket.class, ArrPacketVO.class).customize(
                new CustomMapper<ArrPacket, ArrPacketVO>() {
                    @Override
                    public void mapAtoB(final ArrPacket packet,
                                        final ArrPacketVO packetVO,
                                        final MappingContext context) {
                        packetVO.setId(packet.getPacketId());
                        packetVO.setInvalidPacket(packet.getInvalidPacket());
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
                        packet.setInvalidPacket(packetVO.getInvalidPacket());
                        packet.setStorageNumber(packetVO.getStorageNumber());
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(BulkActionConfig.class, BulkActionVO.class).byDefault().register();
        mapperFactory.classMap(BulkActionState.class, BulkActionStateVO.class).byDefault().register();
        mapperFactory.classMap(ParComplementType.class, ParComplementTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParDynasty.class, ParDynastyVO.class).byDefault().register();
        mapperFactory.classMap(ParParty.class, ParPartyVO.class).exclude("prefferedName").exclude("partyNames").exclude(
                "partyCreators")
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
                                .join(Arrays.asList(parPartyName.getMainPart(), parPartyName.getOtherPart()), " ");
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
                    }

                    @Override
                    public void mapBtoA(final ParRelationVO relationVO,
                                        final ParRelation parRelation,
                                        final MappingContext context) {
                        if(relationVO.getPartyId() != null){
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
                .exclude("format")
                .customize(
                        new CustomMapper<ParUnitdate, ParUnitdateVO>() {
                            @Override
                            public void mapAtoB(final ParUnitdate parUnitdate,
                                                final ParUnitdateVO unitdateVO,
                                                final MappingContext context) {
                                unitdateVO.setCalendarTypeId(parUnitdate.getCalendarType().getCalendarTypeId());
                                unitdateVO.setUnitdateId(parUnitdate.getUnitdateId());
                                String textDate;
                                if (parUnitdate.getTextDate() == null) {
                                    try {
                                        textDate = UnitDateConvertor.convertToString(parUnitdate);
                                    } catch (Exception e) {
                                        textDate = parUnitdate.getTextDate();
                                    }
                                } else {
                                    textDate = parUnitdate.getTextDate();
                                }
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

                        if(regRecordVO.getRegisterTypeId() != null){
                            RegRegisterType regRegisterType = new RegRegisterType();
                            regRegisterType.setRegisterTypeId(regRecordVO.getRegisterTypeId());
                            regRecord.setRegisterType(regRegisterType);
                        }

                        if(regRecordVO.getScopeId() != null){
                            RegScope scope = new RegScope();
                            scope.setScopeId(regRecordVO.getScopeId());
                            regRecord.setScope(scope);
                        }
                    }
                }).byDefault().register();

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

                        if(regRegisterType.getPartyType() != null){
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
        mapperFactory.classMap(RulArrangementType.class, RulArrangementTypeVO.class).byDefault().field(
                "arrangementTypeId", "id").register();
        mapperFactory.classMap(RulDataType.class, RulDataTypeVO.class).byDefault().field("dataTypeId", "id").register();


        mapperFactory.classMap(RulDescItemConstraint.class, RulDescItemConstraintVO.class).customize(
                new CustomMapper<RulDescItemConstraint, RulDescItemConstraintVO>() {
                    @Override
                    public void mapAtoB(final RulDescItemConstraint descItemConstraint,
                                        final RulDescItemConstraintVO descItemConstraintVO,
                                        final MappingContext context) {
                        descItemConstraintVO.setId(descItemConstraint.getDescItemConstraintId());
                        descItemConstraintVO
                                .setDescItemTypeId(descItemConstraint.getDescItemType().getDescItemTypeId());
                        if (descItemConstraint.getDescItemSpec() != null) {
                            descItemConstraintVO
                                    .setDescItemSpecId(descItemConstraint.getDescItemSpec().getDescItemSpecId());
                        }
                        if (descItemConstraint.getVersion() != null) {
                            descItemConstraintVO
                                    .setFindingAidVersionId(descItemConstraint.getVersion().getFindingAidVersionId());
                        }
                    }
                }).byDefault().register();
        mapperFactory.classMap(RulDescItemType.class, RulDescItemTypeDescItemsVO.class).byDefault().field(
                "descItemTypeId",
                "id").register();
        mapperFactory.classMap(RulDescItemTypeExt.class, RulDescItemTypeExtVO.class).byDefault()
                .field("descItemTypeId", "id")
                .field("rulDescItemSpecList", "descItemSpecs")
                .register();
        mapperFactory.classMap(RulDescItemSpec.class, RulDescItemSpecVO.class).byDefault().field("descItemSpecId", "id").register();
        mapperFactory.classMap(RulDescItemSpecExt.class, RulDescItemSpecExtVO.class).byDefault().field("descItemSpecId",
                "id").register();

        mapperFactory.classMap(RulPacketType.class, RulPacketTypeVO.class).byDefault().field("packetTypeId", "id").register();

        mapperFactory.classMap(RulRuleSet.class, RulRuleSetVO.class).byDefault().field("ruleSetId", "id").register();

        mapperFactory.classMap(ArrFindingAid.class, ArrFindingAidVO.class).byDefault().field("findingAidId", "id").register();
        mapperFactory.classMap(ArrFindingAidVersion.class, ArrFindingAidVersionVO.class).byDefault().field(
                "findingAidVersionId", "id").
                exclude("arrangementType").register();
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

}
