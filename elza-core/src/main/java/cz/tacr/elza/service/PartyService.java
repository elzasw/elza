package cz.tacr.elza.service;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParPartyTimeRangeEditVO;
import cz.tacr.elza.controller.vo.ParUnitdateEditVO;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameFormType;
import cz.tacr.elza.domain.ParPartyTimeRange;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.PartyDynastyRepository;
import cz.tacr.elza.repository.PartyEventRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyPersonRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTimeRangeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.repository.VariantRecordRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Servisní třídy pro osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
@Service
public class PartyService {

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private VariantRecordRepository variantRecordRepository;

    @Autowired
    private PartyTimeRangeRepository partyTimeRangeRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private RegisterTypeRepository registerTypeRepository;

    @Autowired
    private PartyNameFormTypeRepository partyNameFormTypeRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;

    @Autowired
    private UnitdateRepository unitdateRepository;

    @Autowired
    private PartyDynastyRepository partyDynastyRepository;

    @Autowired
    private PartyEventRepository partyEventRepository;

    @Autowired
    private PartyGroupRepository partyGroupRepository;

    @Autowired
    private PartyPersonRepository partyPersonRepository;


    /**
     * Najde osobu podle rejstříkového hesla.
     *
     * @param record rejstříkové heslo
     * @return osoba s daným rejstříkovým heslem nebo null
     */
    public ParParty findParPartyByRecord(final RegRecord record) {
        Assert.notNull(record);


        List<ParParty> recordParties = partyRepository.findParPartyByRecordId(record.getRecordId());
        return recordParties.isEmpty() ? null : recordParties.get(0);
    }

    /**
     * Najde seznam osob podle rejstříkových hesel.
     *
     * @param records seznam rejstříkových hesel
     * @return seznam osob s danými hesly
     */
    public List<ParParty> findParPartyByRecords(final Collection<RegRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.EMPTY_LIST;
        }

        Set<Integer> recordIds = new HashSet<>();
        records.forEach(r -> recordIds.add(r.getRecordId()));

        return partyRepository.findParPartyByRecordIds(recordIds);
    }

    public ParParty createParty(final ParPartyEditVO partyVO, final ParPartyType partyType, final RegRegisterType registerType) {
        ParParty party = factoryDO.createParty(partyVO);
        party.setPartyType(partyType);

        // Record
        RegRecord regRecord = genRegRecordFromPartyNames(partyVO.getPartyNames(), registerType);
        genRegVariantRecordsFromPartyNames(partyVO.getPartyNames(), regRecord);
        party.setRecord(regRecord);

        // Party
        partyRepository.save(party);

        // Names
        ParPartyName preferredName = createPartyNames(partyVO, party);

        // TimeRanges
        if (CollectionUtils.isNotEmpty(partyVO.getTimeRanges())) {
            createTimeRanges(partyVO.getTimeRanges(), party);
        }

        Assert.notNull(preferredName);
        party.setPreferredName(preferredName);
        return partyRepository.save(party);
    }

    public ParParty updateParty(final ParPartyEditVO partyVO, final ParPartyType partyType, final RegRegisterType registerType) {
        ParParty origParty = getPartyByType(partyVO.getPartyId(), partyType);

        factoryDO.updateParty(partyVO, origParty);

        // Record
        List<ParPartyNameEditVO> partyNames = partyVO.getPartyNames();
        if (partyNames != null) {
            // pokud nějaké máme, nagenerujeme nové, chování je tedy: null - beze změny, plná - update přegenerováním
            RegRecord origRecord = origParty.getRecord();
            updateRegRecordFromPartyNames(partyNames, origRecord);

            variantRecordRepository.deleteInBatch(origRecord.getVariantRecordList());
            genRegVariantRecordsFromPartyNames(partyNames, origRecord);
        }

        // Party
        partyRepository.save(origParty);

        // Names
        ParPartyName preferredName = updatePartyNames(partyVO, origParty);

        // TimeRanges
        if (CollectionUtils.isNotEmpty(partyVO.getTimeRanges())) {
            createTimeRanges(partyVO.getTimeRanges(), origParty);
        }

        Assert.notNull(preferredName);
        origParty.setPreferredName(preferredName);
        return partyRepository.save(origParty);
    }

    private ParPartyName createPartyNames(final ParPartyEditVO partyVO, final ParParty party) {
        ParPartyName preferredName = null;
        for (final ParPartyNameEditVO partyNameVO : partyVO.getPartyNames()) {
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository.getOne(partyNameVO.getNameFormTypeId());

            ParPartyName partyName = factoryDO.createParPartyName(partyNameVO);
            partyName.setNameFormType(nameFormType);
            partyName.setParty(party);

            // unitdates
            ParUnitdateEditVO validFrom = partyNameVO.getValidFrom();
            if (validFrom != null) {
                ArrCalendarType calendarType = findCalendarType(validFrom);

                partyName.getValidFrom().setCalendarType(calendarType);
                unitdateRepository.save(partyName.getValidFrom());
            }
            ParUnitdateEditVO validTo = partyNameVO.getValidTo();
            if (validTo != null) {
                ArrCalendarType calendarType = findCalendarType(validTo);

                partyName.getValidTo().setCalendarType(calendarType);
                unitdateRepository.save(partyName.getValidTo());
            }

            partyNameRepository.save(partyName);

            if (partyNameVO.isPreferredName()) {
                preferredName = partyName;
            }
        }

        return preferredName;
    }

    private ParPartyName updatePartyNames(final ParPartyEditVO partyVO, final ParParty origParty) {

        List<ParPartyName> origNames = partyNameRepository.findByParty(origParty);

        ParPartyName preferredName = null;
        for (final ParPartyNameEditVO partyNameVO : partyVO.getPartyNames()) {

            //TODO kuzel muze chtit i nove
            ParPartyName origPartyName = partyNameRepository.getOne(partyNameVO.getPartyNameId());
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository.getOne(partyNameVO.getNameFormTypeId());

            factoryDO.updateParPartyName(partyNameVO, origPartyName);
            origPartyName.setNameFormType(nameFormType);

            partyNameRepository.save(origPartyName);

            if (partyNameVO.isPreferredName()) {
                preferredName = origPartyName;
            }

            origNames.remove(origPartyName);
        }

        partyNameRepository.deleteInBatch(origNames);

        return preferredName;
    }

    private void createTimeRanges(final List<ParPartyTimeRangeEditVO> timeRanges, final ParParty party) {
        timeRanges.forEach(tr -> {
            ParPartyTimeRange partyTimeRange = factoryDO.createParPartyTimeRange(tr);

            // unitdates
            ParUnitdateEditVO fromVO = tr.getFrom();
            if (fromVO != null) {
                ArrCalendarType calendarType = findCalendarType(fromVO);

                partyTimeRange.getFrom().setCalendarType(calendarType);
                unitdateRepository.save(partyTimeRange.getFrom());
            }
            ParUnitdateEditVO toVO = tr.getTo();
            if (toVO != null) {
                ArrCalendarType calendarType = findCalendarType(toVO);

                partyTimeRange.getTo().setCalendarType(calendarType);
                unitdateRepository.save(partyTimeRange.getTo());
            }

            partyTimeRange.setParty(party);
            partyTimeRangeRepository.save(partyTimeRange);
        });
    }

//    private void updateTimeRanges(final List<ParPartyTimeRangeEditVO> timeRanges, final ParParty party) {
//        timeRanges.forEach(tr -> {
//            if (tr.getPartyTimeRangeId() != null) {
//
//            }
//
//
////            partyTimeRangeRepository.
////                    //TODO kuzel muze chtit i nove
////
////                    ParPartyTimeRange partyTimeRange = factoryDO.updateParPartyTimeRange(tr);
//
//            // unitdates
//            ParUnitdateEditVO fromVO = tr.getFrom();
//            if (fromVO != null) {
//                ArrCalendarType calendarType = findCalendarType(fromVO);
//
//                partyTimeRange.getFrom().setCalendarType(calendarType);
//                unitdateRepository.save(partyTimeRange.getFrom());
//            }
//            ParUnitdateEditVO toVO = tr.getTo();
//            if (toVO != null) {
//                ArrCalendarType calendarType = findCalendarType(toVO);
//
//                partyTimeRange.getTo().setCalendarType(calendarType);
//                unitdateRepository.save(partyTimeRange.getTo());
//            }
//
//            partyTimeRange.setParty(party);
//            partyTimeRangeRepository.save(partyTimeRange);
//        });
//    }


    /**
     * Nalezne typ kalendáře dle jeho VO.
     * @return typ kalenddáře DO
     */
    private ArrCalendarType findCalendarType(final ParUnitdateEditVO fromVO) {
        Integer calendarTypeId = fromVO.getCalendarTypeId();
        if (calendarTypeId != null) {
            return calendarTypeRepository.findOne(calendarTypeId);
        }

        return null;
    }

    /**
     * Nageneruje rejstříkové heslo dle preferovaného jména osoby. Ostatní jména jako variantní hesla k tomuto.
     * @param partyNamesVO    jména osob
     * @param registerType    typ rejstříku
     * @return      rejstříkoví heslo s variantními hesly daného typu
     */
    private RegRecord genRegRecordFromPartyNames(final List<ParPartyNameEditVO> partyNamesVO,
                                                 final RegRegisterType registerType) {
        if (partyNamesVO == null) {
            return null;
        }

        RegRecord result = null;
        for (final ParPartyNameEditVO pn : partyNamesVO) {
            if (pn.isPreferredName()) {
                RegRecord regRecord = new RegRecord();
                regRecord.setRegisterType(registerType);
                regRecord.setRecord(pn.getMainPart() + StringUtils.defaultString(pn.getOtherPart()));
                regRecord.setLocal(true);

                recordRepository.save(regRecord);
                result = regRecord;
            }
        }

        return result;
    }

    private RegRecord updateRegRecordFromPartyNames(final List<ParPartyNameEditVO> partyNamesVO, final RegRecord regRecord) {
        if (partyNamesVO == null) {
            return null;
        }

        RegRecord result = null;
        for (final ParPartyNameEditVO pn : partyNamesVO) {
            if (pn.isPreferredName()) {
                regRecord.setRecord(pn.getMainPart() + StringUtils.defaultString(pn.getOtherPart()));

                recordRepository.save(regRecord);
                result = regRecord;
            }
        }

        return result;
    }

    private List<RegVariantRecord> genRegVariantRecordsFromPartyNames(final List<ParPartyNameEditVO> partyNamesVO,
                                                         final RegRecord regRecord) {
        if (partyNamesVO == null) {
            return null;
        }

        List<RegVariantRecord> result = new ArrayList<>();

        for (final ParPartyNameEditVO pn : partyNamesVO) {
            if (!pn.isPreferredName()) {
                RegVariantRecord regVariantRecord = new RegVariantRecord();
                regVariantRecord.setRegRecord(regRecord);
                regVariantRecord.setRecord(pn.getMainPart() + StringUtils.defaultString(pn.getOtherPart()));

                result.add(variantRecordRepository.save(regVariantRecord));
            }
        }

        return result;
    }

    private ParParty getPartyByType(final Integer partyId, final ParPartyType partyType) {
        switch (partyType.getPartyTypeEnum()) {
            case DYNASTY:
                return partyDynastyRepository.getOne(partyId);

            case EVENT:
                return partyEventRepository.getOne(partyId);

            case PARTY_GROUP:
                return partyGroupRepository.getOne(partyId);

            case PERSON:
                return partyPersonRepository.getOne(partyId);

            default:
                return partyRepository.getOne(partyId);
        }
    }

}
