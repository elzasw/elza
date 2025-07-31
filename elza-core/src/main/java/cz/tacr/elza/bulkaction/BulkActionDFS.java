package cz.tacr.elza.bulkaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.BulkActionCode;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;

/**
 * Abstraktní třída pro tvorbu hromadných akcí.
 */
abstract public class BulkActionDFS implements BulkAction {

	private static final Logger logger = LoggerFactory.getLogger(BulkActionDFS.class);	

	private static final int batchSize = 1000;
	
	@Autowired
	protected ArrangementInternalService arrInternalService;

    @Autowired
    protected LevelRepository levelRepository;

    @Autowired
    protected NodeRepository nodeRepository;

    @Autowired
    protected DescriptionItemService descriptionItemService;

    @Autowired
    protected DescItemRepository descItemRepository;

    @Autowired
    protected DataStructureRefRepository structureRefRepository;
    
    @Autowired
    protected ApplicationContext appCtx;

	@Autowired
	protected StaticDataService staticDataService;

    @Autowired
    protected PlatformTransactionManager tm;

    /**
	 * Static data provider is set in init method
	 */
	protected StaticDataProvider staticDataProvider;

	/**
	 * Stav hromadné akce
	 */
	protected ArrBulkActionRun bulkActionRun;

    /**
     * Optional context for changing multiple items at once
     *
     * This can speed up processing of operation.
     */
    protected MultipleItemChangeContext multipleItemChangeContext = null;

	protected Result result;

	protected ActionRunContext runContext;

	/**
	 * Flag of the interrupted action.
	 */
	protected boolean interrupt = false;

	/**
	 * This is detached object with initialized fund and lockChange
	 */
	private ArrFundVersion fondsVersion;
	
	public ArrFundVersion getFondsVersion() {
		Objects.requireNonNull(fondsVersion);
		return fondsVersion;
	}

	@Override
	public void terminate() {
		interrupt = true;
	}

    @Override
	public void execute(ActionRunContext runContext) throws InterruptedException {

		// Initialize bulk action
		init(runContext.getBulkActionRun());

		// Run action
		run(runContext);
	}

	private void run(ActionRunContext runContext) throws InterruptedException {

        this.runContext = runContext;

		result = new Result();

        for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrLevel level = levelRepository.findByNodeIdAndDeleteChangeIsNull(nodeId);
            Objects.requireNonNull(level);

            run(level);
		}

        new TransactionTemplate(tm).executeWithoutResult(status -> {
			done();
	
			bulkActionRun.setResult(result);
        });
	}

	/**
	 * Generování hodnot - rekurzivní volání pro procházení celého stromu
	 *
	 * @param level
	 * @throws InterruptedException 
	 */
	protected void run(ArrLevel level) throws InterruptedException {
		if (interrupt) {
			throw new InterruptedException("The action was interrupted");
		}

		// get list of IDs - it can be very large
		List<Integer> levelIds = new TransactionTemplate(tm).execute(status -> {
			logger.debug("Getting all ArrLevels from {}", level);
			return levelRepository.findLevelIdsSubtree(level.getNodeId(), 0, 0, false);
        });

		logger.debug("Updating {} levels...", levelIds.size());

		// processing received records in batches
		do {
			int indexTo = levelIds.size() > batchSize ? batchSize : levelIds.size();
			List<Integer> ids = levelIds.subList(0, indexTo);
	        new TransactionTemplate(tm).executeWithoutResult(status -> {
	        	this.arrInternalService.getFundVersionById(this.runContext.getFundVersionId());
	        	
				List<ArrLevel> levels = levelRepository.findAllById(ids);
				Map<Integer, ArrLevel> levelMap = levels.stream().collect(Collectors.toMap(ArrLevel::getLevelId, lvl -> lvl));
				ids.forEach(id -> update(levelMap.get(id)));
				
				// flush all changes inside transaction
		        if (multipleItemChangeContext != null) {
		           	multipleItemChangeContext.flush();
		        }
	        });
			if (interrupt) {
				throw new InterruptedException("The action was interrupted");
			}
	        levelIds.removeAll(ids);
		} while (levelIds.size() > 0);

        logger.debug("All levels updated.");
	}

	/**
     * Return or create multipleChangeContext
     *
     * @return
     */
    public MultipleItemChangeContext getMultipleItemChangeContext() {
        if (multipleItemChangeContext == null) {
            multipleItemChangeContext = descriptionItemService.createChangeContext(runContext.getFundVersionId());
        }
        return multipleItemChangeContext;
    }

    /**
     * Změna
     */
    public ArrChange getChange() {
		return bulkActionRun.getChange();
	}

    public StaticDataProvider getStaticDataProvider() {
        return staticDataProvider;
    }

	/**
	 * Init method, this method prepare ruleSystem and other fields.
	 *
	 * Method can be specialized in each implementation.
	 */
	protected void init(ArrBulkActionRun bulkActionRun) {
		this.bulkActionRun = bulkActionRun;

		this.fondsVersion = this.arrInternalService.getFundVersionById(bulkActionRun.getFundVersionId());
		checkVersion(fondsVersion);

		staticDataProvider = staticDataService.getData();
	}

    /**
     * Prepare exception for incorrect configuration
     *
     * @param message
     * @return
     */
    protected AbstractException createConfigException(String message) {
        return new SystemException(message, BulkActionCode.INCORRECT_CONFIG).set("name", this.getName());
    }

    /**
     * Uložení nového atributu.
     *
     * @param descItem ukládaný atribut
     * @return finální atribut
     */
    public ArrDescItem saveNewDescItem(ArrFundVersion fondsVersion, final ArrDescItem descItem) {
        ArrDescItem result;
    	Validate.isTrue(descItem.getDescItemObjectId() == null);
        if (multipleItemChangeContext == null) {            
        	result = descriptionItemService.createDescriptionItem(descItem, descItem.getNode(), fondsVersion, getChange());
        } else {
        	result = descriptionItemService.createDescriptionItemInBatch(descItem, descItem.getNode(), fondsVersion, getChange(), multipleItemChangeContext);
            multipleItemChangeContext.flushIfNeeded();
        }
        return result;
    }

    /**
     * Uložení existující atributu.
     * 
     * @param descItem new version of item (deattached with data) 
     * @return finální atribut
     */
    public ArrDescItem updateDescItem(final ArrFundVersion version, final ArrDescItem descItem, boolean forceUpdate) {
        ArrDescItem result;
    	Validate.isTrue(descItem.getDescItemObjectId() != null);
        if (multipleItemChangeContext == null) {
        	//result = descriptionItemService.updateDescriptionItem(descItem, version, getChange(), false);
        	throw new SystemException("The functionality is not implemented.");
        } else {
        	result = descriptionItemService.updateValueAsNewVersion(version, getChange(), descItem, multipleItemChangeContext, forceUpdate);
            multipleItemChangeContext.flushIfNeeded();
        }
        return result;
    }

    /**
     * Vyhledá potomky uzlu.
     *
     * @param level rodičovský uzel
     * @return nalezený potomci
     */
    public List<ArrLevel> getChildren(final ArrLevel level) {
        return levelRepository.findByParentNodeAndDeleteChangeIsNullOrderByPositionAsc(level.getNode());
    }

    /**
     * Kontrola verze.
     *
     * @param version verze archivní pomůcky
     */
    protected void checkVersion(ArrFundVersion version) {
    	Objects.requireNonNull(version);
        if (version.getLockChange() != null) {
            throw new BusinessException("Nelze aplikovat na uzavřenou verzi archivní pomůcky", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }

    /**
     * Načtení požadovaného atributu.
     *
     * @param node uzel
     * @return nalezený atribut
     */
    public ArrDescItem loadSingleDescItem(final ArrNode node, RulItemType descItemType) {
        List<ArrDescItem> descItems = descriptionItemService.findByNodeAndDeleteChangeIsNullAndItemTypeId(node, descItemType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new SystemException(
                    descItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")",
                    BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("nodeId", node.getNodeId());
        }
        return descItems.get(0);
    }

    /**
     * Smazání prvků.
     * 
     * @param items
     * @param moveAfter
     */
    public void deleteDescItems(ArrFundVersion fondsVersion, List<ArrDescItem> items, final boolean moveAfter) {
        if (multipleItemChangeContext == null) {
            descriptionItemService.deleteDescriptionItems(items, fondsVersion, getChange(), moveAfter, false);
        } else {
            descriptionItemService.deleteDescriptionItems(items, fondsVersion, getChange(), moveAfter, false, multipleItemChangeContext);
            multipleItemChangeContext.flushIfNeeded();
        }
    }

    /**
     * Smazání prvku.
     * 
     * @param oldDescItem
     */
    public void deleteDescItem(ArrFundVersion fondsVersion, ArrDescItem oldDescItem) {
        List<ArrDescItem> items = Collections.singletonList(oldDescItem);
        deleteDescItems(fondsVersion, items, true);
    }

    /**
     * Aktualizace dat.
     * 
     * @param level
     */
	protected abstract void update(ArrLevel level);

	/**
	 * Příprava výsledku.
	 */
	protected abstract void done();
}
