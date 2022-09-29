package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.DeleteItemConfig;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DeleteItemResult;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.DescriptionItemService;

/**
 * Akce na odstranění hodnot atributu.
 */
@Component
@Scope("prototype")
public class DeleteItemAction extends Action {

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    private DeleteItemConfig config;

    private ItemType itemType;

    private ArrChange change;

    private ArrFundVersion fundVersion;

    DeleteItemAction(final DeleteItemConfig config) {
        Validate.notNull(config);
        this.config = config;
    }

    @Override
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        String inputTypeCode = config.getInputType();
        itemType = getStaticDataProvider().getItemTypeByCode(inputTypeCode);
        fundVersion = bulkActionRun.getFundVersion();
        change = bulkActionRun.getChange();
    }

    @Override
    public void apply(LevelWithItems level, TypeLevel typeLevel) {
        List<ArrDescItem> descItems = level.getDescItems(itemType, null);
        if (CollectionUtils.isEmpty(descItems)) {
            return;
        }

        List<ArrDescItem> deleteItems = null;
        ArrNode node = null;

        for (ArrItem item : descItems) {
            if (item instanceof ArrDescItem) {
                ArrDescItem arrDescItem = descItemRepository.getOne(item.getItemId());
                if (deleteItems == null) {
                    deleteItems = new ArrayList<>(descItems.size());
                    node = arrDescItem.getNode();
                }
                deleteItems.add(arrDescItem);
            }
        }
        if (deleteItems != null) {
            descriptionItemService.deleteDescriptionItems(deleteItems,
                                                          node,
                                                          fundVersion,
                                                          change,
                                                          // mazou se vsechny prvky popisu daneho typu,
                                                          // nemohou se posouvat pozice
                                                          false, false);
        }
    }

    @Override
    public ActionResult getResult() {
        return new DeleteItemResult();
    }
}
