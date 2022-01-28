package cz.tacr.elza.bulkaction;

import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
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
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.arrangement.MultipleItemChangeContext;

import static cz.tacr.elza.repository.ExceptionThrow.version;


/**
 * Abstraktní třída pro tvorbu hromadných akcí.
 *
 */
public abstract class BulkAction {

    @Autowired
    protected FundVersionRepository fundVersionRepository;

    @Autowired
    protected LevelRepository levelRepository;

    @Autowired
    protected NodeRepository nodeRepository;

    @Autowired
    protected DescriptionItemService descriptionItemService;

    @Autowired
    protected DescItemRepository descItemRepository;

    @Autowired
    protected ApplicationContext appCtx;

	@Autowired
	protected StaticDataService staticDataService;

	/**
	 * Static data provider is set in init method
	 */
	protected StaticDataProvider staticDataProvider;

	/**
	 * Verze archivní pomůcky
	 */
	protected ArrFundVersion version;

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

	/**
	 * Změna
	 */
    public ArrChange getChange() {
		return bulkActionRun.getChange();
	}

    public ArrFundVersion getFundVersion() {
        return version;
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

		this.version = fundVersionRepository.findById(bulkActionRun.getFundVersionId()).orElseThrow(version(bulkActionRun.getFundVersionId()));
		checkVersion(version);

		staticDataProvider = staticDataService.getData();
	}

    /**
     * Prepare exception for incorrect configuration
     * 
     * @param message
     * @return
     */
    protected AbstractException createConfigException(String message) {
        return new SystemException(message, BulkActionCode.INCORRECT_CONFIG)
                .set("name", this.getName());
    }

    /**
     * Abstrakní metoda pro spuštění hromadné akce.
     *
     * @param runContext
     */
	abstract public void run(ActionRunContext runContext);

	/**
	 * Return name of bulkaction
	 *
	 * Value is used to log result, etc.
	 *
	 * @return
	 */
	abstract public String getName();

    /**
     * Uložení nového/existující atributu.
     *
     * @param descItem ukládaný atribut
     * @return finální atribut
     */
    public ArrDescItem saveDescItem(final ArrDescItem descItem) {
        ArrDescItem result;
        if (multipleItemChangeContext == null) {
            if (descItem.getDescItemObjectId() == null) {
                result = descriptionItemService.createDescriptionItem(descItem, descItem.getNode(), version,
                                                                      getChange());
            } else {
                result = descriptionItemService.updateDescriptionItem(descItem, version, getChange());
            }
        } else {
            if (descItem.getDescItemObjectId() == null) {
                result = descriptionItemService.createDescriptionItemInBatch(descItem, descItem.getNode(), version,
                                                                           getChange(), multipleItemChangeContext);
            } else {
                result = descriptionItemService.updateValueAsNewVersion(version, getChange(),
                                                                        descItem,
                                                                        multipleItemChangeContext);
            }
            multipleItemChangeContext.flushIfNeeded();
        }
        return result;
    }

    /**
     * Smazání existujícího atributu.
     *
     * @param descItem atribut ke smazání
     * @param version  verze archivní pomůcky
     * @param change   změna
     * @return finální atribut
     */
    /*protected ArrDescItem deleteDescItem(final ArrDescItem descItem,
                                         final ArrFundVersion version,
                                         final ArrChange change) {
        return descriptionItemService.deleteDescriptionItem(descItem, version, change, true);
    }*/


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
		Validate.notNull(version);
        if (version.getLockChange() != null) {
            throw new BusinessException("Nelze aplikovat na uzavřenou verzi archivní pomůcky", ArrangementCode.VERSION_ALREADY_CLOSED);
        }
    }

	@Transactional
	public void execute(ActionRunContext runContext) {

		// Initialize bulk action
		init(runContext.getBulkActionRun());

		// Run action
		run(runContext);

	}

    /**
     * Načtení požadovaného atributu
     *
     * @param node
     *            uzel
     * @return nalezený atribut
     */
    public ArrDescItem loadSingleDescItem(final ArrNode node, RulItemType descItemType) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndItemTypeId(
                                                                                                      node, descItemType
                                                                                                              .getItemTypeId());
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

    public void deleteDescItem(ArrDescItem oldDescItem) {
        List<ArrDescItem> items = Collections.singletonList(oldDescItem);
        descriptionItemService.deleteDescriptionItems(items, oldDescItem.getNode(), getFundVersion(), getChange(),
                                                      true, false);
    }
}
