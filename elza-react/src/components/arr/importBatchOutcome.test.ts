import { describe, expect, it, vi } from 'vitest';
import { createIntl } from 'react-intl';
import { BatchImportType, BatchState, ImportBatch } from 'elza-api';

import { ignoreMissingTranslation } from 'test/test-utils';

import { notifyBatchOutcome } from './importBatchOutcome';

/**
 * The user hears how the batch ended in a tone that matches what happened. A batch the user
 * stopped is not a failure - reporting it as one told them their import had broken when they had
 * just asked it to stop (#9991).
 */

const batch = (state: BatchState): ImportBatch => ({
    batchId: 1,
    importType: BatchImportType.Edx2,
    name: 'dávka',
    state,
    createdAt: '2026-09-11T08:00:00Z',
    createdByUserId: 1,
    lastStateChangeAt: '2026-09-11T08:10:00Z',
    skipError: false,
    keepFiles: false,
});

// Empty catalog on purpose - the assertions are on the Czech source texts in defineMessages.
const intl = createIntl({ locale: 'cs', messages: {}, onError: ignoreMissingTranslation });

const toastOf = (state: BatchState) => {
    const dispatch = vi.fn();
    expect(notifyBatchOutcome(dispatch, intl, batch(state))).toBe(true);
    expect(dispatch).toHaveBeenCalledTimes(1);
    return dispatch.mock.calls[0][0] as { style: string; title: string; message: string };
};

describe('notifyBatchOutcome', () => {

    it('dokončení hlásí jako úspěch', () => {
        const toast = toastOf(BatchState.Finished);
        expect(toast.style).toBe('success');
        expect(toast.title).toBe('Import „dávka" byl dokončen');
    });

    it('chybu hlásí jako chybu', () => {
        const toast = toastOf(BatchState.Failed);
        expect(toast.style).toBe('danger');
        expect(toast.title).toBe('Import „dávka" skončil s chybou');
    });

    it('přerušení nehlásí jako chybu a řekne, že dokončené položky zůstávají', () => {
        const toast = toastOf(BatchState.Cancelled);
        expect(toast.style).not.toBe('danger');
        expect(toast.style).toBe('warning');
        expect(toast.title).toBe('Import „dávka" byl přerušen');
        expect(toast.message).toContain('zůstávají naimportované');
    });

    it('neukončenou dávku nehlásí a nechá ji dál sledovat', () => {
        for (const state of [BatchState.Preparation, BatchState.InProgress, BatchState.TestInProgress,
                             BatchState.TestFinished, BatchState.Paused]) {
            const dispatch = vi.fn();
            expect(notifyBatchOutcome(dispatch, intl, batch(state))).toBe(false);
            expect(dispatch).not.toHaveBeenCalled();
        }
    });
});
