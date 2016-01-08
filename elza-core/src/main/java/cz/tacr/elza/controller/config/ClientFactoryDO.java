package cz.tacr.elza.controller.config;

import cz.tacr.elza.controller.vo.ParDynastyEditVO;
import cz.tacr.elza.controller.vo.ParEventEditVO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyGroupEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeEditVO;
import cz.tacr.elza.controller.vo.ParPersonEditVO;
import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.controller.vo.RegVariantRecordVO;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyTimeRange;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


/**
 * Továrna na vytváření DO objektů z VO objektů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 07.01.2016
 */
@Service
public class ClientFactoryDO {

    @Autowired
    @Qualifier("configVOMapper")
    private MapperFactory mapperFactory;


    /**
     * Vytvoří objekt osoby z předaného VO.
     *
     * @param partyVO VO osoby
     * @return objekt osoby
     */
    public ParParty createParty(final ParPartyEditVO partyVO) {
        if (partyVO == null) {
            return null;
        }

        MapperFacade mapper = mapperFactory.getMapperFacade();

        if (partyVO instanceof ParDynastyEditVO) {
            return mapper.map(partyVO, ParDynasty.class);
        }
        if (partyVO instanceof ParPersonEditVO) {
            return mapper.map(partyVO, ParPerson.class);
        }
        if (partyVO instanceof ParEventEditVO) {
            return mapper.map(partyVO, ParEvent.class);
        }
        if (partyVO instanceof ParPartyGroupEditVO) {
            return mapper.map(partyVO, ParPartyGroup.class);
        }

        return mapper.map(partyVO, ParParty.class);
    }

    public void updateParty(final ParPartyEditVO partyVO, final ParParty origParty) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        mapper.map(partyVO, origParty);
    }

    /**
     * Vytvoří objekt jména osoby. Jsou načteny i detailní informace.
     *
     * @param partyNameVOSave jméno osoby VO
     * @return vo jména osoba
     */
    public ParPartyName createParPartyName(final ParPartyNameEditVO partyNameVOSave) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(partyNameVOSave, ParPartyName.class);
    }

    /**
     * Vytvoří objekt působnosti osoby.
     *
     * @param parPartyTimeRangeEditVO působnost VO
     * @return do působnost
     */
    public ParPartyTimeRange createParPartyTimeRange(final ParPartyTimeRangeEditVO parPartyTimeRangeEditVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(parPartyTimeRangeEditVO, ParPartyTimeRange.class);
    }

    /**
     * Vytvoření rejstříkového hesla.
     *
     * @param regRecordVO VO rejstříkové heslo
     * @return DO rejstříkové heslo
     */
    public RegRecord createRegRecord(final RegRecordVO regRecordVO) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regRecordVO, RegRecord.class);
    }

    /**
     * Vytvoření variantního rejstříkového hesla.
     *
     * @param regVariantRecord VO variantní rejstříkové heslo
     * @return DO variantní rejstříkové heslo
     */
    public RegVariantRecord createRegVariantRecord(final RegVariantRecordVO regVariantRecord) {
        MapperFacade mapper = mapperFactory.getMapperFacade();
        return mapper.map(regVariantRecord, RegVariantRecord.class);
    }

}
