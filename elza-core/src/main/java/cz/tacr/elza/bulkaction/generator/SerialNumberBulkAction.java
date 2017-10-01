package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DescItemRepository;


/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 */
@Component
@Scope("prototype")
public class SerialNumberBulkAction extends BulkAction {

    /**
     * Verze archivní pomůcky
     */
    private ArrFundVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Pomocná třída pro generování pořadových čísel
     */
	final private SerialNumber serialNumber = new SerialNumber();

    /**
     * Typ atributu
     */
    private RulItemType descItemType;

    /**
     * Stav hromadné akce
     */
    private ArrBulkActionRun bulkActionRun;

    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

	@Autowired
	private StaticDataService staticDataService;

	protected final SerialNumberConfig config;

	SerialNumberBulkAction(SerialNumberConfig config) {
		Validate.notNull(config);
		this.config = config;
	}

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
	private void init() {
		StaticDataProvider sdp = staticDataService.getData();
		RuleSystem ruleSystem = sdp.getRuleSystems().getByRuleSetCode(config.getRules());
		Validate.notNull(ruleSystem, "Rule system not available, code:" + config.getRules());

		// prepare item type
		RuleSystemItemType itemType = ruleSystem.getItemTypeByCode(config.getItemType());
		Validate.notNull(itemType);

		descItemType = itemType.getEntity();
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     * @param rootNode
     */
    private void generate(final ArrLevel level, final ArrNode rootNode) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(State.INTERRUPTED);
            throw new BusinessException("Hromadná akce " + toString() + " byla přerušena.", ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
        }
        change = bulkActionRun.getChange();

        // update serial number
        update(level);

        List<ArrLevel> childLevels = getChildren(level);

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel, rootNode);
        }

    }

    /**
     * Update number for given level
     * @param level Level to be updated
     */
    private void update(final ArrLevel level) {
        ArrNode currNode = level.getNode();

        ArrDescItem descItem = loadDescItem(currNode);
        int sn = serialNumber.getNext();

        // vytvoření nového atributu
        if (descItem == null) {
            descItem = new ArrDescItem(new ArrItemInt());
            descItem.setItemType(descItemType);
            descItem.setNode(currNode);
        }

        if (!(descItem.getItem() instanceof ArrItemInt)) {
            throw new BusinessException(descItemType.getCode() + " není typu ArrDescItemInt", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", descItemType.getCode())
                    .set("expected", "ArrItemInt")
                    .set("actual", descItem.getItem().getClass().getSimpleName());
        }

        ArrItemInt item = (ArrItemInt) descItem.getItem();

        // uložit pouze při rozdílu
        if (item.getValue() == null || sn != item.getValue() || BooleanUtils.isNotFalse(descItem.getUndefined())) {
            descItem.setUndefined(false);
            item.setValue(sn);
            ArrDescItem ret = saveDescItem(descItem, version, change);
            level.setNode(ret.getNode());
            countChanges++;
        }
    }

    /**
     * Načtení požadovaného atributu
     *
     * @param node uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrNode node) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndItemTypeId(
                node, descItemType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new SystemException(
                    descItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")",
                    BaseCode.DB_INTEGRITY_PROBLEM);
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    @Override
    @Transactional
	public void run(ActionRunContext runContext) {
		this.bulkActionRun = runContext.getBulkActionRun();
		init();

        ArrFundVersion version = bulkActionRun.getFundVersion();

		Validate.notNull(version);
        checkVersion(version);
		this.version = version;

        ArrNode rootNode = version.getRootNode();

		for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode node = nodeRepository.findOne(nodeId);
            Assert.notNull(nodeId, "Node s nodeId=" + nodeId + " neexistuje");
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull(level, "Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            generate(level, rootNode);
        }

        Result resultBA = new Result();
        SerialNumberResult result = new SerialNumberResult();
        result.setCountChanges(countChanges);
        resultBA.getResults().add(result);
        bulkActionRun.setResult(resultBA);
    }

    /**
     * Generátor pořadových čísel.
     */
    private class SerialNumber {

        private int i;

        public SerialNumber() {
            this.i = 0;
        }

        public int getNext() {
            return ++i;
        }
    }

    @Override
    public String toString() {
        return "SerialNumberBulkAction{" +
                "version=" + version +
                '}';
    }
}
