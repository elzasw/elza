package cz.tacr.elza.controller.config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrChangeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.ArrNodeRegisterVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.BulkActionStateVO;
import cz.tacr.elza.controller.vo.BulkActionVO;
import cz.tacr.elza.controller.vo.NodeConformityErrorVO;
import cz.tacr.elza.controller.vo.NodeConformityMissingVO;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParDynastyVO;
import cz.tacr.elza.controller.vo.ParEventVO;
import cz.tacr.elza.controller.vo.ParPartyGroupIdentifierVO;
import cz.tacr.elza.controller.vo.ParPartyGroupVO;
import cz.tacr.elza.controller.vo.ParPartyNameComplementVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPersonVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
import cz.tacr.elza.controller.vo.RegExternalSourceVO;
import cz.tacr.elza.controller.vo.RegRecordSimple;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegScopeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemConstraintVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulPacketTypeVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.ScenarioOfNewLevelVO;
import cz.tacr.elza.controller.vo.XmlImportConfigVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.DescItemSpecLiteVO;
import cz.tacr.elza.controller.vo.nodes.DescItemTypeDescItemsLiteVO;
import cz.tacr.elza.controller.vo.nodes.DescItemTypeLiteVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemCoordinatesVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemDecimalVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemEnumVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemFormattedTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemIntVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemPacketVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemPartyRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemRecordRefVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemStringVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemTextVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemUnitdateVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemUnitidVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItemCoordinates;
import cz.tacr.elza.domain.ArrDescItemDecimal;
import cz.tacr.elza.domain.ArrDescItemEnum;
import cz.tacr.elza.domain.ArrDescItemFormattedText;
import cz.tacr.elza.domain.ArrDescItemInt;
import cz.tacr.elza.domain.ArrDescItemPacketRef;
import cz.tacr.elza.domain.ArrDescItemPartyRef;
import cz.tacr.elza.domain.ArrDescItemRecordRef;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemText;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParCreator;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
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
import cz.tacr.elza.domain.RegExternalSource;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.ScenarioOfNewLevel;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;


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
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RegRecordRepository recordRepository;

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
        mapperFactory.classMap(ArrDescItemDecimal.class, ArrDescItemDecimalVO.class).byDefault().field(
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
        mapperFactory.classMap(ArrDescItemPartyRef.class, ArrDescItemPartyRefVO.class).customize(
                new CustomMapper<ArrDescItemPartyRef, ArrDescItemPartyRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDescItemPartyRef partyRef,
                                        final ArrDescItemPartyRefVO patryRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(partyRef, patryRefVO, context);
                        patryRefVO.setValue(partyRef.getParty().getPartyId());
                    }

                    @Override
                    public void mapBtoA(final ArrDescItemPartyRefVO partyRefVO,
                                        final ArrDescItemPartyRef partyRef,
                                        final MappingContext context) {
                        super.mapBtoA(partyRefVO, partyRef, context);
                        partyRef.setParty(partyRepository.findOne(partyRefVO.getValue()));
                    }
                }).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemRecordRef.class, ArrDescItemRecordRefVO.class).customize(
                new CustomMapper<ArrDescItemRecordRef, ArrDescItemRecordRefVO>() {
                    @Override
                    public void mapAtoB(final ArrDescItemRecordRef recordRef,
                                        final ArrDescItemRecordRefVO recordRefVO,
                                        final MappingContext context) {
                        super.mapAtoB(recordRef, recordRefVO, context);
                        recordRefVO.setValue(recordRef.getRecord().getRecordId());
                    }

                    @Override
                    public void mapBtoA(final ArrDescItemRecordRefVO recordRefVO,
                                        final ArrDescItemRecordRef recordRef,
                                        final MappingContext context) {
                        super.mapBtoA(recordRefVO, recordRef, context);
                        recordRef.setRecord(recordRepository.findOne(recordRefVO.getValue()));
                    }
                }).byDefault().field(
                "descItemId", "id").register();
        mapperFactory.classMap(ArrDescItemString.class, ArrDescItemStringVO.class).byDefault().field(
                "descItemId", "id").register();

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

        mapperFactory.classMap(ArrChange.class, ArrChangeVO.class).byDefault().field("changeId", "id").register();
        mapperFactory.classMap(BulkActionConfig.class, BulkActionVO.class).customize(
                new CustomMapper<BulkActionConfig, BulkActionVO>() {
                    @Override
                    public void mapAtoB(final BulkActionConfig bulkActionConfig,
                                        final BulkActionVO bulkActionVO,
                                        final MappingContext context) {
                        bulkActionVO.setName((String) bulkActionConfig.getProperty("name"));
                    }
                }
        ).byDefault().register();
        mapperFactory.classMap(BulkActionState.class, BulkActionStateVO.class).field("bulkActionCode", "code").byDefault().register();
        mapperFactory.classMap(ParComplementType.class, ParComplementTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParDynasty.class, ParDynastyVO.class).byDefault().register();
        mapperFactory.classMap(ParParty.class, ParPartyVO.class)
                .exclude("prefferedName")
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
        mapperFactory.classMap(RulDescItemType.class, DescItemTypeDescItemsLiteVO.class).byDefault()
                .field("descItemTypeId", "id")
                .register();
        mapperFactory.classMap(RulDescItemTypeExt.class, RulDescItemTypeExtVO.class).byDefault()
                .field("descItemTypeId", "id")
                .field("rulDescItemSpecList", "descItemSpecs")
                .register();
        mapperFactory.classMap(RulDescItemTypeExt.class, DescItemTypeLiteVO.class).byDefault()
                .field("descItemTypeId", "id")
                .field("rulDescItemSpecList", "specs")
                .customize(new CustomMapper<RulDescItemTypeExt, DescItemTypeLiteVO>() {
                    @Override
                    public void mapAtoB(final RulDescItemTypeExt rulDescItemTypeExt,
                                        final DescItemTypeLiteVO descItemTypeLiteVO,
                                        final MappingContext context) {
                        super.mapAtoB(rulDescItemTypeExt, descItemTypeLiteVO, context);
                        descItemTypeLiteVO.setRep(rulDescItemTypeExt.getRepeatable() ? 1 : 0);
                    }
                })
                .register();
        mapperFactory.classMap(RulDescItemSpec.class, RulDescItemSpecVO.class).byDefault().field("descItemSpecId", "id").register();
        mapperFactory.classMap(RulDescItemSpecExt.class, RulDescItemSpecExtVO.class).byDefault().field("descItemSpecId",
                "id").register();
        mapperFactory.classMap(RulDescItemSpecExt.class, DescItemSpecLiteVO.class).byDefault()
                .field("descItemSpecId", "id")
                .customize(new CustomMapper<RulDescItemSpecExt, DescItemSpecLiteVO>() {
                    @Override
                    public void mapAtoB(final RulDescItemSpecExt rulDescItemSpecExt,
                                        final DescItemSpecLiteVO descItemSpecLiteVO,
                                        final MappingContext context) {
                        super.mapAtoB(rulDescItemSpecExt, descItemSpecLiteVO, context);
                        descItemSpecLiteVO.setRep(rulDescItemSpecExt.getRepeatable() ? 1 : 0);
                    }
                })
                .register();

        mapperFactory.classMap(RulPacketType.class, RulPacketTypeVO.class).byDefault().field("packetTypeId", "id").register();

        mapperFactory.classMap(RulRuleSet.class, RulRuleSetVO.class).byDefault().field("ruleSetId", "id").register();

        mapperFactory.classMap(ScenarioOfNewLevel.class, ScenarioOfNewLevelVO.class).byDefault().register();

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

        mapperFactory.classMap(XmlImportConfig.class, XmlImportConfigVO.class).byDefault().register();
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

    public class DescItemTypeEnumConverter extends BidirectionalConverter<RulDescItemType.Type, Integer> {


        @Override
        public Integer convertTo(final RulDescItemType.Type type,
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
        public RulDescItemType.Type convertFrom(final Integer type,
                                                final Type<RulDescItemType.Type> type2) {
            switch (type) {
                case 3:
                    return RulDescItemType.Type.REQUIRED;
                case 2:
                    return RulDescItemType.Type.RECOMMENDED;
                case 1:
                    return RulDescItemType.Type.POSSIBLE;
                case 0:
                    return RulDescItemType.Type.IMPOSSIBLE;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
        }

    }

    public class DescItemSpecEnumConverter extends BidirectionalConverter<RulDescItemSpec.Type, Integer> {


        @Override
        public Integer convertTo(final RulDescItemSpec.Type type,
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
        public RulDescItemSpec.Type convertFrom(final Integer type,
                                                final Type<RulDescItemSpec.Type> type2) {
            switch (type) {
                case 3:
                    return RulDescItemSpec.Type.REQUIRED;
                case 2:
                    return RulDescItemSpec.Type.RECOMMENDED;
                case 1:
                    return RulDescItemSpec.Type.POSSIBLE;
                case 0:
                    return RulDescItemSpec.Type.IMPOSSIBLE;
                default:
                    throw new IllegalStateException("Type convert not defined: " + type);
            }
        }

    }

}
