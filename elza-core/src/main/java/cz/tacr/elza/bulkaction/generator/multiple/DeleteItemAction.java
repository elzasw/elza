package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.bulkaction.generator.DeleteItemConfig;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DeleteItemResult;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.repository.DescItemRepository;

/**
 * Akce na odstranění hodnot atributu.
 */
@Component
@Scope("prototype")
public class DeleteItemAction extends Action {

    @Autowired
    private DescItemRepository descItemRepository;

    private DeleteItemConfig config;

    private ItemType itemType;

    DeleteItemAction(final DeleteItemConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    public void init(BulkActionTransactional bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        // initialize multipleChangeContext
        bulkAction.getMultipleItemChangeContext();

        String inputTypeCode = config.getInputType();
        itemType = getStaticDataProvider().getItemTypeByCode(inputTypeCode);
    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        List<ArrDescItem> descItems = level.getDescItems(itemType, null);
        if (CollectionUtils.isEmpty(descItems)) {
            return;
        }

        List<ArrDescItem> deleteItems = null;

        for (ArrItem item : descItems) {
            if (item instanceof ArrDescItem) {
                ArrDescItem arrDescItem = descItemRepository.getReferenceById(item.getItemId());
                if (deleteItems == null) {
                    deleteItems = new ArrayList<>(descItems.size());
                }
                deleteItems.add(arrDescItem);
            }
        }
        if (deleteItems != null) {
            bulkAction.deleteDescItems(
            							deleteItems,
                                       // mazou se vsechny prvky popisu daneho typu,
                                       // nemohou se posouvat pozice
                                       false);
        }
    }

    @Override
    public ActionResult getResult() {
        return new DeleteItemResult();
    }
}
