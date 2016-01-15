package cz.tacr.elza.controller.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.tacr.elza.controller.vo.ArrCalendarTypeVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVO;
import cz.tacr.elza.controller.vo.ArrFindingAidVersionVO;
import cz.tacr.elza.controller.vo.ArrPacketVO;
import cz.tacr.elza.controller.vo.ParComplementTypeVO;
import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParDynastyVO;
import cz.tacr.elza.controller.vo.ParEventEditVO;
import cz.tacr.elza.controller.vo.ParEventVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupIdentifierVO;
import cz.tacr.elza.controller.vo.ParPartyGroupVO;
import cz.tacr.elza.controller.vo.ParPartyNameComplementVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameFormTypeVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeEditVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeVO;
import cz.tacr.elza.controller.vo.ParPartyTypeVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPersonEditVO;
import cz.tacr.elza.controller.vo.ParPersonVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationRoleTypeVO;
import cz.tacr.elza.controller.vo.ParRelationTypeVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateEditVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
import cz.tacr.elza.controller.vo.RegExternalSourceVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegRegisterTypeVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.controller.vo.RulArrangementTypeVO;
import cz.tacr.elza.controller.vo.RulDataTypeVO;
import cz.tacr.elza.controller.vo.RulDescItemConstraintVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;
import cz.tacr.elza.controller.vo.RulRuleSetVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemSpecExtVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeDescItemsVO;
import cz.tacr.elza.controller.vo.nodes.RulDescItemTypeExtVO;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemCoordinatesVO;
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
import cz.tacr.elza.domain.ArrDescItemCoordinates;
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
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyTimeRange;
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
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulDescItemTypeExt;
import cz.tacr.elza.domain.RulRuleSet;
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
                    }
                }).byDefault().register();

        mapperFactory.classMap(ArrDescItemPacketRef.class, ArrDescItemPacketVO.class).byDefault().field(
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
                        packetVO.setFindingAidId(packet.getFindingAid().getFindingAidId());
                        packetVO.setInvalidPacket(packet.getInvalidPacket());
                        packetVO.setPacketTypeId(packet.getPacketType().getPacketTypeId());
                    }
                }
        ).byDefault().register();

        mapperFactory.classMap(ParComplementType.class, ParComplementTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParDynasty.class, ParDynastyVO.class).byDefault().register();
        mapperFactory.classMap(ParParty.class, ParPartyVO.class).exclude("preferredName").byDefault().register();
        mapperFactory.classMap(ParEvent.class, ParEventVO.class).byDefault().register();
        mapperFactory.classMap(ParPartyEditVO.class, ParParty.class).byDefault().register();

        mapperFactory.classMap(ParDynastyEditVO.class, ParDynasty.class).byDefault().register();
        mapperFactory.classMap(ParPersonEditVO.class, ParPerson.class).byDefault().register();
        mapperFactory.classMap(ParEventEditVO.class, ParEvent.class).byDefault().register();
        mapperFactory.classMap(ParPartyGroupEditVO.class, ParPartyGroup.class).byDefault().register();

        mapperFactory.classMap(RegRecord.class, RegRecord.class)
                .exclude(RegRecord.RECORD_ID)
                .exclude(RegRecord.PARENT_RECORD)
            .byDefault().register();

        mapperFactory.classMap(ParPartyTimeRangeEditVO.class, ParPartyTimeRange.class).byDefault().register();
        mapperFactory.classMap(ParUnitdateEditVO.class, ParUnitdate.class).byDefault().register();

        mapperFactory.classMap(ParPartyNameEditVO.class, ParPartyName.class).byDefault().register();
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
                        parPartyNameVO.setPartyId(party.getPartyId());
                    }
                }).byDefault().register();

        mapperFactory.classMap(ParPartyNameComplement.class, ParPartyNameComplementVO.class)
                .exclude("partyName").byDefault().register();

        mapperFactory.classMap(ParPartyNameFormType.class, ParPartyNameFormTypeVO.class).byDefault().register();


        mapperFactory.classMap(ParPartyTimeRange.class, ParPartyTimeRangeVO.class).customize(
                new CustomMapper<ParPartyTimeRange, ParPartyTimeRangeVO>() {
                    @Override
                    public void mapAtoB(final ParPartyTimeRange parPartyTimeRange,
                                        final ParPartyTimeRangeVO parPartyTimeRangeVO,
                                        final MappingContext context) {
                        ParParty party = parPartyTimeRange.getParty();
                        parPartyTimeRangeVO.setPartyId(party.getPartyId());
                    }
                }).byDefault().register();

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
                }).byDefault().register();


        mapperFactory.classMap(ParRelationRoleType.class, ParRelationRoleTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParRelationType.class, ParRelationTypeVO.class).byDefault().register();
        mapperFactory.classMap(ParUnitdate.class, ParUnitdateVO.class).byDefault().register();

        mapperFactory.classMap(RegRecord.class, RegRecordVO.class)
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
                    }
                }).field("registerTypeId", "id").byDefault()
                .register();

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
