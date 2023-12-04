package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.generator.unitid.UnitIdException;
import cz.tacr.elza.bulkaction.generator.unitid.UnitIdPart;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrLockedValue;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.LockedValueRepository;
import cz.tacr.elza.service.ArrangementCacheService;

public class SealUnitId extends BulkActionDFS {

    static class StackUnitId {
        final Integer nodeId;
        final String unitId;

        StackUnitId lastSibling = null;

        public StackUnitId(Integer nodeId, String value) {
            this.nodeId = nodeId;
            this.unitId = value;
        }

        public Integer getNodeId() {
            return nodeId;
        }

        public void setLastSibling(StackUnitId stackUnitId) {
            this.lastSibling = stackUnitId;
        }

        public StackUnitId getLastSibling() {
            return lastSibling;
        }

        public String getUnitId() {
            return unitId;
        }
    }

    final SealUnitIdConfig config;

    @Autowired
    ArrangementCacheService arrangementCacheService;

    @Autowired
    LockedValueRepository usedValueRepository;

    Deque<StackUnitId> nodeIdStack = new ArrayDeque<>();

    private RulItemType itemType;

    public SealUnitId(SealUnitIdConfig sealUnitIdConfig) {
        this.config = sealUnitIdConfig;
    }

    @Override
    protected void init(ArrBulkActionRun bulkActionRun) {
        super.init(bulkActionRun);

        this.multipleItemChangeContext = descriptionItemService.createChangeContext(this.version.getFundVersionId());

        // prepare item type
        ItemType itemType = staticDataProvider.getItemTypeByCode(config.getItemType());
        Validate.notNull(itemType);

        // check if supported source data type
        if (itemType.getDataType() != DataType.UNITID) {
            throw new SystemException(
                    "Hromadná akce " + getName() + " je nakonfigurována pro nepodporovaný datový typ:",
                    BaseCode.SYSTEM_ERROR).set("itemTypeCode", itemType.getCode());
        }

        this.itemType = itemType.getEntity();
    }

    @Override
    protected void update(ArrLevel level) {
        Integer parentNodeId = level.getNodeIdParent();
        // Skip root -> root is without UnitId
        if (parentNodeId == null) {
            return;
        }
        StackUnitId parentUnitId = getFromStack(parentNodeId);
        // get unit id
        ArrDescItem descItem = loadSingleDescItem(level.getNode(), itemType);
        if (descItem == null || descItem.getData() == null) {
            // item without unitid -> error
            throw new SystemException(
                    "Every level of description has to has valid unit id. Found level without unit id",
                    BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("nodeId", level.getNodeId());
        }

        ArrData data = descItem.getData();
        ArrDataUnitid dataUnitId = HibernateUtils.unproxy(data);

        String value = dataUnitId.getUnitId();
        StackUnitId stackUnitId = new StackUnitId(level.getNodeId(), value);

        if (parentUnitId != null) {
            // has parent -> check unitid format
            // we have to check prev. sibling (if exists) or parent
            StackUnitId siblingUnitId = parentUnitId.getLastSibling();
            if (siblingUnitId != null) {
                validateSiblingUnitId(siblingUnitId.getUnitId(), value);
            } else {
                validateParentUnitId(parentUnitId.getUnitId(), value);
            }

            // set this as last sibling
            parentUnitId.setLastSibling(stackUnitId);
        }

        ArrFund fund = runContext.getFund();

        // find as used value
        ArrLockedValue fixedValue = usedValueRepository.findByFundAndItemTypeAndValue(fund, itemType, value);
        if (fixedValue == null) {
            // create new ArrDescItem
            ArrDescItem newItem = new ArrDescItem(descItem);
            newItem.setItemId(null);
            newItem.setCreateChange(getChange());
            newItem.setReadOnly(true);
            newItem = this.saveDescItem(newItem);

            // lock if not locked
            fixedValue = new ArrLockedValue();
            fixedValue.setFund(fund);
            fixedValue.setItem(newItem);
            fixedValue.setCreateChange(getChange());

            fixedValue = usedValueRepository.save(fixedValue);
        } else {
            // check that descItem match current frozen value
            if (!fixedValue.getItemId().equals(descItem.getItemId())) {
                throw new SystemException("Incorrect value to be fixed, value is already fixed with another item",
                        BaseCode.INVALID_STATE)
                                .set("value", value)
                                .set("fixedAtItemId", fixedValue.getItemId())
                                .set("otherItemId", descItem.getItemId());
            }
        }

        // store on stack
        nodeIdStack.push(stackUnitId);
    }

    private void validateSiblingUnitId(String siblUnitId, String unitId) {
        // siblings has to has same part till last slash
        int pos = siblUnitId.lastIndexOf('/');
        String parentUnitId = siblUnitId.substring(0, pos + 1);
        if (unitId.startsWith(parentUnitId)) {
            String partOfSiblUnitId = siblUnitId.substring(pos + 1);
            String partOfUnitId = unitId.substring(pos + 1);

            if (isUnitIdGreater(partOfUnitId, partOfSiblUnitId)) {
                return;
            }
        }
        throw new SystemException(
                "Incorrect sibling unitId",
                BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("siblingUnitId", siblUnitId)
                        .set("unitId", unitId);
    }

    private void validateParentUnitId(String parentUnitId, String unitId) {
        if (unitId.startsWith(parentUnitId)) {
            // check substring
            String lastPart = unitId.substring(parentUnitId.length());
            // lastPart should start with slash
            if (lastPart.startsWith("/")) {
                // remove leading slashes
                while (lastPart.startsWith("/")) {
                    lastPart = lastPart.substring(1);
                }
                // 
                if (isValidUnitPart(lastPart)) {
                    return;
                }
            }
        }
        throw new SystemException(
                "Incorrect child unitId",
                BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("parentUnitId", parentUnitId)
                        .set("unitId", unitId);

    }

    private boolean isUnitIdGreater(String part1, String part2) {
        try {
            UnitIdPart particle1 = UnitIdPart.parse(part1);
            UnitIdPart particle2 = UnitIdPart.parse(part2);
            int result = particle1.compareTo(particle2);
            return (result > 0);
        } catch (UnitIdException e) {
            return false;
        }
    }

    private boolean isValidUnitPart(String lastPart) {
        try {
            UnitIdPart.parse(lastPart);
        } catch (UnitIdException e) {
            return false;
        }
        return true;
    }

    private StackUnitId getFromStack(Integer nodeIdParent) {
        while (!nodeIdStack.isEmpty()) {
            StackUnitId topUnitId = nodeIdStack.peek();
            if (topUnitId.getNodeId().equals(nodeIdParent)) {
                return topUnitId;
            }
            nodeIdStack.pop();
        }
        return null;
    }

    @Override
    protected void done() {
        usedValueRepository.flush();
    }

    @Override
    public String getName() {
        return SealUnitId.class.getSimpleName();
    }

}
