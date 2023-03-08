package cz.tacr.elza.print.format;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.Item;

public class When implements ConditionalFormatAction {

    private Expression expression;

    /**
     * List of formatting actions
     */
    List<FormatAction> whenActions = new ArrayList<>();

    List<FormatAction> elseActions;

    public When(final Expression expr) {
        this.expression = expr;
    }

    @Override
    public void format(FormatContext ctx, List<Item> items) {

        List<FormatAction> actions;
        if (this.expression.eval(ctx, items)) {
            // process block
            actions = whenActions;
        } else {
            // else block
            actions = elseActions;
        }
        if (actions != null) {
            for (FormatAction action : actions) {
                action.format(ctx, items);
            }
        }
    }

    @Override
    public void addAction(FormatAction formatAction) {
        if (elseActions != null) {
            elseActions.add(formatAction);
        } else {
            whenActions.add(formatAction);
        }
    }

    public void otherwise() {
        if (elseActions != null) {
            throw new BusinessException("Keyword 'otherwise' can be used only once after keyword 'when'",
                    BaseCode.INVALID_STATE);
        }
        elseActions = new ArrayList<>();
    }

}
