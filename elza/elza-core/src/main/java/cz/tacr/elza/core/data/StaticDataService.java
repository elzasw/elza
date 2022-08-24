package cz.tacr.elza.core.data;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.repository.ApExternalIdTypeRepository;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.repository.ApTypeRepository;
import cz.tacr.elza.repository.ArrangementExtensionRepository;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.DataTypeRepository;
import cz.tacr.elza.repository.ExtensionRuleRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.ItemTypeSpecAssignRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.PartTypeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import cz.tacr.elza.repository.SysLanguageRepository;

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
     */
    private final Set<Transaction> modificationTransactions = new HashSet<>();

    private StaticDataProvider activeProvider;

    @Value("${version:0.0.0}")
    private String appVersion;

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

    final PackageRepository packageRepository;

    final ApTypeRepository apTypeRepository;

    final ApExternalIdTypeRepository apEidTypeRepository;

    final SysLanguageRepository sysLanguageRepository;

    final PartTypeRepository partTypeRepository;

    final ApExternalSystemRepository apExternalSystemRepository;

    final StructuredTypeExtensionRepository structuredTypeExtensionRepository;

    final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    final ArrangementExtensionRepository ruleSetExtRepository;

    final ExtensionRuleRepository extensionRuleRepository;

    final ArrangementRuleRepository arrangementRuleRepository;

    final ComponentRepository componentRepository;

    final PolicyTypeRepository policyTypeRepository;

    @Autowired
    public StaticDataService(final EntityManager em,
                             final RuleSetRepository ruleSetRepository,
                             final ArrangementRuleRepository arrangementRuleRepository,
                             final ArrangementExtensionRepository ruleSetExtRepository,
                             final ExtensionRuleRepository extensionRuleRepository,
                             final ItemTypeRepository itemTypeRepository,
                             final ItemSpecRepository itemSpecRepository,
                             final ItemTypeSpecAssignRepository itemTypeSpecAssignRepository,
                             final DataTypeRepository dataTypeRepository,
                             final PackageRepository packageRepository,
                             final StructuredTypeRepository structuredTypeRepository,
                             final StructureDefinitionRepository structureDefinitionRepository,
                             final StructuredTypeExtensionRepository structuredTypeExtensionRepository,
                             final StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
                             final ApTypeRepository apTypeRepository,
                             final ApExternalIdTypeRepository apEidTypeRepository,
                             final SysLanguageRepository sysLanguageRepository,
                             final PartTypeRepository partTypeRepository,
                             final ApExternalSystemRepository apExternalSystemRepository,
                             final ComponentRepository componentRepository,
                             final PolicyTypeRepository policyTypeRepository) {
        this.em = em;
        this.ruleSetRepository = ruleSetRepository;
        this.arrangementRuleRepository = arrangementRuleRepository;
        this.ruleSetExtRepository = ruleSetExtRepository;
        this.extensionRuleRepository = extensionRuleRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.itemSpecRepository = itemSpecRepository;
        this.itemTypeSpecAssignRepository = itemTypeSpecAssignRepository;
        this.dataTypeRepository = dataTypeRepository;
        this.packageRepository = packageRepository;
        this.structuredTypeRepository = structuredTypeRepository;
        this.structureDefinitionRepository = structureDefinitionRepository;
        this.structuredTypeExtensionRepository = structuredTypeExtensionRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.apTypeRepository = apTypeRepository;
        this.apEidTypeRepository = apEidTypeRepository;
        this.sysLanguageRepository = sysLanguageRepository;
        this.partTypeRepository = partTypeRepository;
        this.apExternalSystemRepository = apExternalSystemRepository;
        this.componentRepository = componentRepository;
        this.policyTypeRepository = policyTypeRepository;
    }

    /**
     * Must be called during application startup within transaction. Initialize type enums and
     * static data provider.
     */
    @Transactional(value = TxType.MANDATORY)
    public void init() {
        if (activeProvider == null) {
            // init type enums
            // enums from DB are initialized only once
            DataType.init(dataTypeRepository);
        }
        // prepare active provider
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

    public String getAppVersion() {
        return appVersion;
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
