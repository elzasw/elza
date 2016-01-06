package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.BulkActionState.State;
import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionState;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemSpecRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;


/**
 * Hromadná akce maže hodnoty atributů.
 *
 * @author Martin Šlapa
 * @since 21.10.2015
 */
@Component
@Scope("prototype")
public class CleanDescriptionItemBulkAction extends BulkAction {

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "CLEAN_DESCRIPTION_ITEM";

    /**
     * Verze archivní pomůcky
     */
    private ArrFindingAidVersion version;

    /**
     * Změna
     */
    private ArrChange change;

    /**
     * Typ atributu pro smazání
     */
    private RulDescItemType descItemType;

    /**
     * Specifikace atributu pro smazání
     */
    private RulDescItemSpec descItemSpec;

    /**
     * Stav hromadné akce
     */
    private BulkActionState bulkActionState;

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

        String descriptionCode = (String) bulkActionConfig.getProperty("description_code");
        Assert.notNull(descriptionCode);
        this.descItemType = descItemTypeRepository.getOneByCode(descriptionCode);
        Assert.notNull(this.descItemType);

        String specificationCode = (String) bulkActionConfig.getProperty("specification_code");
        if (specificationCode != null) {
            this.descItemSpec = descItemSpecRepository.getOneByCode(specificationCode);
            Assert.notNull(this.descItemSpec);
        } else {
            this.descItemSpec = null;
        }
    }

    /**
     * Generování hodnot - rekurzivní volání pro procházení celého stromu
     *
     * @param level uzel
     */
    private void generate(final ArrLevel level) {
        if (bulkActionState.isInterrupt()) {
            bulkActionState.setState(State.ERROR);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }

        ArrDescItem descItem = loadDescItem(level);

        // smazat pouze existujici
        if (descItem != null) {
            ArrDescItem ret = deleteDescItem(descItem, this.version, this.change);
            level.setNode(ret.getNode());
        }

        List<ArrLevel> childLevels = getChildren(level);

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel);
        }
    }

    /**
     * Načtení atributu.
     *
     * @param level uzel
     * @return nalezený atribut
     */
    private ArrDescItem loadDescItem(final ArrLevel level) {
        List<ArrDescItem> descItems;
        if (descItemSpec == null) {
            descItems = descItemRepository.findByNodeAndDeleteChangeIsNullAndDescItemTypeId(level.getNode(),
                    descItemType.getDescItemTypeId());
        } else {
            descItems = descItemRepository
                    .findByNodeAndDeleteChangeIsNullAndDescItemTypeIdAndSpecItemTypeId(level.getNode(),
                            descItemType.getDescItemTypeId(), descItemSpec.getDescItemSpecId());
        }

        if (descItems.size() == 0) {
            return null;
        }
        if (descItems.size() > 1) {
            throw new IllegalStateException(
                    descItemType.getCode() + " nemuže být více než jeden (" + descItems.size() + ")");
        }
        return descItemFactory.getDescItem(descItems.get(0));
    }

    @Override
    @Transactional
    public void run(final Integer faVersionId,
                    final BulkActionConfig bulkActionConfig,
                    final BulkActionState bulkActionState) {
        this.bulkActionState = bulkActionState;
        init(bulkActionConfig);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(faVersionId);
        Assert.notNull(version);
        this.version = version;
        checkVersion(version);

        this.change = createChange();
        this.bulkActionState.setRunChange(this.change);

        generate(version.getRootLevel());
    }

    @Override
    public String toString() {
        return "CleanDescriptionItemBulkAction{" +
                "version=" + version +
                ", change=" + change +
                '}';
    }
}