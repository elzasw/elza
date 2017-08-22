package cz.tacr.elza.core.data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.MapMaker;

import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.utils.HibernateUtils;

@Service
public class StaticDataService {

    private final ConcurrentMap<Transaction, StaticDataProvider> registeredTxMap = new MapMaker().weakKeys().makeMap();

    private final Set<Transaction> modifiedTxSet = new HashSet<>();

    private final ReentrantLock reloadLock = new ReentrantLock();

    private StaticDataProvider modifiedProvider;

    private StaticDataProvider activeProvider;

    /* managed components */

    private final EntityManager em;

    final RuleSetRepository ruleSetRepository;

    final PacketTypeRepository packetTypeRepository;

    final ItemTypeRepository itemTypeRepository;

    final ItemSpecRepository itemSpecRepository;

    final DataTypeRepository dataTypeRepository;

    final CalendarTypeRepository calendarTypeRepository;

    final PartyTypeRepository partyTypeRepository;

    final PackageRepository packageRepository;

    final PartyNameFormTypeRepository partyNameFormTypeRepository;

    final ComplementTypeRepository complementTypeRepository;

    final PartyTypeComplementTypeRepository partyTypeComplementTypeRepository;

    final RegisterTypeRepository registerTypeRepository;

    @Autowired
    public StaticDataService(EntityManager em,
                             RuleSetRepository ruleSetRepository,
                             PacketTypeRepository packetTypeRepository,
                             ItemTypeRepository itemTypeRepository,
                             ItemSpecRepository itemSpecRepository,
                             DataTypeRepository dataTypeRepository,
                             CalendarTypeRepository calendarTypeRepository,
                             PartyTypeRepository partyTypeRepository,
                             PackageRepository packageRepository,
                             PartyNameFormTypeRepository partyNameFormTypeRepository,
                             ComplementTypeRepository complementTypeRepository,
                             PartyTypeComplementTypeRepository partyTypeComplementTypeRepository,
                             RegisterTypeRepository registerTypeRepository) {
        this.em = em;
        this.ruleSetRepository = ruleSetRepository;
        this.packetTypeRepository = packetTypeRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.itemSpecRepository = itemSpecRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.calendarTypeRepository = calendarTypeRepository;
        this.partyTypeRepository = partyTypeRepository;
        this.packageRepository = packageRepository;
        this.partyNameFormTypeRepository = partyNameFormTypeRepository;
        this.complementTypeRepository = complementTypeRepository;
        this.partyTypeComplementTypeRepository = partyTypeComplementTypeRepository;
        this.registerTypeRepository = registerTypeRepository;
    }

    /**
     * Must be called during application startup within transaction. Initialize type enums and
     * static data provider.
     */
    @Transactional(value = TxType.MANDATORY)
    public void init() {
        // init type enums and active provider
        DataType.init(dataTypeRepository);
        PartyType.init(partyTypeRepository);
        CalendarType.init(calendarTypeRepository);
        activeProvider = initializeProvider();
        // init interceptor
        StaticDataTransactionInterceptor.INSTANCE.begin(this);
    }

    public void reloadOnCommit() {
        Transaction tx = getCurrentActiveTransaction();
        reloadOnCommit(tx);
    }

    public void reloadOnCommit(Transaction tx) {
        checkActiveTransaction(tx);
        reloadLock.lock();
        try {
            if (!modifiedTxSet.add(tx)) {
                throw new IllegalStateException("Transaction already registered");
            }
        } finally {
            reloadLock.unlock();
        }
    }

    public StaticDataProvider getData() {
        Transaction tx = getCurrentActiveTransaction();
        return getData(tx);
    }

    public StaticDataProvider getData(Transaction tx) {
        checkActiveTransaction(tx);
        StaticDataProvider provider = registeredTxMap.get(tx);
        if (provider == null) {
            throw new IllegalStateException("Static data provider for transaction not found");
        }
        return provider;
    }

    void registerTransaction(Transaction tx) {
        checkActiveTransaction(tx);
        StaticDataProvider provider;
        reloadLock.lock();
        try {
            // lock for exclusive read of active provider
            Assert.notNull(activeProvider);
            provider = activeProvider;
        } finally {
            reloadLock.unlock();
        }
        if (registeredTxMap.putIfAbsent(tx, provider) != null) {
            tx.markRollbackOnly();
            throw new IllegalStateException("Transaction already registered");
        }
    }

    void beforeTransactionCommit(Transaction tx) {
        checkActiveTransaction(tx);
        reloadLock.lock();
        // check for modified transaction
        if (!modifiedTxSet.contains(tx)) {
            reloadLock.unlock();
            return;
        }
        // prepare modified provider in current transaction
        try {
            Assert.isNull(modifiedProvider);
            modifiedProvider = initializeProvider();
        } catch (Throwable t) {
            tx.markRollbackOnly();
            reloadLock.unlock();
            throw t;
        }
    }

    void unregisterTransaction(Transaction tx) {
        reloadLock.lock();
        try {
            // check for modified transaction
            if (modifiedTxSet.remove(tx)) {
                if (tx.getStatus() == TransactionStatus.COMMITTED) {
                    Assert.notNull(modifiedProvider);
                    activeProvider = modifiedProvider;
                }
                modifiedProvider = null;
            }
        } finally {
            reloadLock.unlock();
            registeredTxMap.remove(tx);
        }
    }

    private Transaction getCurrentActiveTransaction() {
        Session session = HibernateUtils.getCurrentSession(em);
        if (!session.isJoinedToTransaction()) {
            throw new IllegalStateException("Current session doesn't have active transaction");
        }
        return session.getTransaction();
    }

    private StaticDataProvider initializeProvider() {
        StaticDataProvider provider = new StaticDataProvider();
        provider.init(this);
        return provider;
    }

    private static void checkActiveTransaction(Transaction tx) {
        if (!tx.isActive()) {
            throw new IllegalStateException("Inactive transaction");
        }
    }
}
