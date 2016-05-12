package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.api.ArrBulkActionRun.State;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemString;
import cz.tacr.elza.domain.ArrDescItemUnitid;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private RulDescItemType descItemType;

    /**
     * Typ atributu levelu
     */
    private RulDescItemType descItemLevelType;

    /**
     * Typ atributu pro předchozí uložení
     */
    private RulDescItemType descItemPreviousType;

    /**
     * Specifikace atributu pro předchozí uložení
     */
    private RulDescItemSpec descItemPreviousSpec;

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

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

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

        String unitIdCode = (String) bulkActionConfig.getProperty("unit_id_code");
        Assert.notNull(unitIdCode);

        descItemType = descItemTypeRepository.getOneByCode(unitIdCode);
        Assert.notNull(descItemType);

        String levelTypeCode = (String) bulkActionConfig.getProperty("level_type_code");
        Assert.notNull(levelTypeCode);

        descItemLevelType = descItemTypeRepository.getOneByCode(levelTypeCode);
        Assert.notNull(descItemLevelType);

        String delimiterMajor = (String) bulkActionConfig.getProperty("delimiter_major");
        Assert.notNull(delimiterMajor);
        this.delimiterMajor = delimiterMajor;

        String delimiterMinor = (String) bulkActionConfig.getProperty("delimiter_minor");
        Assert.notNull(delimiterMinor);
        this.delimiterMinor = delimiterMinor;

        String previousIdCode = (String) bulkActionConfig.getProperty("previous_id_code");
        descItemPreviousType = descItemTypeRepository.getOneByCode(previousIdCode);
        Assert.notNull(descItemPreviousType);

        String previousIdSpecCode = (String) bulkActionConfig.getProperty("previous_id_spec_code");
        descItemPreviousSpec = descItemSpecRepository.getOneByCode(previousIdSpecCode);
        Assert.notNull(descItemPreviousSpec);

        String delimiterMajorLevelTypeNotUse = (String) bulkActionConfig
                .getProperty("delimiter_major_level_type_not_use");
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
                    String specCode = descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode();

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
                    descItem = new ArrDescItemUnitid();
                    descItem.setDescItemType(descItemType);
                    descItem.setNode(level.getNode());
                }

                if (!(descItem instanceof ArrDescItemUnitid)) {
                    throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
                }

                // uložit pouze při rozdílu
                if (((ArrDescItemUnitid) descItem).getValue() == null || !unitId.getData()
                        .equals(((ArrDescItemUnitid) descItem).getValue())) {

                    ArrDescItem ret;

                    // uložit původní hodnotu pouze při první změně z předchozí verze
                    if (descItem.getDescItemObjectId() != null && descItem.getCreateChange().getChangeId() < version
                            .getCreateChange().getChangeId()) {
                        ArrDescItem descItemPrev = descItemFactory.createDescItemByType(descItemPreviousType.getDataType());
                        descItemPrev.setDescItemType(descItemPreviousType);
                        descItemPrev.setDescItemSpec(descItemPreviousSpec);
                        descItemPrev.setNode(level.getNode());

                        if (descItemPrev instanceof ArrDescItemString) {
                            ((ArrDescItemString) descItemPrev).setValue(((ArrDescItemUnitid) descItem).getValue());
                        } else {
                            throw new IllegalStateException(
                                    descItemPrev.getClass().getName() + " nema definovany prevod hodnoty");
                        }

                        ret = saveDescItem(descItemPrev, version, change);
                        level.setNode(ret.getNode());

                    }

                    ((ArrDescItemUnitid) descItem).setValue(unitId.getData());
                    ret = saveDescItem(descItem, version, change);
                    level.setNode(ret.getNode());

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
            generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode());
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
                .findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(), descItemType.getDescItemTypeId());
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
                .findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(),
                        descItemLevelType.getDescItemTypeId());
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
                if (!(descItem instanceof ArrDescItemUnitid)) {
                    throw new IllegalStateException(descItemType.getCode() + " neni typu ArrDescItemUnitid");
                }

                List<ArrLevel> childLevels = getChildren(level);

                String value = ((ArrDescItemUnitid) descItem).getValue();
                UnitId unitId = new UnitId(value);
                unitId.setSeparator("");

                UnitId unitIdChild = null;
                for (ArrLevel childLevel : childLevels) {
                    if (unitId != null && unitIdChild == null) {
                        unitIdChild = unitId.getClone();
                    }
                    generate(childLevel, rootNode, unitIdChild, descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode());
                }

            } else if(node.equals(rootNode)) {
                generate(level, rootNode, null, descItemLevel == null ? null : descItemLevel.getDescItemSpec().getCode());
            }

        }
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
                "change=" + change +
                ", version=" + version +
                '}';
    }
}