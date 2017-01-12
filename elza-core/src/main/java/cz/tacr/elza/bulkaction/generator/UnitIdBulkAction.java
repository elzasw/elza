package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.bulkaction.generator.result.UnitIdResult;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemUnitid;
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
 * @since 21.10.2015
 */
public class UnitIdBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_UNIT_ID";

    /**
     * Verze archivní pomůcky
     */
    private ArrFundVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Typ atributu
     */
    private RulItemType descItemType;

    /**
     * Typ atributu levelu
     */
    private RulItemType descItemLevelType;

    /**
     * Typ atributu pro předchozí uložení
     */
    private RulItemType descItemPreviousType;

    /**
     * Specifikace atributu pro předchozí uložení
     */
    private RulItemSpec descItemPreviousSpec;

    /**
     * Vedlejší oddělovač
     */
    private String delimiterMinor;

    /**
     * Hlavní oddělovač
     */
    private String delimiterMajor;

    /**
     * Seznam kódů typů uzlů při které se nepoužije major oddělovač.
     */
    private List<String> delimiterMajorLevelTypeNotUseList;

    /**
     * Stav hromadné akce
     */
    private ArrBulkActionRun bulkActionRun;

    /**
     * Počet změněných položek.
     */
    private Integer countChanges = 0;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {

        Assert.notNull(bulkActionConfig);

        String unitIdCode = (String) bulkActionConfig.getString("unit_id_code");
        Assert.notNull(unitIdCode);

        descItemType = itemTypeRepository.getOneByCode(unitIdCode);
        Assert.notNull(descItemType);

        String levelTypeCode = (String) bulkActionConfig.getString("level_type_code");
        Assert.notNull(levelTypeCode);

        descItemLevelType = itemTypeRepository.getOneByCode(levelTypeCode);
        Assert.notNull(descItemLevelType);

        String delimiterMajor = (String) bulkActionConfig.getString("delimiter_major");
        Assert.notNull(delimiterMajor);
        this.delimiterMajor = delimiterMajor;

        String delimiterMinor = (String) bulkActionConfig.getString("delimiter_minor");
        Assert.notNull(delimiterMinor);
        this.delimiterMinor = delimiterMinor;

        String previousIdCode = (String) bulkActionConfig.getString("previous_id_code");
        descItemPreviousType = itemTypeRepository.getOneByCode(previousIdCode);
        Assert.notNull(descItemPreviousType);

        String previousIdSpecCode = (String) bulkActionConfig.getString("previous_id_spec_code");
        descItemPreviousSpec = itemSpecRepository.getOneByCode(previousIdSpecCode);
        Assert.notNull(descItemPreviousSpec);

        String delimiterMajorLevelTypeNotUse = (String) bulkActionConfig
                .getString("delimiter_major_level_type_not_use");
        if (delimiterMajorLevelTypeNotUse == null) {
            delimiterMajorLevelTypeNotUseList = new ArrayList<>();
        } else {
            delimiterMajorLevelTypeNotUseList = Arrays.asList(delimiterMajorLevelTypeNotUse.split("\\|"));
        }
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *  @param level          uzel
     * @param rootNode
     * @param unitId         generátor pořadových čísel
     * @param parentSpecCode specifický kód rodiče
     */
    private void generate(final ArrLevel level, final ArrNode rootNode, UnitId unitId, final String parentSpecCode) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(State.INTERRUPTED);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }
        change = bulkActionRun.getChange();

        ArrDescItem descItemLevel = loadDescItemLevel(level);
        if (!level.getNode().equals(rootNode)) {

            if (level.getNodeParent() != null) {

                ArrDescItem descItem = loadDescItem(level);

                if (unitId == null) {
                    unitId = new UnitId(1);
                } else {
                    String specCode = descItemLevel == null ? null : descItemLevel.getItemSpec().getCode();

                    if ((specCode == null && parentSpecCode == null)
                            || (specCode != null && specCode.equals(parentSpecCode))
                            || (parentSpecCode != null && parentSpecCode.equals(specCode))
                            || (delimiterMajorLevelTypeNotUseList.contains(specCode))) {
                        unitId.setSeparator(delimiterMinor);
                    } else {
                        unitId.setSeparator(delimiterMajor);
                    }

                    unitId.genNext();
                }

                // vytvoření nového atributu
                if (descItem == null) {
                    descItem = new ArrDescItem(new ArrItemUnitid());
                    descItem.setItemType(descItemType);
                    descItem.setNode(level.getNode());
                }

                ArrItemData item = descItem.getItem();

                if (!(item instanceof ArrItemUnitid)) {
                    throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
                }

                // uložit pouze při rozdílu
                if (((ArrItemUnitid) item).getValue() == null || !unitId.getData()
                        .equals(((ArrItemUnitid) item).getValue())) {

                    ArrDescItem ret;

                    // uložit původní hodnotu pouze při první změně z předchozí verze
                    if (descItem.getDescItemObjectId() != null && descItem.getCreateChange().getChangeId() < version
                            .getCreateChange().getChangeId()) {
                        ArrDescItem descItemPrev = new ArrDescItem(descItemFactory.createItemByType(descItemPreviousType.getDataType()));
                        ArrItemData itemPrev = descItemPrev.getItem();
                        descItemPrev.setItemType(descItemPreviousType);
                        descItemPrev.setItemSpec(descItemPreviousSpec);
                        descItemPrev.setNode(level.getNode());

                        if (itemPrev instanceof ArrItemString) {
                            ((ArrItemString) itemPrev).setValue(((ArrItemUnitid) itemPrev).getValue());
                        } else {
                            throw new IllegalStateException(
                                    descItemPrev.getClass().getName() + " nema definovany prevod hodnoty");
                        }

                        ret = saveDescItem(descItemPrev, version, change);
                        level.setNode(ret.getNode());

                    }

                    ((ArrItemUnitid) item).setValue(unitId.getData());
                    ret = saveDescItem(descItem, version, change);
                    level.setNode(ret.getNode());
                    countChanges++;
                }

            }
        }

        List<ArrLevel> childLevels = getChildren(level);

        if (unitId == null) {
            unitId = new UnitId("");
            unitId.setSeparator("");
        }

        UnitId unitIdChild = null;
        for (ArrLevel childLevel : childLevels) {
            if (unitId != null && unitIdChild == null) {
                unitIdChild = unitId.getClone();
            }
            generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
        }

    }

    /**
     * Načtení atributu.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndItemTypeId(level.getNode(), descItemType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    /**
     * Načtení atributu - level.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItemLevel(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndItemTypeId(level.getNode(),
                        descItemLevelType.getItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuze byt vice nez jeden (" + descItems.size() + ")");
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

        ArrFundVersion version = fundVersionRepository.findOne(bulkActionRun.getFundVersionId());

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        ArrNode rootNode = version.getRootNode();

        for (Integer nodeId : inputNodeIds) {
            ArrNode node = nodeRepository.findOne(nodeId);
            Assert.notNull("Node s nodeId=" + nodeId + " neexistuje");
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull("Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            ArrDescItem descItem = loadDescItem(level);
            ArrDescItem descItemLevel = loadDescItemLevel(level);
            if (descItem != null) {
                ArrItemData item = descItem.getItem();

                if (!(item instanceof ArrItemUnitid)) {
                    throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
                }

                List<ArrLevel> childLevels = getChildren(level);

                String value = ((ArrItemUnitid) item).getValue();
                UnitId unitId = new UnitId(value);
                unitId.setSeparator("");

                UnitId unitIdChild = null;
                for (ArrLevel childLevel : childLevels) {
                    if (unitId != null && unitIdChild == null) {
                        unitIdChild = unitId.getClone();
                    }
                    generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
                }

            } else if(node.equals(rootNode)) {
                generate(level, rootNode, null, descItemLevel == null ? null : descItemLevel.getItemSpec().getCode());
            }

        }

        Result resultBA = new Result();
        UnitIdResult result = new UnitIdResult();
        result.setCountChanges(countChanges);
        resultBA.getResults().add(result);
        bulkActionRun.setResult(resultBA);
    }

    /**
     * Generátor pořadových čísel.
     */
    private class UnitId {

        String data;
        Integer id = null;
        String separator = null;

        public UnitId(final Integer id) {
            this.id = id;
            this.data = "";
            this.separator = "";
        }

        private UnitId(final String data) {
            this.data = data;
        }

        public String getData() {
            String tmp;
            if (this.data.equals("")) {
                tmp = (id == null ? "" : id.toString());
            } else {
                tmp = this.data + separator + (id == null ? "" : id.toString());
            }
            return tmp;
        }

        public UnitId getClone() {
            return new UnitId(getData());
        }

        public void setSeparator(final String separator) {
            this.separator = separator;
        }

        public void genNext() {
            if (id == null) {
                id = 1;
            } else {
                id++;
            }
        }
    }

    @Override
    public String toString() {
        return "UnitIdBulkAction{" +
                "version=" + version +
                '}';
    }
}