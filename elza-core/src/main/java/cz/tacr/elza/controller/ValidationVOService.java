package cz.tacr.elza.controller;

import java.util.HashMap;
import java.util.List;

import cz.tacr.elza.api.UseUnitdateEnum;
import cz.tacr.elza.domain.ParPartyType.PartyTypeEnum;
import cz.tacr.elza.domain.ParRelationType;
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
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
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
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private RegisterTypeRepository registerTypeRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RelationTypeRepository relationTypeRepository;

    /**
     * Mapa Typů osob na třídy
     */
    private final static HashMap<PartyTypeEnum, Class> partyTypeToClass = new HashMap<>(4);


    static {
        // Plnění statických map
        partyTypeToClass.put(PartyTypeEnum.DYNASTY, ParDynastyVO.class);
        partyTypeToClass.put(PartyTypeEnum.PERSON, ParPersonVO.class);
        partyTypeToClass.put(PartyTypeEnum.EVENT, ParEventVO.class);
        partyTypeToClass.put(PartyTypeEnum.GROUP_PARTY, ParPartyGroupVO.class);
    }

    /**
     * Zkontroluje zda party je instancí objektu a pokud ano zkontroluje zda odpovídá jeho party type enum
     * @param party osoba
     * @param partyType typ osoby
     * @param checkedClass třída
     * @param checkedEnum typ enum.3
     */
    private void partyCheckerHelper(final ParPartyVO party, final ParPartyType partyType, final Class checkedClass, final PartyTypeEnum checkedEnum) {
        if (checkedClass.isInstance(party) && !checkedEnum.equals(partyType.getPartyTypeEnum())) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }
    }

    /**
     *
     * @param partyVO
     */
    public void checkParty(final ParPartyVO partyVO) {
        final ParPartyType partyType = partyTypeRepository.getOneCheckExist(partyVO.getPartyType().getId());

        // Definice TypeEnum na Class
        partyTypeToClass.forEach((type, clazz) -> partyCheckerHelper(partyVO, partyType, clazz, type));

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        checkPreferredNameExist(partyVO.getPartyNames());
    }


    public void checkPartyUpdate(final ParPartyVO partyVO) {
        ParPartyType partyType = partyTypeRepository.getOneCheckExist(partyVO.getPartyType().getId());

        if (partyVO.getId() == null) {
            throw new IllegalArgumentException("Není vyplněno id existující entity pro update.");
        }

        ParParty parParty = partyRepository.getOneCheckExist(partyVO.getId());
        if (!parParty.getPartyType().getPartyTypeId().equals(partyVO.getPartyType().getId())) {
            throw new IllegalArgumentException("Nelze měnit typ osoby.");
        }

        List<RegRegisterType> regRegisterTypes = registerTypeRepository.findRegisterTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(regRegisterTypes)) {
            throw new IllegalArgumentException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode());
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        }
    }


    public void checkRelation(final ParRelationVO relation) {
        Assert.notNull(relation);

        Assert.notNull(relation.getRelationTypeId(), "Není nastaven typ vztahu.");

        Assert.notNull(relation.getPartyId());
        partyRepository.getOneCheckExist(relation.getPartyId());

        Integer relationTypeId = relation.getRelationTypeId();
        ParRelationType relationType = relationTypeRepository.getOneCheckExist(relationTypeId);

        boolean isInterval = UseUnitdateEnum.INTERVAL.equals(relationType.getUseUnitdate());
        if ((UseUnitdateEnum.ONE.equals(relationType.getUseUnitdate()) || isInterval) && relation.getFrom() != null) {
            checkUnitDate(relation.getFrom());
        }
        if (isInterval && relation.getTo() != null) {
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
