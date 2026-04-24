import { describe, it, expect } from 'vitest';
import status from './status';
import { STATUS_SAVED, STATUS_SAVING } from '../../actions/constants/ActionTypes';

describe('status reducer', () => {
    it('returns the initial state for unknown actions', () => {
        const state = status(undefined, { type: '@@INIT' });

        expect(state).toEqual({ saveCounter: 0 });
    });

    it('increments saveCounter on STATUS_SAVING', () => {
        const state = status({ saveCounter: 0 }, { type: STATUS_SAVING });

        expect(state.saveCounter).toBe(1);
    });

    it('decrements saveCounter on STATUS_SAVED and clears the saving flag', () => {
        const state = status({ saveCounter: 2, saving: true }, { type: STATUS_SAVED });

        expect(state.saveCounter).toBe(1);
        expect(state.saving).toBe(false);
    });

    it('clamps saveCounter at zero', () => {
        const state = status({ saveCounter: 0 }, { type: STATUS_SAVED });

        expect(state.saveCounter).toBe(0);
    });

    it('round-trips saving/saved pairs', () => {
        let s = status(undefined, { type: '@@INIT' });
        s = status(s, { type: STATUS_SAVING });
        s = status(s, { type: STATUS_SAVING });
        s = status(s, { type: STATUS_SAVED });

        expect(s.saveCounter).toBe(1);
    });
});
