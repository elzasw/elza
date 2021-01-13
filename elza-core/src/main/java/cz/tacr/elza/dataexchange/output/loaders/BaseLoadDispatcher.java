package cz.tacr.elza.dataexchange.output.loaders;

import org.apache.commons.lang3.Validate;

/**
 * Base implementation declares completed handler based on begin/end load call count.
 */
public abstract class BaseLoadDispatcher<R> implements LoadDispatcher<R> {

    private int stepCount;

    @Override
    public void onLoadBegin() {
        stepCount++;
    }

    @Override
    public void onLoadEnd() {
        Validate.isTrue(stepCount > 0);
        stepCount--;

        if (stepCount == 0) {
            onCompleted();
        }
    }

    protected abstract void onCompleted();
}
