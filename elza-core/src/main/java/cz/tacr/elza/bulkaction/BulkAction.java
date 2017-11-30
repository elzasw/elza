package cz.tacr.elza.bulkaction;

import java.util.List;

import javax.transaction.Transactional;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.DescriptionItemService;


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

	protected RuleSystem ruleSystem;

	/**
	 * Změna
	 */
	protected ArrChange getChange() {
		return bulkActionRun.getChange();
	}

	/**
	 * Init method, this method prepare ruleSystem and other fields.
	 * 
	 * Method can be specialized in each implementation.
	 */
	protected void init(ArrBulkActionRun bulkActionRun) {
		this.bulkActionRun = bulkActionRun;

		this.version = fundVersionRepository.findOne(bulkActionRun.getFundVersionId());
		Validate.notNull(version);
		checkVersion(version);

		staticDataProvider = staticDataService.getData();
		ruleSystem = staticDataProvider.getRuleSystems().getByRuleSetId(version.getRuleSetId());
		Validate.notNull(ruleSystem, "Rule system not available, id: {}", version.getRuleSetId());
	}

    /**
     * Abstrakní metoda pro spuštění hromadné akce.
     *
     * @param inputNodeIds      seznam vstupních uzlů (podstromů AS)
     * @param bulkActionConfig  nastavení hromadné akce
     * @param bulkActionRun     stav hromadné akce
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
     * @param version  verze archivní pomůcky
     * @param change   změna
     * @return finální atribut
     */
    protected ArrDescItem saveDescItem(final ArrDescItem descItem,
                                       final ArrFundVersion version,
                                       final ArrChange change) {
        if (descItem.getDescItemObjectId() == null) {
            return descriptionItemService.createDescriptionItem(descItem, descItem.getNode(), version, change);
        } else {
            return descriptionItemService.updateDescriptionItem(descItem, version, change, true);
        }
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
    protected List<ArrLevel> getChildren(final ArrLevel level) {
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
}
