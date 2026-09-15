import { IntlShape, defineMessages } from 'react-intl';
import { BatchState, ImportBatch } from 'elza-api';

import { addToastrDanger, addToastrSuccess, addToastrWarning } from 'components/shared/toastr/ToastrActions';

export const outcomeMessages = defineMessages({
    finished: {
        id: 'import.toast.batch.finished',
        defaultMessage: 'Import „{name}" byl dokončen',
    },
    failed: {
        id: 'import.toast.batch.failed',
        defaultMessage: 'Import „{name}" skončil s chybou',
    },
    failedDetail: {
        id: 'import.toast.batch.failed.detail',
        defaultMessage: 'Podrobnosti najdete v části Administrace → Import.',
    },
    cancelled: {
        id: 'import.toast.batch.cancelled',
        defaultMessage: 'Import „{name}" byl přerušen',
    },
    cancelledDetail: {
        id: 'import.toast.batch.cancelled.detail',
        defaultMessage: 'Položky dokončené před přerušením zůstávají naimportované, přehled najdete v části Administrace → Import.',
    },
});

type Dispatch = (action: unknown) => unknown;

const TERMINAL: BatchState[] = [BatchState.Finished, BatchState.Failed, BatchState.Cancelled];

/**
 * Tells the user how the batch ended, in a tone that matches what happened. Cancelling is what the
 * user asked for, so it is not a failure - and because the run commits each item on its own, what
 * got through before the stop stays imported.
 *
 * Returns false for a batch that has not ended yet, so the caller keeps following it.
 */
export function notifyBatchOutcome(dispatch: Dispatch, intl: IntlShape, batch: ImportBatch): boolean {
    if (!TERMINAL.includes(batch.state)) {
        return false;
    }
    const name = batch.name;
    if (batch.state === BatchState.Failed) {
        dispatch(addToastrDanger(
                intl.formatMessage(outcomeMessages.failed, { name }),
                intl.formatMessage(outcomeMessages.failedDetail)));
    } else if (batch.state === BatchState.Cancelled) {
        dispatch(addToastrWarning(
                intl.formatMessage(outcomeMessages.cancelled, { name }),
                intl.formatMessage(outcomeMessages.cancelledDetail)));
    } else {
        dispatch(addToastrSuccess(intl.formatMessage(outcomeMessages.finished, { name })));
    }
    return true;
}
