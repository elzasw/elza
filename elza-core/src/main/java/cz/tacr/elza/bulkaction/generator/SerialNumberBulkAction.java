package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;


/**
 * Hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @author Martin Šlapa
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
    private RulDescItemType descItemType;

    /**
     * Stav hromadné akce
     */
    private BulkActionState bulkActionState;

    /**
     * Typ atributu pro zastaveni
     */
    private RulDescItemType descItemEndType;

    /**
     * Specifikace atributu pro zastaveni
     */
    private RulDescItemSpec descItemEndSpec;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemSpecRepository descItemSpecRepository;

    @Autowired
    private DescItemFactory descItemFactory;

    @Autowired
    private EventNotificationService eventNotificationService;

    /**
     * Inicializace hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    private void init(final BulkActionConfig bulkActionConfig) {

        Assert.notNull(bulkActionConfig);

        String serialIdCode = (String) bulkActionConfig.getProperty("serial_id_code");
        Assert.notNull(serialIdCode);

        descItemType = descItemTypeRepository.getOneByCode(serialIdCode);
        Assert.notNull(descItemType);

        String levelTypeCode = (String) bulkActionConfig.getProperty("level_type_code");
        if (levelTypeCode != null) {
            descItemEndType = descItemTypeRepository.getOneByCode(levelTypeCode);
            Assert.notNull(descItemEndType);

            String levelTypeEndGenerationForArrType = (String) bulkActionConfig.getProperty(
                    "level_type_end_generation_for_arr_type");
            Assert.notNull(levelTypeEndGenerationForArrType);
            descItemEndSpec = descItemSpecRepository.getOneByCode(levelTypeEndGenerationForArrType);
            Assert.notNull(descItemEndSpec);
        }
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     * @param rootNode
     */
    private void generate(final ArrLevel level, final ArrNode rootNode) {
        if (bulkActionState.isInterrupt()) {
            bulkActionState.setState(State.ERROR);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }

        if (!level.getNode().equals(rootNode)) {
            ArrDescItem descItem = loadDescItem(level);
            int sn = serialNumber.getNext();

            // vytvoření nového atributu
            if (descItem == null) {
                descItem = new ArrDescItemInt();
                descItem.setDescItemType(descItemType);
                descItem.setNode(level.getNode());
            }

            if (!(descItem instanceof ArrDescItemInt)) {
                throw new IllegalStateException(descItemType.getCode() + " není typu ArrDescItemInt");
            }

            // uložit pouze při rozdílu
            if (((ArrDescItemInt) descItem).getValue() == null || sn != ((ArrDescItemInt) descItem).getValue()) {
                ((ArrDescItemInt) descItem).setValue(sn);
                ArrDescItem ret = saveDescItem(descItem, version, change);
                level.setNode(ret.getNode());
            }

            if (descItemEndType != null) {
                ArrDescItem descItemLevel = loadDescItem(level, descItemEndType, descItemEndSpec);
                if (descItemLevel != null) {
                    return;
                }
            }
        }

        List<ArrLevel> childLevels = getChildren(level);

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel, rootNode);
        }

    }

    /**
     * Načtení atributu.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(
                level.getNode(), descItemType.getDescItemTypeId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    /**
     * Načtení atributu.
     *
     * @param level           uzel
     * @param rulDescItemSpec specifikace atributu
     * @param rulDescItemType typ atributu
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level,
                                     final RulDescItemType rulDescItemType,
                                     final RulDescItemSpec rulDescItemSpec) {
        List<ArrDescItem> descItems = descItemRepository
                .findByNodeAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(
                        level.getNode(), rulDescItemType.getDescItemTypeId(), rulDescItemSpec.getDescItemSpecId());
        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    rulDescItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    @Override
    @Transactional
    public void run(final Integer userId,
                    final Integer fundVersionId,
                    final List<Integer> inputNodeIds,
                    final BulkActionConfig bulkAction,
                    final BulkActionState bulkActionState) {
        this.bulkActionState = bulkActionState;
        init(bulkAction);

        ArrFundVersion version = fundVersionRepository.findOne(fundVersionId);

        Assert.notNull(version);
        checkVersion(version);
        this.version = version;

        this.change = createChange(userId);
        this.bulkActionState.setRunChange(this.change);
        this.serialNumber = new SerialNumber();

        ArrNode rootNode = version.getRootNode();

        for (Integer nodeId : inputNodeIds) {
            ArrNode node = nodeRepository.findOne(nodeId);
            Assert.notNull("Node s nodeId=" + nodeId + " neexistuje");
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull("Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            generate(level, rootNode);
        }

        eventNotificationService.publishEvent(EventFactory.createStringInVersionEvent(EventType.BULK_ACTION_STATE_CHANGE, fundVersionId, bulkAction.getCode()), true);
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
                ", change=" + change +
                '}';
    }
}