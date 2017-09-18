package cz.tacr.elza.deimport.storage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.aps.context.APGeoLocationWrapper;
import cz.tacr.elza.deimport.aps.context.APVariantNameWrapper;
import cz.tacr.elza.deimport.aps.context.AccessPointWrapper;
import cz.tacr.elza.deimport.institutions.context.InstitutionWrapper;
import cz.tacr.elza.deimport.parties.context.PartyAccessPointWrapper;
import cz.tacr.elza.deimport.parties.context.PartyGroupIdentifierWrapper;
import cz.tacr.elza.deimport.parties.context.PartyNameComplementWrapper;
import cz.tacr.elza.deimport.parties.context.PartyNameWrapper;
import cz.tacr.elza.deimport.parties.context.PartyPreferredNameWrapper;
import cz.tacr.elza.deimport.parties.context.PartyUnitDateWrapper;
import cz.tacr.elza.deimport.parties.context.PartyWrapper;
import cz.tacr.elza.deimport.sections.context.ArrDataWrapper;
import cz.tacr.elza.deimport.sections.context.ArrDescItemWrapper;
import cz.tacr.elza.deimport.sections.context.ArrLevelWrapper;
import cz.tacr.elza.deimport.sections.context.ArrNodeRegisterWrapper;
import cz.tacr.elza.deimport.sections.context.ArrNodeWrapper;
import cz.tacr.elza.deimport.sections.context.ArrPacketWrapper;
import cz.tacr.elza.repository.PartyGroupIdentifierRepository;
import cz.tacr.elza.repository.PartyNameComplementRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.repository.UnitdateRepository;
import cz.tacr.elza.service.ArrangementService;

/**
 * Storage manager for all imported items. Must be initialized with active session.
 */
public class StorageManager implements StorageListener {

    private final List<Object> persistEntities = new LinkedList<>();

    private final Session session;

    private final long memoryScoreLimit;

    private final RegRecordStorage recordStorage;

    private final ParPartyStorage partyStorage;

    private long currentMemoryScore;

    public StorageManager(Session session,
                          long memoryScoreLimit,
                          RegRecordRepository recordRepository,
                          ArrangementService arrangementService,
                          RegCoordinatesRepository coordinatesRepository,
                          RegVariantRecordRepository variantRecordRepository,
                          PartyRepository partyRepository,
                          PartyNameRepository nameRepository,
                          PartyNameComplementRepository nameComplementRepository,
                          PartyGroupIdentifierRepository groupIdentifierRepository,
                          UnitdateRepository unitdateRepository) {
        this.session = session;
        this.memoryScoreLimit = memoryScoreLimit;

        this.recordStorage = new RegRecordStorage(session, this, LocalDateTime.now(), recordRepository, arrangementService,
                coordinatesRepository, variantRecordRepository);
        this.partyStorage = new ParPartyStorage(session, this, partyRepository, nameRepository, nameComplementRepository,
                groupIdentifierRepository, unitdateRepository);
    }

    /**
     * Clear all stored entities from persistent context. Unflushed changes to the entity will not
     * be synchronized with the database.
     */
    public void clear() {
        for (Object entity : persistEntities) {
            session.evict(entity);
        }
        persistEntities.clear();
        currentMemoryScore = 0;
    }

    @Override
    public void onEntityPersist(EntityWrapper item, Object entity) {
        Validate.notNull(item);
        Validate.notNull(entity);

        persistEntities.add(entity);
        // estimate memory score
        long memoryScore = 1;
        if (item instanceof EntityMetrics) {
            memoryScore = ((EntityMetrics) item).getMemoryScore();
        }
        // flush & clear when overflow the limit
        currentMemoryScore += memoryScore;
        if (currentMemoryScore > memoryScoreLimit) {
            session.flush();
            clear();
        }
    }

    public void saveAccessPoints(Collection<AccessPointWrapper> items) {
        recordStorage.save(items);
        session.flush();
    }

    public void saveAPVariantNames(Collection<APVariantNameWrapper> items) {
        saveEntities(items);
    }

    public void saveAPGeoLocations(Collection<APGeoLocationWrapper> items) {
        saveEntities(items);
    }

    public void saveParties(Collection<PartyWrapper> items) {
        partyStorage.save(items);
        session.flush();
    }

    public void savePartyUnitDates(Collection<PartyUnitDateWrapper> items) {
        saveEntities(items);
    }

    public void savePartyGroupIdentifiers(Collection<PartyGroupIdentifierWrapper> items) {
        saveEntities(items);
    }

    public void savePartyNames(Collection<PartyNameWrapper> items) {
        saveEntities(items);
    }

    public void savePartyNameComplements(Collection<PartyNameComplementWrapper> items) {
        saveEntities(items);
    }

    public void savePartyPreferredNames(Collection<PartyPreferredNameWrapper> items) {
        saveEntities(items);
    }

    public void saveInstitutions(Collection<InstitutionWrapper> items) {
        saveEntities(items);
    }

    public void savePartyAccessPoints(Collection<PartyAccessPointWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionPackets(Collection<ArrPacketWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionNodes(Collection<ArrNodeWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionNodeRegistry(Collection<ArrNodeRegisterWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionLevels(Collection<ArrLevelWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionDescItems(Collection<ArrDescItemWrapper> items) {
        saveEntities(items);
    }

    public void saveSectionData(Collection<ArrDataWrapper> items) {
        saveEntities(items);
    }

    /**
     * Stores all items and flushes persistent context.
     */
    private <T extends EntityWrapper> void saveEntities(Collection<T> items) {
        if (items.isEmpty()) {
            return;
        }
        EntityStorage<T> storage = new EntityStorage<>(session, this);
        storage.save(items);
        session.flush();
    }
}
