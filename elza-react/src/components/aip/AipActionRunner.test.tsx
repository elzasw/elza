import { describe, expect, it, vi } from 'vitest';
import { createIntl } from 'react-intl';
import { DaAipActionItemState, DaAipActionState, DaAipActionType, DaAipActionVO } from 'elza-api';

import { ActionWebsocket, notifyActionOutcome, watchAipAction } from './AipActionRunner';

/**
 * The user hears about the outcome of an action exactly once, in a tone that matches what happened.
 * The action is followed independently of the dialog, which can be closed before the action ends.
 */

type Listener = (message: unknown) => void;

const fakeWebsocket = () => {
    const listeners: Listener[] = [];
    const websocket: ActionWebsocket = {
        addListener: (listener) => {
            listeners.push(listener);
            return listener;
        },
        removeListener: (listener) => {
            const index = listeners.indexOf(listener as Listener);
            if (index >= 0) {
                listeners.splice(index, 1);
            }
        },
    };
    const push = (action: DaAipActionVO) =>
        [...listeners].forEach(listener => listener({ eventType: 'AIP_ACTION_UPDATE', action }));
    return { websocket, push, listeners };
};

const action = (overrides: Partial<DaAipActionVO> = {}): DaAipActionVO => ({
    id: 1,
    actionType: DaAipActionType.LoadMetadata,
    state: DaAipActionState.Running,
    createDate: '2026-09-04T08:00:00Z',
    items: [],
    ...overrides,
});

const intl = createIntl({ locale: 'cs', messages: {} });

describe('watchAipAction', () => {

    it('ohlásí dokončení a přestane poslouchat', () => {
        const { websocket, push, listeners } = fakeWebsocket();
        const onFinished = vi.fn();

        watchAipAction(websocket, action(), onFinished);
        push(action({ state: DaAipActionState.Running }));
        expect(onFinished).not.toHaveBeenCalled();

        const finished = action({ state: DaAipActionState.Finished });
        push(finished);
        expect(onFinished).toHaveBeenCalledWith(finished);
        expect(listeners).toHaveLength(0);
    });

    it('jinou akci ignoruje', () => {
        const { websocket, push } = fakeWebsocket();
        const onFinished = vi.fn();

        watchAipAction(websocket, action(), onFinished);
        push(action({ id: 99, state: DaAipActionState.Finished }));

        expect(onFinished).not.toHaveBeenCalled();
    });

    // An action the server settled at once (every AIP skipped, say) is finished in the response already
    it('akci hotovou už v odpovědi ohlásí rovnou', () => {
        const { websocket, listeners } = fakeWebsocket();
        const onFinished = vi.fn();
        const finished = action({ state: DaAipActionState.Finished });

        watchAipAction(websocket, finished, onFinished);

        expect(onFinished).toHaveBeenCalledWith(finished);
        expect(listeners).toHaveLength(0);
    });
});

describe('notifyActionOutcome', () => {

    const toastOf = (finished: DaAipActionVO) => {
        const dispatch = vi.fn();
        notifyActionOutcome(dispatch, intl, finished);
        expect(dispatch).toHaveBeenCalledTimes(1);
        return dispatch.mock.calls[0][0] as { style: string; title: string };
    };

    it('chybu hlásí jako chybu', () => {
        const toast = toastOf(action({
            state: DaAipActionState.Error,
            items: [
                { aipId: 1, state: DaAipActionItemState.Error, message: 'Chyba' },
                { aipId: 2, state: DaAipActionItemState.Finished },
            ],
        }));
        expect(toast.style).toBe('danger');
        expect(toast.title).toBe('Akce skončila chybou u 1 AIPu');
    });

    it('úspěch počítá jen provedené AIPy a přeskočené přizná', () => {
        const toast = toastOf(action({
            state: DaAipActionState.Finished,
            items: [
                { aipId: 1, state: DaAipActionItemState.Finished },
                { aipId: 2, state: DaAipActionItemState.Finished },
                { aipId: 3, state: DaAipActionItemState.Skipped, message: 'Není co dělat' },
            ],
        }));
        expect(toast.style).toBe('success');
        expect(toast.title).toBe('Akce dokončena u 2 AIPů · 1 přeskočen');
    });

    // An action that skipped every AIP did nothing - a success would claim otherwise
    it('akci, která všechno přeskočila, nehlásí jako úspěch', () => {
        const toast = toastOf(action({
            state: DaAipActionState.Finished,
            items: [{ aipId: 1, state: DaAipActionItemState.Skipped, message: 'AIP není navázaný na archivní soubor' }],
        }));
        expect(toast.style).toBe('warning');
        expect(toast.title).toBe('Akce nebyla u žádného AIPu provedena · 1 přeskočen');
    });
});
