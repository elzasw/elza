package cz.tacr.elza.controller;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.api.UseUnitdateEnum;
import cz.tacr.elza.controller.vo.ParDynastyVO;
import cz.tacr.elza.controller.vo.ParEventVO;
import cz.tacr.elza.controller.vo.ParPartyGroupVO;
import cz.tacr.elza.controller.vo.ParPartyNameVO;
import cz.tacr.elza.controller.vo.ParPartyVO;
import cz.tacr.elza.controller.vo.ParPersonVO;
import cz.tacr.elza.controller.vo.ParRelationVO;
import cz.tacr.elza.controller.vo.ParUnitdateVO;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
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
    private ApAccessPointRepository apAccessPointRepository;
    @Autowired
    private ApStateRepository apStateRepository;
    @Autowired
    private ApTypeRepository apTypeRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RelationTypeRepository relationTypeRepository;

    /**
     * Mapa Typů osob na třídy
     */
    private final static HashMap<PartyType, Class<? extends ParPartyVO>> partyTypeToClass = new HashMap<>(4);


    static {
        // Plnění statických map
        partyTypeToClass.put(PartyType.DYNASTY, ParDynastyVO.class);
        partyTypeToClass.put(PartyType.PERSON, ParPersonVO.class);
        partyTypeToClass.put(PartyType.EVENT, ParEventVO.class);
        partyTypeToClass.put(PartyType.GROUP_PARTY, ParPartyGroupVO.class);
    }

    /**
     * Zkontroluje zda party je instancí objektu a pokud ano zkontroluje zda odpovídá jeho party type enum
     * @param party osoba
     * @param partyType typ osoby
     * @param checkedClass třída
     * @param checkedEnum typ enum.3
     */
    private void partyCheckerHelper(final ParPartyVO party, final ParPartyType partyType, final Class<? extends ParPartyVO> checkedClass, final PartyType checkedEnum) {
        if (checkedClass.isInstance(party) && !checkedEnum.equals(partyType.toEnum())) {
            throw new ObjectNotFoundException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode(), BaseCode.ID_NOT_EXIST);
        }
    }

    public ApState checkAccessPoint(Integer accessPointId) {
        if (accessPointId != null) {
            ApAccessPoint accessPoint = apAccessPointRepository.findOne(accessPointId);
            if (accessPoint != null) {
                ApState state = apStateRepository.findLastByAccessPoint(accessPoint);
                if (state.getDeleteChange() != null) {
                    throw new IllegalStateException("Zneplatněné osodby není možno upravovat");
                }
                return state;
            }
        }
        return null;
    }

    public void checkParty(ParPartyVO partyVO) {
        final ParPartyType partyType = partyTypeRepository.getOneCheckExist(partyVO.getPartyType().getId());

        // Definice TypeEnum na Class
        partyTypeToClass.forEach((type, clazz) -> partyCheckerHelper(partyVO, partyType, clazz, type));

        List<ApType> apTypes = apTypeRepository.findApTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(apTypes)) {
            throw new ObjectNotFoundException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode(), BaseCode.ID_NOT_EXIST);
        }

        checkPreferredNameExist(partyVO.getPartyNames());
    }

    // TODO: tuto metodu bude vhodne asi dale prozkoumat
    // hlavne ve vztahu k metodam, ktere ji vyuzivaji a zda by
    // rovnou nemela vracet party apod.
    public ApState checkPartyUpdate(final ParPartyVO partyVO) {

        if (partyVO.getId() == null) {
            throw new SystemException("Není vyplněno id existující entity pro update.", BaseCode.ID_NOT_EXIST);
        }

        ParParty parParty = partyRepository.getOneCheckExist(partyVO.getId());
        if (!parParty.getPartyType().getPartyTypeId().equals(partyVO.getPartyType().getId())) {
            throw new IllegalArgumentException("Nelze měnit typ osoby.");
        }

        ApState apState = apStateRepository.findLastByAccessPoint(parParty.getAccessPoint());
        if (apState.getDeleteChange() != null) {
            throw new IllegalStateException("Zneplatněné osoby není možno upravovat");
        }

        ParPartyType partyType = partyTypeRepository.getOneCheckExist(partyVO.getPartyType().getId());

        List<ApType> apTypes = apTypeRepository.findApTypeByPartyType(partyType);
        if (CollectionUtils.isEmpty(apTypes)) {
            throw new ObjectNotFoundException("Nenalezen typ rejstříku příslušející typu osoby s kódem: " + partyType.getCode(), BaseCode.ID_NOT_EXIST);
        }

        if (partyVO.getPartyNames() != null) {
            checkPreferredNameExist(partyVO.getPartyNames());
        }

        return apState;
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
        throw new BusinessException("Není přítomno žádné preferované jméno osoby.", BaseCode.PROPERTY_NOT_EXIST).set("property", "partyName");
    }
}
