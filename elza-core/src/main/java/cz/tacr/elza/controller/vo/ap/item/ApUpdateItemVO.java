package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.nodes.descitems.UpdateOp;

/**
 * Položka změny.
 */
public class ApUpdateItemVO {

    /**
     * Typ změny.
     */
    private UpdateOp updateOp;

    /**
     * Položka změny.
     */
    private ApItemVO item;

    public UpdateOp getUpdateOp() {
        return updateOp;
    }

    public void setUpdateOp(final UpdateOp updateOp) {
        this.updateOp = updateOp;
    }

    public ApItemVO getItem() {
        return item;
    }

    public void setItem(final ApItemVO item) {
        this.item = item;
    }
}
