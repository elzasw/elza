package cz.tacr.elza.controller;

import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
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
    private PartyRepository partyRepository;
    @Autowired
    private RegRecordRepository recordRepository;
    @Autowired
    private PartyTypeRepository partyTypeRepository;
    @Autowired
    private PartyNameRepository partyNameRepository;
    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;


    @RequestMapping(value = "/getPartyTypes", method = RequestMethod.GET)
    @Override
    public List<ParPartyTypeExt> getPartyTypes() {
        List<ParPartyType> all = partyTypeRepository.findAll();

        List<ParPartyTypeExt> result = new ArrayList<>();
        all.forEach((partyType) -> {
            ParPartyTypeExt partyTypeExt = new ParPartyTypeExt();
            BeanUtils.copyProperties(partyType, partyTypeExt);
            result.add(partyTypeExt);
        });

        return result;
    }

    @Transactional
    private void updateParty(final ParParty source, final ParParty target) {
        Assert.notNull(source.getPartyType(), "Není vyplněné partyType");
        Assert.notNull(source.getRecord(), "Není vyplněné record");
        Integer recordId = source.getRecord().getRecordId();
        Integer partyTypeId = source.getPartyType().getPartyTypeId();
        ParPartyName preferredName = source.getPreferredName();
        
        Assert.notNull(partyTypeId, "Není vyplněné partyTypeId");
        Assert.notNull(recordId, "Není vyplněné recordId");

        final ParPartyType partyType =
                partyTypeRepository.findOne(partyTypeId);
        final RegRecord record = recordRepository.findOne(recordId);

        ParPartyName newPreferredName = null;
        if (preferredName != null) {
            Integer preferredNameId = preferredName.getPartyNameId();
            if (preferredNameId != null) {
                preferredName = partyNameRepository.findOne(preferredNameId);
                Assert.notNull(preferredName, "Nebylo nalezeno preferredName s id " + preferredNameId);
            } else {
                newPreferredName = preferredName;
                partyNameRepository.save(preferredName);
            }
        } else {
            throw new IllegalArgumentException("Není vyplněné  preferredName.");
        }

        Assert.notNull(partyType,
                "Nebyla nalezena ParPartyType s id " + partyTypeId);
        Assert.notNull(record, "Nebyla nalezena RegRecord s id " + recordId);

        target.setPartyType(partyType);
        target.setPreferredName(preferredName);
        target.setRecord(record);
        partyRepository.save(target);
        if (newPreferredName != null) {
            newPreferredName.setParty(target);
            partyNameRepository.save(newPreferredName);
        }
    }

    @RequestMapping(value = "/insertParty", method = RequestMethod.PUT)
    @Override
    public ParParty insertParty(@RequestBody final ParParty party) {
        ParParty newParty = insertPartyInternal(party);

        if (newParty.getRecord() != null) {
            newParty.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }
        if (newParty.getPreferredName() != null) {
            newParty.getPreferredName().setParty(null);
        }
        return newParty;
    }

    @Transactional
    private ParParty insertPartyInternal(final ParParty party) {
        ParParty newParty = new ParParty();
        updateParty(party, newParty);
        return newParty;
    }

    @RequestMapping(value = "/updateParty", method = RequestMethod.PUT)
    @Override
    public ParParty updateParty(@RequestBody final ParParty party) {
        updatePartyInternal(party);
        if (party.getRecord() != null) {
            party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }
        if (party.getPreferredName() != null) {
            party.setPreferredName(null);
        }

        return party;
    }

    @Transactional
    private ParParty updatePartyInternal(final ParParty party) {
        Integer partyId = party.getPartyId();
        Assert.notNull(partyId);
        ParParty checkParty = partyRepository.findOne(partyId);
        Assert.notNull(checkParty, "Nebyla nalezena ParParty s id " + partyId);
        updateParty(party, party);
        return party;
    }


    @RequestMapping(value = "/deleteParty", method = RequestMethod.DELETE)
    @Transactional
    @Override
    public void deleteParty(@RequestParam("partyId") final Integer partyId) {
        Assert.notNull(partyId);
        ParParty party = partyRepository.findOne(partyId);
        if (party == null) {
            return;
        }

        checkPartyUsage(party);
        ParPartyName partyName = party.getPreferredName();
        partyName.setParty(null);
        partyNameRepository.save(partyName);
        partyRepository.delete(partyId);
        partyNameRepository.delete(partyName);
    }

    private void checkPartyUsage(ParParty party) {
        // vazby ( arr_node_register, ArrDataRecordRef, ArrDataPartyRef),
        Long pocet = dataPartyRefRepository.getCountByParty(party.getPartyId());
        if (pocet > 0) {
            throw new IllegalStateException("Nalezeno použití party v tabulce ArrDataPartyRef.");
        }
    }

    @RequestMapping(value = "/findParty", method = RequestMethod.GET)
    @Override
    public List<ParParty> findParty(@RequestParam("search") final String search,
                                    @RequestParam("from") final Integer from, @RequestParam("count") final Integer count,
                                    @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
                                    @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        List<ParParty> resultList = partyRepository
                .findPartyByTextAndType(search, partyTypeId, from, count, originator);
        resultList.forEach((party) -> {
            if (party.getRecord() != null) {
                party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                    variantRecord.setRegRecord(null);
                });
            }
            if (party.getPreferredName() != null) {
                party.setPreferredName(null);
            }
        });
        return resultList;
    }

    @RequestMapping(value = "/findPartyCount", method = RequestMethod.GET)
    @Override
    public Long findPartyCount(@RequestParam("search") final String search,
                               @RequestParam("partyTypeId") final Integer partyTypeId,
                               @Nullable @RequestParam(value = "originator", required = false) final Boolean originator) {

        return partyRepository.findPartyByTextAndTypeCount(search, partyTypeId, originator);
    }

    @RequestMapping(value = "/getParty", method = RequestMethod.GET)
    @Override
    public ParParty getParty(
            @RequestParam("partyId") final Integer partyId) {
        Assert.notNull(partyId);
        ParParty party = partyRepository.getOne(partyId);
        if (party.getRecord() != null) {
            party.getRecord().getVariantRecordList().forEach((variantRecord) -> {
                variantRecord.setRegRecord(null);
            });
        }
        if (party.getPreferredName() != null) {
            party.setPreferredName(null);
        }

        return party;
    }

}
