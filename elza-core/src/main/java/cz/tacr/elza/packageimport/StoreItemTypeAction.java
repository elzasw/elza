package cz.tacr.elza.packageimport;

import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeAction;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.RuleUpdateContext.RuleUpdateAction;
import cz.tacr.elza.repository.ItemTypeActionRepository;
import cz.tacr.elza.repository.ItemTypeRepository;

public class StoreItemTypeAction implements RuleUpdateAction {

    private ItemTypeRepository itemTypeRepository;
    private RulAction dbAction;
    private String itemTypeCode;
    private ItemTypeActionRepository itemTypeActionRepository;

    public StoreItemTypeAction(ItemTypeRepository itemTypeRepository, ItemTypeActionRepository itemTypeActionRepository,
                               String itemTypeCode, RulAction dbAction) {
        this.itemTypeRepository = itemTypeRepository;
        this.itemTypeActionRepository = itemTypeActionRepository;
        this.dbAction = dbAction;
        this.itemTypeCode = itemTypeCode;
    }

    @Override
    public void run(RuleUpdateContext ruc) {
        RulItemType rulItemType = itemTypeRepository.findOneByCode(itemTypeCode);
        if (rulItemType == null) {
            throw new BusinessException("RulItemType s code=" + itemTypeCode + " nenalezen",
                    PackageCode.CODE_NOT_FOUND)
                            .set("code", itemTypeCode);
        }

        RulItemTypeAction rulItemTypeAction = new RulItemTypeAction();
        rulItemTypeAction.setItemType(rulItemType);
        rulItemTypeAction.setAction(dbAction);

        itemTypeActionRepository.save(rulItemTypeAction);
    }

}
