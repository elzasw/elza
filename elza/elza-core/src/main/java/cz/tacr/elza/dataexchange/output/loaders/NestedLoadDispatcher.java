package cz.tacr.elza.dataexchange.output.loaders;

import org.apache.commons.lang3.Validate;

/**
 * Nested load dispatcher delegating begin/end load calls to parent dispatcher.
 * Also represent his own completed stage by implementing {@link BaseLoadDispatcher}.
 */
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
