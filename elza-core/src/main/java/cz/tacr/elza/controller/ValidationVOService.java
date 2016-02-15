package cz.tacr.elza.controller;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.ParDynastyVO;
import cz.tacr.elza.controller.vo.ParEventVO;
import cz.tacr.elza.controller.vo.ParPartyGroupVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPersonVO;
import cz.tacr.elza.controller.vo.ParRelationEntityVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
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


    public void checkParty(final ParPartyVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyType() != null && partyVO.getPartyType().getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyType().getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby");
        }

        // object type dle party type ?
        if (partyVO instanceof ParDynastyVO
                && !ParPartyType.PartyTypeEnum.DYNASTY.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPersonVO
                && !ParPartyType.PartyTypeEnum.PERSON.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParEventVO
                && !ParPartyType.PartyTypeEnum.EVENT.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
        if (partyVO instanceof ParPartyGroupVO
                && !ParPartyType.PartyTypeEnum.GROUP_PARTY.equals(partyType.getPartyTypeEnum())) {

            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException(
                    "Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        checkPreferredNameExist(partyVO.getPartyNames());
        // end CHECK
    }


    public void checkPartyUpdate(final ParPartyVO partyVO) {
        ParPartyType partyType;
        if (partyVO.getPartyType().getPartyTypeId() != null) {
            partyType = partyTypeRepository.getOne(partyVO.getPartyType().getPartyTypeId());
        } else {
            throw new IllegalArgumentException("Nenalezen typ osoby s id: " + partyVO.getPartyId());
        }

        if (partyVO.getPartyId() == null) {
            throw new IllegalArgumentException("Není vyplněno id existující entity pro update.");
        }

        ParParty parParty = partyRepository.getOne(partyVO.getPartyId());
        if (!parParty.getPartyType().getPartyTypeId().equals(partyVO.getPartyType().getPartyTypeId())) {
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

        Assert.notNull(unitdateVO.getCalendarTypeId(), "Není nastaveno id typu kalendáře datace.");
    }



    private void checkPreferredNameExist(final List<ParPartyNameVO> partyNameEditVOs) {

        for (ParPartyNameVO partyName : partyNameEditVOs) {
            if (partyName.isPrefferedName()) {
                return;
            }
        }
        throw new IllegalArgumentException("Není přítomno žádné preferované jméno osoby.");
    }
}
