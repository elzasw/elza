package cz.tacr.elza.core.data;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import cz.tacr.elza.repository.*;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;

/**
 * Service for static data
 *
 * Service returns StaticDataProvider with valid data for given transaction
 */
@Service
public class StaticDataService {

	/**
	 * Thread specific providers
	 */
    private final ThreadLocal<StaticDataProvider> threadSpecificProvider = new ThreadLocal<StaticDataProvider>();

    /**
     * Set of transactions which should refresh StaticDataProvider after commit
     *
     */
    private final Set<Transaction> modificationTransactions = new HashSet<>();

    private StaticDataProvider activeProvider;

    /* managed components */

    private final EntityManager em;

    /* repository with package visibility for initialization */

    final RuleSetRepository ruleSetRepository;

    final ItemTypeRepository itemTypeRepository;

    final ItemSpecRepository itemSpecRepository;

    final ItemTypeSpecAssignRepository itemTypeSpecAssignRepository;

    final StructuredTypeRepository structuredTypeRepository;

    final StructureDefinitionRepository structureDefinitionRepository;

    final DataTypeRepository dataTypeRepository;

    final CalendarTypeRepository calendarTypeRepository;

    final PackageRepository packageRepository;

    final ApTypeRepository apTypeRepository;

    final ApExternalIdTypeRepository apEidTypeRepository;

    final SysLanguageRepository sysLanguageRepository;

    final RegistryRoleRepository registryRoleRepository;

    final PartTypeRepository partTypeRepository;

    @Autowired
    public StaticDataService(final EntityManager em,
                             final RuleSetRepository ruleSetRepository,
                             final ItemTypeRepository itemTypeRepository,
                             final ItemSpecRepository itemSpecRepository,
                             final ItemTypeSpecAssignRepository itemTypeSpecAssignRepository,
                             final DataTypeRepository dataTypeRepository,
                             final CalendarTypeRepository calendarTypeRepository,
                             final PackageRepository packageRepository,
                             final StructuredTypeRepository structuredTypeRepository,
                             final StructureDefinitionRepository structureDefinitionRepository,
                             final ApTypeRepository apTypeRepository,
                             final ApExternalIdTypeRepository apEidTypeRepository,
                             final SysLanguageRepository sysLanguageRepository,
                             final RegistryRoleRepository registryRoleRepository,
                             final PartTypeRepository partTypeRepository) {
        this.em = em;
        this.ruleSetRepository = ruleSetRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.itemSpecRepository = itemSpecRepository;
        this.itemTypeSpecAssignRepository = itemTypeSpecAssignRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.calendarTypeRepository = calendarTypeRepository;
        this.packageRepository = packageRepository;
        this.structuredTypeRepository = structuredTypeRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.apTypeRepository = apTypeRepository;
        this.apEidTypeRepository = apEidTypeRepository;
        this.sysLanguageRepository = sysLanguageRepository;
        this.registryRoleRepository = registryRoleRepository;
        this.partTypeRepository = partTypeRepository;
    }

    /**
     * Must be called during application startup within transaction. Initialize type enums and
     * static data provider.
     */
    @Transactional(value = TxType.MANDATORY)
    public void init() {
        // init type enums and active provider
        DataType.init(dataTypeRepository);
        CalendarType.init(calendarTypeRepository);
        activeProvider = createProvider();

        // init interceptor
        StaticDataTransactionInterceptor.INSTANCE.begin(this);

        // register provider for current transaction/thread
        threadSpecificProvider.set(activeProvider);
    }

    /**
     * Switch data provider for current transaction
     */
    public StaticDataProvider refreshForCurrentThread() {
        StaticDataProvider provider = activeProvider;
        // update data provider for thread
        threadSpecificProvider.set(provider);
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
        StaticDataProvider provider = threadSpecificProvider.get();
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

        threadSpecificProvider.set(provider);
        }

    void beforeTransactionCommit(Transaction tx) {
        //checkActiveTransaction(tx);

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
            StaticDataProvider modifiedProvider = createProvider();

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

    /**
     * Create new provider and load all data from DB
     *
     * @return
     */
    public StaticDataProvider createProvider() {
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
