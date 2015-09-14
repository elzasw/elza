package cz.tacr.elza.controller;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Implementace API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/api/partyManager")
public class PartyManager implements cz.tacr.elza.api.controller.PartyManager<ParParty> {

    @Autowired
    private PartyRepository abstractPartyRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;


    @RequestMapping(value = "/getPartyTypes", method = RequestMethod.GET)
    @Override
    public List<ParPartyTypeExt> getPartyTypes() {
        List<ParPartyType> all = partyTypeRepository.findAll();
        List<ParPartySubtype> allSubtype = partySubtypeRepository.findAll();
        Map<ParPartyType, List<ParPartySubtype>> map = new HashMap<>();

        all.forEach((partyType) -> {
            map.put(partyType, new ArrayList<>());
        });

        allSubtype.forEach((partySubtype) -> {
            List<ParPartySubtype> list = map.get(partySubtype.getPartyType());
            list.add(partySubtype);
        });

        List<ParPartyTypeExt> result = new ArrayList<>();
        map.forEach((partyType, subtypes) -> {
            ParPartyTypeExt partyTypeExt = new ParPartyTypeExt();
            BeanUtils.copyProperties(partyType, partyTypeExt);
            partyTypeExt.setPartySubTypeList(subtypes);
            result.add(partyTypeExt);
        });

        return result;
    }

    @Transactional
    private void updateAbstractParty(final ParParty source, final ParParty target) {
        Assert.notNull(source.getPartySubtype(), "Není vyplněné partySubtype");
        Assert.notNull(source.getRecord(), "Není vyplněné record");
        Integer recordId = source.getRecord().getRecordId();
        Integer partySubtypeId = source.getPartySubtype().getPartySubtypeId();
        Assert.notNull(partySubtypeId, "Není vyplněné partySubtypeId");
        Assert.notNull(recordId, "Není vyplněné recordId");

        final ParPartySubtype partySubtype =
                partySubtypeRepository.findOne(partySubtypeId);
        final RegRecord record = recordRepository.findOne(recordId);
        Assert.notNull(partySubtype,
                "Nebyla nalezena ParPartySubtype s id " + partySubtypeId);
        Assert.notNull(record, "Nebyla nalezena RegRecord s id " + recordId);

        target.setPartySubtype(partySubtype);
        target.setRecord(record);
    }

    @RequestMapping(value = "/insertAbstractParty", method = RequestMethod.PUT)
    @Override
    public ParParty insertParty(@RequestBody final ParParty party) {
        ParParty newParty = insertAbstractPartyInternal(party);

        if (newParty.getRecord() != null) {
            newParty.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }
        return newParty;
    }

    @Transactional
    private ParParty insertAbstractPartyInternal(final ParParty abstractParty) {
        ParParty newParty = new ParParty();
        updateAbstractParty(abstractParty, newParty);
        abstractPartyRepository.save(newParty);
        return newParty;
    }

    @RequestMapping(value = "/updateAbstractParty", method = RequestMethod.PUT)
    @Override
    public ParParty updateParty(@RequestBody final ParParty party) {
        updateAbstractPartyInternal(party);
        if (party.getRecord() != null) {
            party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }

        return party;
    }

    @Transactional
    private ParParty updateAbstractPartyInternal(final ParParty abstractParty) {
        Integer abstractPartyId = abstractParty.getPartyId();
        Assert.notNull(abstractPartyId);
        ParParty party = abstractPartyRepository.findOne(abstractPartyId);
        Assert.notNull(party, "Nebyla nalezena ParParty s id " + abstractPartyId);
        updateAbstractParty(abstractParty, abstractParty);
        abstractPartyRepository.save(abstractParty);
        return abstractParty;
    }


    @RequestMapping(value = "/deleteAbstractParty", method = RequestMethod.DELETE)
    @Transactional
    @Override
    public void deleteParty(@RequestParam("abstractPartyId") final Integer partyId) {
        ParParty abstractParty = abstractPartyRepository.findOne(partyId);
        if (abstractParty == null) {
            return;
        }
        Assert.notNull(partyId);
        abstractPartyRepository.delete(partyId);
    }

    @RequestMapping(value = "/findAbstractParty", method = RequestMethod.GET)
    @Override
    public List<ParParty> findParty(@RequestParam("search") final String search,
                                    @RequestParam("from") final Integer from, @RequestParam("count") final Integer count,
                                    @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
                                    @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        List<ParParty> resultList = abstractPartyRepository
                .findAbstractPartyByTextAndType(search, partyTypeId, from, count, originator);
        resultList.forEach((party) -> {
            if (party.getRecord() != null) {
                party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                    variantRecord.setRegRecord(null);
                });
            }
        });
        return resultList;
    }

    @RequestMapping(value = "/findAbstractPartyCount", method = RequestMethod.GET)
    @Override
    public Long findPartyCount(@RequestParam("search") final String search,
                               @RequestParam("partyTypeId") final Integer partyTypeId,
                               @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        return abstractPartyRepository.findAbstractPartyByTextAndTypeCount(search, partyTypeId, originator);
    }

    @RequestMapping(value = "/getAbstractParty", method = RequestMethod.GET)
    @Override
    public ParParty getAbstractParty(
            @RequestParam("abstractPartyId") final Integer partyId) {
        Assert.notNull(partyId);
        ParParty party = abstractPartyRepository.getOne(partyId);
        if (party.getRecord() != null) {
            party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }

        return party;
    }

}
