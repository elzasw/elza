package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.SerialNumberResult;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;


/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 21.10.2015
 */
@Component
@Scope("prototype")
public class SerialNumberBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_SERIAL_NUMBER";

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
    private SerialNumber serialNumber;

    /**
     * Typ atributu
     */
    private RulItemType descItemType;

    /**
     * Stav hromadné akce
     */
    private ArrBulkActionRun bulkActionRun;

    /**
     * Typ atributu pro zastaveni
     */
    private RulItemType descItemStopType;

    /**
     * Specifikace atributu pro zastaveni
     */
    private RulItemSpec descItemStopSpec;

    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {

        Assert.notNull(bulkActionConfig);

        String serialIdCode = (String) bulkActionConfig.getString("serial_id_code");
        Assert.notNull(serialIdCode);

        descItemType = itemTypeRepository.getOneByCode(serialIdCode);
        Assert.notNull(descItemType);

        String stopWhenType = (String) bulkActionConfig.getString("stop_when_type");
        if (stopWhenType != null) {
            descItemStopType = itemTypeRepository.getOneByCode(stopWhenType);
            Assert.notNull(descItemStopType, "Description item not found: " + stopWhenType);

            String stopOnValue = (String) bulkActionConfig.getString(
                        "stop_on_value");
            Assert.notNull(stopOnValue, "stop_on_value is not defined");
            descItemStopSpec = itemSpecRepository.getOneByCode(stopOnValue);
            Assert.notNull(descItemStopSpec, "Specification: "+stopOnValue+" does not exists");
        }
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

        if (descItemStopType != null) {
            ArrDescItem descItemLevel = loadDescItem(level, descItemStopType, descItemStopSpec);
            if (descItemLevel != null) {
                return;
            }
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

    /**
     * Načtení atributu.
     *
     * @param node           uzel
     * @param rulDescItemSpec specifikace atributu
     * @param rulDescItemType typ atributu
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level,
                                     final RulItemType rulDescItemType,
                                     final RulItemSpec rulDescItemSpec) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndItemTypeIdAndSpecItemTypeId(
                        level.getNode(), rulDescItemType.getItemTypeId(), rulDescItemSpec.getItemSpecId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new SystemException(
                    rulDescItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")",
                    BaseCode.DB_INTEGRITY_PROBLEM);
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    @Override
    @Transactional
    public void run(final List<Integer> inputNodeIds,
                    final BulkActionConfig bulkAction,
                    final ArrBulkActionRun bulkActionRun) {
        this.bulkActionRun = bulkActionRun;
        init(bulkAction);

        ArrFundVersion version = bulkActionRun.getFundVersion();

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        this.serialNumber = new SerialNumber();

        ArrNode rootNode = version.getRootNode();

        for (Integer nodeId : inputNodeIds) {
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
