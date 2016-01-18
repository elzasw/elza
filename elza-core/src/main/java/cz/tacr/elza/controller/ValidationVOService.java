package cz.tacr.elza.controller;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParEventEditVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPersonEditVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RelationEntityRepository;
import cz.tacr.elza.repository.RelationRepository;
import cz.tacr.elza.repository.RelationRoleTypeRepository;
import cz.tacr.elza.repository.RelationTypeRepository;


/**
 * Třída pro validaci vstupních VO objektů (zkontroluje, že objekty mají vyplněny povinné hodnoty a že mají nastaveny
 * číselníky, které existují.)
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.01.2016
 */
@Service
public class ValidationVOService {

    @Autowired
    private RelationRepository relationRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RelationTypeRepository relationTypeRepository;
    @Autowired
    private CalendarTypeRepository calendarTypeRepository;
    @Autowired
    private RelationEntityRepository relationEntityRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    public void checkParty(final ParPartyEditVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby s id: " + partyVO.getPartyTypeId());
        }

        // object type dle party type ?
        if (partyVO instanceof ParDynastyEditVO
                && !ParPartyType.PartyTypeEnum.DYNASTY.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPersonEditVO
                && !ParPartyType.PartyTypeEnum.PERSON.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParEventEditVO
                && !ParPartyType.PartyTypeEnum.EVENT.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPartyGroupEditVO
                && !ParPartyType.PartyTypeEnum.GROUP_PARTY.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        } else {
            throw new IllegalArgumentException("Je povinné alespoň jedno preferované jméno.");
        }
        // end CHECK
    }


    public void checkPartyUpdate(final ParPartyEditVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby s id: " + partyVO.getPartyTypeId());
        }

        if (partyVO.getPartyId() == null) {
            throw new IllegalArgumentException("Není vyplněno id existující entity pro update.");
        }

        ParParty parParty = partyRepository.getOne(partyVO.getPartyId());
        if (!parParty.getPartyType().getPartyTypeId().equals(partyVO.getPartyTypeId())) {
            throw new IllegalArgumentException("Nelze měnit typ osoby.");
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        }
    }


    public void checkRelation(final ParRelationVO relation) {
        Assert.notNull(relation);

        Assert.notNull(relation.getComplementType(), "Není nastaven typ vztahu.");
        Assert.notNull(relation.getComplementType().getRelationTypeId(), "Není nastaven typ vztahu.");

        Assert.notNull(relation.getPartyId());
        ParParty party = partyRepository.findOne(relation.getPartyId());
        Assert.notNull(party, "Nebyla nalezena osoba s id " + relation.getPartyId());

        Integer relationTypeId = relation.getComplementType().getRelationTypeId();
        ParRelationType relationType = relationTypeRepository.findOne(relationTypeId);
        Assert.notNull(relationType, "Není nalezen typ vztahu s id " + relationTypeId);

        if (relation.getFrom() != null) {
            checkUnitDate(relation.getFrom());
        }
        if (relation.getTo() != null) {
            checkUnitDate(relation.getTo());
        }
    }

    public void checkUnitDate(final ParUnitdateVO unitdateVO) {
        Assert.notNull(unitdateVO);

        if (unitdateVO.getCalendarType() != null) {
            Assert.notNull(unitdateVO.getCalendarType().getId(), "Není nastaveno id typu kalendáře datace.");
            ArrCalendarType calendarType = calendarTypeRepository.findOne(unitdateVO.getCalendarType().getId());
            Assert.notNull(calendarType, "Nebyl nalezen typ kalendáře s id " + unitdateVO.getCalendarType().getId());
        }
    }

    public void checkRelationEntity(final ParRelationEntityVO relationEntityVO, final boolean checkRelation) {

        Integer relationEntityId = relationEntityVO.getRelationEntityId();
        if (relationEntityId != null) {
            ParRelationEntity relationEntity = relationEntityRepository.findOne(relationEntityId);
            Assert.notNull(relationEntity, "Nebyla nalezena vazba vztahu na entitu s id " + relationEntityId);
        }

        Assert.notNull(relationEntityVO.getRecord());
        Assert.notNull(relationEntityVO.getRecord().getRecordId());
        RegRecord record = recordRepository.findOne(relationEntityVO.getRecord().getRecordId());
        Assert.notNull(record, "Nebyl nalezeno rejstříkové heslo s id " + relationEntityVO.getRecord().getRecordId());

        Assert.notNull(relationEntityVO.getRoleType());
        Assert.notNull(relationEntityVO.getRoleType().getRoleTypeId());
        ParRelationRoleType roleType = relationRoleTypeRepository
                .findOne(relationEntityVO.getRoleType().getRoleTypeId());
        Assert.notNull(roleType,
                "Nebyl nalezen typ relace entity s id " + relationEntityVO.getRoleType().getRoleTypeId());

        if (checkRelation) {
            Assert.notNull(relationEntityVO.getRelationId());
            ParRelation relation = relationRepository.findOne(relationEntityVO.getRelationId());
            Assert.notNull(relation, "Nebyla nalezena vazba s id " + relationEntityVO.getRelationId());
        }
    }


    private void checkPreferredNameExist(final List<ParPartyNameEditVO> partyNameEditVOs) {
        boolean isPreferred = false;
        for (final ParPartyNameEditVO partyName : partyNameEditVOs) {
            if (partyName.isPreferredName()) {
                isPreferred = true;
            }
        }
        if (!isPreferred) {
            throw new IllegalArgumentException("Není přítomno žádné preferované jméno osoby.");
        }
    }
}
