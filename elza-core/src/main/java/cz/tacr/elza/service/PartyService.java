package cz.tacr.elza.service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.vo.ParPartyEditVO;
import cz.tacr.elza.controller.vo.ParPartyNameEditVO;
import cz.tacr.elza.controller.vo.ParUnitdateEditVO;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.*;


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
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private ClientFactoryDO factoryDO;

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

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private RelationRepository relationRepository;

    @Autowired
    private RelationEntityRepository relationEntityRepository;

    @Autowired
    private IEventNotificationService eventNotificationService;

    @Autowired
    private RelationRoleTypeRepository relationRoleTypeRepository;

    @Autowired
    private DataPartyRefRepository dataPartyRefRepository;
    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private NodeRegisterRepository nodeRegisterRepository;
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;
    @Autowired
    private PartyCreatorRepository partyCreatorRepository;
    @Autowired
    private PartyRelationRepository partyRelationRepository;
    @Autowired
    private PartyGroupIdentifierRepository partyGroupIdentifierRepository;
    @Autowired
    private RegistryService registryService;

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
     * Najde id osob podle rejstříkových hesel.
     *
     * @param records seznam rejstříkových hesel
     *
     * @return mapa id rejstříku -> id osoby
     */
    public Map<Integer, Integer> findParPartyIdsByRecords(final Collection<RegRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.EMPTY_MAP;
        }

        List<Object[]> recordIdsAndPartyIds = partyRepository.findRecordIdAndPartyIdByRecords(records);
        Map<Integer, Integer> recordIdPartyIdMap = new HashMap<>(recordIdsAndPartyIds.size());
        for (Object[] row : recordIdsAndPartyIds) {
            recordIdPartyIdMap.put((Integer) row[0], (Integer) row[1]);
        }

        return recordIdPartyIdMap;
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
        ParPartyName preferredName = insertPartyNames(partyVO, party);

        Assert.notNull(preferredName);
        party.setPreferredName(preferredName);
        ParParty result = partyRepository.save(party);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.PARTY_CREATE, result.getPartyId()));
        return result;
    }

    public ParParty updateParty(final ParPartyEditVO partyVO, final ParPartyType partyType) {
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

        Assert.notNull(preferredName);
        origParty.setPreferredName(preferredName);

        eventNotificationService.publishEvent(EventFactory.createIdEvent(EventType.PARTY_UPDATE, origParty.getPartyId()));

        return partyRepository.save(origParty);
    }

    /**
     * Provede smazání osoby a navázaných entit.
     * @param party osoba ke smazání
     */
    public void deleteParty(final ParParty party){
        Assert.notNull(party);

        checkPartyUsage(party);

        List<ParPartyName> partyNames = new ArrayList<>(partyNameRepository.findByParty(party));
        ParPartyName partyName = party.getPreferredName();
        if (partyName != null) {
            partyNames.add(partyName);
        }

        party.setPreferredName(null);
        for (ParPartyName parPartyName : partyNames) {
            partyNameComplementRepository.deleteByPartyName(parPartyName);
            partyNameRepository.flush();

            deleteUnitDates(parPartyName.getValidFrom(), parPartyName.getValidTo());
            partyNameRepository.delete(parPartyName);
        }

        partyCreatorRepository.deleteByPartyBoth(party);

        partyRelationRepository.findByParty(party).forEach((pr) -> {
                    deleteRelation(pr);
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

        partyRepository.flush();

        partyRepository.delete(party);
        registryService.deleteRecord(party.getRecord(), false);
    }


    /**
     * Provede uložení vztahu a jeho vazeb.
     *
     * @param relationSource   zdrojový objakt vztahu
     * @param relationEntities seznam vazeb vztahu (Pokud je null, nedojde k aktualizaci vazeb)
     * @return uložený objekt
     */
    public ParRelation saveRelation(final ParRelation relationSource,
                                    @Nullable final Collection<ParRelationEntity> relationEntities) {


        Assert.notNull(relationSource.getComplementType());
        Assert.notNull(relationSource.getComplementType().getRelationTypeId());

        Set<ParUnitdate> unitdateRemove = new HashSet<>();

        ParRelationType relationType = relationTypeRepository
                .findOne(relationSource.getComplementType().getRelationTypeId());


        ParRelation relation;
        if (relationSource.getRelationId() == null) {
            relation = relationSource;

            relation.setFrom(saveUnitDate(relationSource.getFrom()));
            relation.setTo(saveUnitDate(relationSource.getTo()));

        } else {
            relation = relationRepository.findOne(relationSource.getRelationId());


            Integer fromId = relation.getFrom() == null ? null : relation.getFrom().getUnitdateId();
            Integer toId = relation.getTo() == null ? null : relation.getTo().getUnitdateId();

            //pokud měl nastavenou dataci a nyní došlo k jejímu smazání, bude původní datace smazána
            if (fromId != null && (relationSource.getFrom() == null || relationSource.getFrom().getUnitdateId() == null
                    || !relationSource.getFrom().getUnitdateId().equals(fromId))) {
                unitdateRemove.add(relation.getFrom());
            }
            relation.setFrom(saveUnitDate(relationSource.getFrom()));

            //pokud měl nastavenou dataci a nyní došlo k jejímu smazání, bude původní datace smazána
            if (toId != null && (relationSource.getTo() == null || relationSource.getTo().getUnitdateId() == null
                    || !relationSource.getTo().getUnitdateId().equals(toId))) {
                unitdateRemove.add(relation.getTo());
            }

            relation.setTo(saveUnitDate(relationSource.getTo()));
        }


        ParParty party = partyRepository.findOne(relationSource.getParty().getPartyId());
        Assert.notNull(party);

        relation.setParty(party);
        relation.setComplementType(relationType);
        relation.setDateNote(relationSource.getDateNote());
        relation.setNote(relationSource.getNote());

        ParRelation result = relationRepository.save(relation);
        relationRepository.flush();

        saveDeleteRelationEntities(result, relationEntities);


        for (ParUnitdate unitdate : unitdateRemove) {
            unitdateRepository.delete(unitdate);
        }

        return result;
    }

    public ParUnitdate saveUnitDate(@Nullable final ParUnitdate unitdateSource) {
        if(unitdateSource == null){
            return null;
        }


        ParUnitdate unitdate;

        Integer calendarTypeId =
                unitdateSource.getCalendarType() == null || unitdateSource.getCalendarType().getCalendarTypeId() == null
                ? null : unitdateSource.getCalendarType().getCalendarTypeId();


        if (unitdateSource.getUnitdateId() == null) {
            unitdate = unitdateSource;

        } else {
            unitdate = unitdateRepository.findOne(unitdateSource.getUnitdateId());

            unitdate.setValueFrom(unitdateSource.getValueFrom());
            unitdate.setValueFromEstimated(unitdateSource.getValueFromEstimated());
            unitdate.setValueTo(unitdateSource.getValueTo());
            unitdate.setValueToEstimated(unitdateSource.getValueToEstimated());
            unitdate.setFormat(unitdateSource.getFormat());
            unitdate.setTextDate(unitdateSource.getTextDate());
        }

        if (calendarTypeId != null) {
            ArrCalendarType calendarType = calendarTypeRepository
                    .findOne(unitdateSource.getCalendarType().getCalendarTypeId());
            unitdate.setCalendarType(calendarType);
        }

        return unitdateRepository.save(unitdate);
    }


    public void deleteRelation(final ParRelation relation) {


        ParUnitdate from = relation.getFrom();
        ParUnitdate to = relation.getTo();

        List<ParRelationEntity> relationEntities = relationEntityRepository.findByRelation(relation);
        if (!relationEntities.isEmpty()) {
            relationEntityRepository.delete(relationEntities);
        }

        relationRepository.delete(relation);

        deleteUnitDates(from, to);
    }


    /**
     * Provede nastavení stavu vazeb u vztahu. Dojde k vytvoření, aktualizaci a smazání přebytečných vazeb.
     *
     * @param relation            vztah
     * @param newRelationEntities seznam vazeb vztahu (pokud je null, nedojde k žádné změně)
     * @return nový seznam vazeb
     */
    private List<ParRelationEntity> saveDeleteRelationEntities(final ParRelation relation,
                                                               @Nullable final Collection<ParRelationEntity> newRelationEntities) {
        if (newRelationEntities == null) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, ParRelationEntity> relationEntityMap = ElzaTools
                .createEntityMap(relationEntityRepository.findByRelation(relation), (r) -> r.getRelationEntityId());

        Set<ParRelationEntity> toRemoveEntities = new HashSet<>(relationEntityMap.values());

        List<ParRelationEntity> result = new ArrayList<>(newRelationEntities.size());

        for (ParRelationEntity newRelationEntity : newRelationEntities) {
            ParRelationEntity saveEntity;
            if (newRelationEntity.getRelationEntityId() == null) {
                saveEntity = newRelationEntity;

            } else {
                saveEntity = relationEntityMap.get(newRelationEntity.getRelationEntityId());
                Assert.notNull(saveEntity,
                        "Nebyla nalezena entita vztahu s id " + newRelationEntity.getRelationEntityId());
                toRemoveEntities.remove(saveEntity);

                saveEntity.setSource(newRelationEntity.getSource());
            }


            Assert.notNull(newRelationEntity.getRoleType());
            Assert.notNull(newRelationEntity.getRoleType().getRoleTypeId());
            ParRelationRoleType relationRoleType = relationRoleTypeRepository
                    .findOne(newRelationEntity.getRoleType().getRoleTypeId());
            saveEntity.setRoleType(relationRoleType);

            Assert.notNull(newRelationEntity.getRecord());
            Assert.notNull(newRelationEntity.getRecord().getRecordId());
            RegRecord record = recordRepository.findOne(newRelationEntity.getRecord().getRecordId());
            saveEntity.setRecord(record);

            saveEntity.setRelation(relation);

            result.add(relationEntityRepository.save(saveEntity));
        }

        if (!toRemoveEntities.isEmpty()) {
            relationEntityRepository.delete(toRemoveEntities);
        }

        return result;
    }


    private ParPartyName insertPartyNames(final ParPartyEditVO partyVO, final ParParty parParty) {

        ParPartyName preferredName = null;

        for (final ParPartyNameEditVO partyNameVO : partyVO.getPartyNames()) {

            ParPartyName partyName = factoryDO.createParPartyName(partyNameVO);

            // nf type
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository.getOne(partyNameVO.getNameFormTypeId());
            partyName.setNameFormType(nameFormType);
            partyName.setParty(parParty);

            // unitdates
            ParUnitdateEditVO validFromVO = partyNameVO.getValidFrom();
            saveValidFrom(validFromVO, partyName);
            ParUnitdateEditVO validToVO = partyNameVO.getValidTo();
            saveValidTo(validToVO, partyName);

            partyNameRepository.save(partyName);

            if (partyNameVO.isPreferredName()) {
                preferredName = partyName;
            }
        }

        return preferredName;
    }

    private ParPartyName updatePartyNames(final ParPartyEditVO partyVO, final ParParty parParty) {

        List<ParPartyName> namesToDelete = partyNameRepository.findByParty(parParty);

        ParPartyName preferredName = null;
        for (final ParPartyNameEditVO partyNameVO : partyVO.getPartyNames()) {

            ParPartyName partyName = partyNameRepository.getOne(partyNameVO.getPartyNameId());

            // nf type
            ParPartyNameFormType nameFormType = partyNameFormTypeRepository.getOne(partyNameVO.getNameFormTypeId());
            partyName.setNameFormType(nameFormType);
            partyName.setParty(parParty);

            // UNITDATES
            ParUnitdateEditVO validFromVO = partyNameVO.getValidFrom();
            // nedal ho, chce smazat
            ParUnitdate validFrom = partyName.getValidFrom();
            if (validFrom != null && validFrom.getUnitdateId() != null
                    && (validFromVO == null || validFromVO.getUnitdateId() == null)) {
                partyName.setValidFrom(null);
                partyNameRepository.save(partyName);
                unitdateRepository.delete(validFrom);
            }
            saveValidFrom(validFromVO, partyName);

            ParUnitdateEditVO validToVO = partyNameVO.getValidTo();
            // nedal ho, chce smazat
            ParUnitdate validTo = partyName.getValidTo();
            if (validTo != null && validTo.getUnitdateId() != null
                    && (validToVO == null || validToVO.getUnitdateId() == null)) {
                partyName.setValidTo(null);
                partyNameRepository.save(partyName);
                unitdateRepository.delete(validTo);
            }
            saveValidTo(validToVO, partyName);

            partyNameRepository.save(partyName);

            if (partyNameVO.isPreferredName()) {
                preferredName = partyName;
            }

            // remove from deletion
            namesToDelete.remove(partyName);
        }

        partyNameRepository.deleteInBatch(namesToDelete);

        return preferredName;
    }

    private void saveValidFrom(final ParUnitdateEditVO validFromVO, final ParPartyName partyName) {
        if (validFromVO != null) {
            ArrCalendarType calendarType = findCalendarType(validFromVO);
            // mapovana mapperem
            partyName.getValidFrom().setCalendarType(calendarType);
            unitdateRepository.save(partyName.getValidFrom());
        }
    }

    private void saveValidTo(final ParUnitdateEditVO validToVO, final ParPartyName partyName) {
        if (validToVO != null) {
            ArrCalendarType calendarType = findCalendarType(validToVO);
            // mapovana mapperem
            partyName.getValidTo().setCalendarType(calendarType);
            unitdateRepository.save(partyName.getValidTo());
        }
    }

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
                regRecord.setLocal(false);

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

            case GROUP_PARTY:
                return partyGroupRepository.getOne(partyId);

            case PERSON:
                return partyPersonRepository.getOne(partyId);

            default:
                return partyRepository.getOne(partyId);
        }
    }

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

    /**
     * Promazání dvojice datumů od kterékoliv entity.
     * @param from  od
     * @param to    do
     */
    private void deleteUnitDates(final ParUnitdate from, final ParUnitdate to) {
        if (from != null) {
            unitdateRepository.delete(from);
        }
        if (to != null) {
            unitdateRepository.delete(to);
        }
    }
}
