package cz.tacr.elza.dataexchange.output.loaders;

import org.apache.commons.lang.Validate;

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
