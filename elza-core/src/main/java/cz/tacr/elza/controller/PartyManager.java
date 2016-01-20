package cz.tacr.elza.controller;

import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParPartyTypeExt;
import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.vo.ParPartyWithCount;
import cz.tacr.elza.repository.DataPartyRefRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.PartyCreatorRepository;
import cz.tacr.elza.repository.PartyDynastyRepository;
import cz.tacr.elza.repository.PartyEventRepository;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyGroupRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyPersonRepository;
import cz.tacr.elza.repository.PartyRelationRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.PartyTimeRangeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.RegistryService;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.List;


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
    private PartyNameComplementRepository partyNameComplementRepository;
    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;
    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private PartyCreatorRepository partyCreatorRepository;
    @Autowired
    private PartyTimeRangeRepository partyTimeRangeRepository;
    @Autowired
    private PartyRelationRepository partyRelationRepository;
    @Autowired
    private UnitdateRepository partyUnitdateRepository;
    @Autowired
    private PartyGroupIdentifierRepository partyGroupIdentifierRepository;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private PartyPersonRepository partyPersonRepository;
    @Autowired
    private PartyDynastyRepository partyDynastyRepository;
    @Autowired
    private PartyEventRepository partyEventRepository;
    @Autowired
    private PartyGroupRepository partyGroupRepository;


    //přepsáno do PartyController
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


    //přepsáno do partyController
    @Transactional
    @RequestMapping(value = "/deleteParty", method = RequestMethod.DELETE)
    @Override
    public void deleteParty(@RequestParam("partyId") final Integer partyId) {
        Assert.notNull(partyId);
        ParParty party = partyRepository.findOne(partyId);
        if (party == null) {
            return;
        }

         checkPartyUsage(party);
        ParPartyName partyName = party.getPreferredName();
        if (partyName != null) {
            partyNameComplementRepository.deleteByPartyName(partyName);
            partyNameRepository.delete(partyName);
        }

        partyCreatorRepository.deleteByPartyBoth(party);


        partyTimeRangeRepository.findByParty(party).forEach((pt) -> {
                    ParUnitdate from = pt.getFrom();
                    ParUnitdate to = pt.getTo();
                    partyTimeRangeRepository.delete(pt);
                    deleteUnitDates(from, to);
                }
        );

        partyRelationRepository.findByParty(party).forEach((pr) -> {
                    ParUnitdate from = pr.getFrom();
                    ParUnitdate to = pr.getTo();
                    partyRelationRepository.delete(pr);
                    deleteUnitDates(from, to);
                }
        );

        ParPartyGroup partyGroup = partyGroupRepository.findOne(party.getPartyId());
        if (partyGroup != null) {
            partyGroupIdentifierRepository.findByParty(partyGroup).forEach((pg) -> {
                        ParUnitdate from = pg.getFrom();
                        ParUnitdate to = pg.getTo();
                        partyGroupIdentifierRepository.delete(pg);
                        deleteUnitDates(from, to);
                    }
            );
        }

        if (partyPersonRepository.findOne(party.getPartyId()) != null) {
            partyPersonRepository.delete(party.getPartyId());
        }
        if (partyDynastyRepository.findOne(party.getPartyId()) != null) {
            partyDynastyRepository.delete(party.getPartyId());
        }
        if (partyEventRepository.findOne(party.getPartyId()) != null) {
            partyEventRepository.delete(party.getPartyId());
        }
        if (partyGroupRepository.findOne(party.getPartyId()) != null) {
            partyGroupRepository.delete(party.getPartyId());
        }

        partyRepository.delete(partyId);

        registryService.deleteRecord(party.getRecord(), false);
    }


    //přepsáno do partyService
    /**
     * Promazání dvojice datumů od kterékoliv entity.
     * @param from  od
     * @param to    do
     */
    private void deleteUnitDates(final ParUnitdate from, final ParUnitdate to) {
        if (from != null) {
            partyUnitdateRepository.delete(from);
        }
        if (to != null) {
            partyUnitdateRepository.delete(to);
        }
    }


    //přepsáno do partyService
    /**
     * Prověří existenci vazeb na osobu. Pokud existují, vyhodí příslušnou výjimku, nelze mazat.
     * @param party osoba
     */
    private void checkPartyUsage(final ParParty party) {
        // vazby ( arr_node_register, ArrDataRecordRef, ArrDataPartyRef),
        Long pocet = dataPartyRefRepository.getCountByParty(party.getPartyId());
        if (pocet > 0) {
            throw new IllegalStateException("Nalezeno použití party v tabulce ArrDataPartyRef.");
        }

        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByRecordId(party.getRecord().getRecordId());
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }

        List<ArrNodeRegister> nodeRegisterList = nodeRegisterRepository.findByRecordId(party.getRecord());
        if (CollectionUtils.isNotEmpty(nodeRegisterList)) {
            throw new IllegalStateException("Nalezeno použití hesla v tabulce ArrDataRecordRef.");
        }
    }

    //přepsáno do PartyController
    @RequestMapping(value = "/findParty", method = RequestMethod.GET)
    @Override
    public ParPartyWithCount findParty(@Nullable @RequestParam(value = "search", required = false) final String search,
                                    @RequestParam("from") final Integer from, @RequestParam("count") final Integer count,
                                    @Nullable @RequestParam(value = "partyTypeId", required = false) final Integer partyTypeId,
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

        long countAll = partyRepository.findPartyByTextAndTypeCount(search, partyTypeId, originator);

        return new ParPartyWithCount(resultList, countAll);
    }

    //přepsáno do PartyController
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
