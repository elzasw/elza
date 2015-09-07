package cz.tacr.elza.controller;

import cz.tacr.elza.domain.ParAbstractParty;
import cz.tacr.elza.domain.ParAbstractPartyVals;
import cz.tacr.elza.domain.ParPartySubtype;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.AbstractPartyRepository;
import cz.tacr.elza.repository.PartySubtypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;


/**
 * API pro práci s pravidly.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 30. 7. 2015
 */
@RestController
@RequestMapping("/api/partyManager")
public class PartyManager implements cz.tacr.elza.api.controller.PartyManager<ParAbstractPartyVals> {

    @Autowired
    private AbstractPartyRepository abstractPartyRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private PartySubtypeRepository partySubtypeRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;

    @RequestMapping(value = "/getPartyTypes", method = RequestMethod.GET)
    @Override
    public List<ParPartyType> getPartyTypes() {
        return partyTypeRepository.findAll();
    }

    @RequestMapping(value = "/insertAbstractParty", method = RequestMethod.PUT)
    @Transactional
    @Override
    public ParAbstractParty insertAbstractParty(@RequestBody final ParAbstractPartyVals abstractParty) {
        Assert.notNull(abstractParty.getPartySubtypeId(), "Není vyplněné partySubtypeId");
        Assert.notNull(abstractParty.getRecordId(), "Není vyplněné recordId");

        final ParPartySubtype partySubtype =
                partySubtypeRepository.findOne(abstractParty.getPartySubtypeId());
        final RegRecord record = recordRepository.findOne(abstractParty.getRecordId());
        Assert.notNull(partySubtype,
                "Nebyla nalezena ParPartySubtype s id " + abstractParty.getPartySubtypeId());
        Assert.notNull(record, "Nebyla nalezena RegRecord s id " + abstractParty.getRecordId());

        ParAbstractParty party = new ParAbstractParty();
        party.setPartySubtype(partySubtype);
        party.setRecord(record);
        abstractPartyRepository.save(party);
        return party;
    }

    @RequestMapping(value = "/updateAbstractParty", method = RequestMethod.PUT)
    @Transactional
    @Override
    public ParAbstractParty updateAbstractParty(
            @RequestParam("abstractPartyId") final Integer abstractPartyId,
            @RequestBody final ParAbstractPartyVals abstractParty) {
        Assert.notNull(abstractPartyId);
        Assert.notNull(abstractParty.getPartySubtypeId(), "Není vyplněné partySubtypeId");
        Assert.notNull(abstractParty.getRecordId(), "Není vyplněné recordId");
        final ParPartySubtype partySubtype =
                partySubtypeRepository.findOne(abstractParty.getPartySubtypeId());
        final RegRecord record = recordRepository.findOne(abstractParty.getRecordId());
        Assert.notNull(partySubtype,
                "Nebyla nalezena ParPartySubtype s id " + abstractParty.getPartySubtypeId());
        Assert.notNull(record, "Nebyla nalezena RegRecord s id " + abstractParty.getRecordId());

        ParAbstractParty party = abstractPartyRepository.findOne(abstractPartyId);
        Assert.notNull(party, "Nebyla nalezena ParAbstractParty s id " + abstractPartyId);
        party.setPartySubtype(partySubtype);
        party.setRecord(record);
        abstractPartyRepository.save(party);
        return party;
    }

    @RequestMapping(value = "/deleteAbstractParty", method = RequestMethod.DELETE)
    @Transactional
    @Override
    public void deleteAbstractParty(@RequestParam("abstractPartyId") final Integer abstractPartyId) {
        Assert.notNull(abstractPartyId);
        abstractPartyRepository.delete(abstractPartyId);
    }

    @RequestMapping(value = "/findAbstractParty", method = RequestMethod.GET)
    @Override
    public List<ParAbstractParty> findAbstractParty(@RequestParam("search") final String search,
            @RequestParam("from") final Integer from, @RequestParam("count") final Integer count,
            @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
            @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        List<ParAbstractParty> resultList = abstractPartyRepository
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
    public Long findAbstractPartyCount(@RequestParam("search") final String search,
            @RequestParam("partyTypeId") final Integer partyTypeId,
            @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        return abstractPartyRepository.findAbstractPartyByTextAndTypeCount(search, partyTypeId, originator);
    }

    @RequestMapping(value = "/getAbstractParty", method = RequestMethod.GET)
    @Override
    public ParAbstractParty getAbstractParty(
            @RequestParam("abstractPartyId") final Integer abstractPartyId) {
        Assert.notNull(abstractPartyId);
        return abstractPartyRepository.getOne(abstractPartyId);
    }
}
