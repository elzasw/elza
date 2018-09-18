package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO pro přenost typu operace s hodnotou atributu.
 *
 * @since 04.04.2018
 */
public class ArrUpdateItemVO {

    private UpdateOp updateOp;

    private ArrItemVO item;

    public UpdateOp getUpdateOp() {
        return updateOp;
    }

    public void setUpdateOp(final UpdateOp updateOp) {
        this.updateOp = updateOp;
    }

    public ArrItemVO getItem() {
        return item;
    }

    public void setItem(final ArrItemVO item) {
        this.item = item;
    }
}
