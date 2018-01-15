package cz.tacr.elza.core.data;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.MapMaker;

import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ComplementTypeRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartyNameFormTypeRepository;
import cz.tacr.elza.repository.PartyTypeComplementTypeRepository;
import cz.tacr.elza.repository.PartyTypeRepository;
import cz.tacr.elza.repository.RegisterTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.StructureTypeRepository;
import cz.tacr.elza.utils.HibernateUtils;

/**
 * Service for static data
 *
 * Service returns StaticDataProvider with valid data for given transaction
 */
@Service
public class StaticDataService {

	/**
	 * Map of providers for each transaction
	 */
    private final ConcurrentMap<Transaction, StaticDataProvider> registeredTxMap = new MapMaker().weakKeys().makeMap();

    /**
     * Set of transactions which should refresh StaticDataProvider after commit
     *
     */
    private final Set<Transaction> modificationTransactions = new HashSet<>();

    private StaticDataProvider activeProvider;

    /* managed components */

    private final EntityManager em;

    final RuleSetRepository ruleSetRepository;

    final ItemTypeRepository itemTypeRepository;

    final ItemSpecRepository itemSpecRepository;

    final StructureTypeRepository structuredTypeRepository;

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
                             ItemTypeRepository itemTypeRepository,
                             ItemSpecRepository itemSpecRepository,
                             DataTypeRepository dataTypeRepository,
                             CalendarTypeRepository calendarTypeRepository,
                             PartyTypeRepository partyTypeRepository,
                             PackageRepository packageRepository,
                             StructureTypeRepository structuredTypeRepository,
                             PartyNameFormTypeRepository partyNameFormTypeRepository,
                             ComplementTypeRepository complementTypeRepository,
                             PartyTypeComplementTypeRepository partyTypeComplementTypeRepository,
                             RegisterTypeRepository registerTypeRepository) {
        this.em = em;
        this.ruleSetRepository = ruleSetRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.itemSpecRepository = itemSpecRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.calendarTypeRepository = calendarTypeRepository;
        this.partyTypeRepository = partyTypeRepository;
        this.packageRepository = packageRepository;
        this.structuredTypeRepository = structuredTypeRepository;
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

    /**
     * Switch data provider for current transaction
     */
    public StaticDataProvider reloadForCurrentTransaction() {
    	Transaction tx = getCurrentActiveTransaction();
    	StaticDataProvider provider = initializeProvider();
    	// update data provider for transaction
   		registeredTxMap.put(tx, provider);
    	return provider;
    }

    public void reloadOnCommit() {
        Transaction tx = getCurrentActiveTransaction();
        reloadOnCommit(tx);
    }

    public void reloadOnCommit(Transaction tx) {
        checkActiveTransaction(tx);

        synchronized(modificationTransactions)
        {
        	modificationTransactions.add(tx);
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

    /**
     * Called when new transaction if registered
     * @param tx
     */
    void registerTransaction(Transaction tx) {
        checkActiveTransaction(tx);

        StaticDataProvider provider = activeProvider;
        // some provider have to exists
        Validate.notNull(provider);

        if (registeredTxMap.putIfAbsent(tx, provider) != null) {
            tx.markRollbackOnly();
            throw new IllegalStateException("Transaction already registered");
        }
    }

    void beforeTransactionCommit(Transaction tx) {
        checkActiveTransaction(tx);

        synchronized(modificationTransactions)
        {
        	// check for modified transaction
        	if (!modificationTransactions.contains(tx)) {
        		return;
        	}
        	modificationTransactions.remove(tx);
        }

        // prepare modified provider in current transaction
        try {
        	StaticDataProvider modifiedProvider = initializeProvider();

        	tx.registerSynchronization(new Synchronization(){
					@Override
					public void beforeCompletion() {
						// nop
					}

					@Override
					public void afterCompletion(int status) {
						// set new provider if committed
						if(status==javax.transaction.Status.STATUS_COMMITTED) {
							activeProvider = modifiedProvider;
						}
					}
        		});
        } catch (Throwable t) {
        	tx.markRollbackOnly();
        	throw t;
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
