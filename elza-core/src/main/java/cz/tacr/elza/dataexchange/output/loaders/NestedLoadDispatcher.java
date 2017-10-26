package cz.tacr.elza.dataexchange.output.loaders;

import org.apache.commons.lang3.Validate;

public abstract class NestedLoadDispatcher<R> extends BaseLoadDispatcher<R> {

    private final LoadDispatcher<?> parentDispatcher;

    public NestedLoadDispatcher(LoadDispatcher<?> parentDispatcher) {
        this.parentDispatcher = Validate.notNull(parentDispatcher);
    }

    @Override
    public final void onLoadBegin() {
        super.onLoadBegin();
        parentDispatcher.onLoadBegin();
    }

    @Override
    public final void onLoadEnd() {
        super.onLoadEnd();
        parentDispatcher.onLoadEnd();
    }
}
