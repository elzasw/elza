import { describe, it, expect } from 'vitest';
import splitter, { DEFAULT_SPLITTER_SIZES, SPLITTER_AREA_GLOBAL } from './splitter';
import { GLOBAL_SPLITTER_RESIZE, STORE_STATE_DATA_INIT } from '../../../actions/constants/ActionTypes';

const initialState = () => splitter(undefined, { type: '@@INIT' });

/** Uložená data mohou být poškozená, takže jsou hodnoty záměrně netypované. */
type StoredSizes = {leftWidth?: unknown; rightWidth?: unknown} | null;
type StoredSplitter = {splitters?: Record<string, StoredSizes>; leftWidth?: unknown; rightWidth?: unknown};

const restore = (storedSplitter: StoredSplitter) =>
    splitter(undefined, { type: STORE_STATE_DATA_INIT, storageData: { splitter: storedSplitter } });

describe('splitter reducer', () => {
    it('starts with the default sizes for every known area', () => {
        const state = initialState();

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual(DEFAULT_SPLITTER_SIZES);
        expect(state.splitters['ARR']).toEqual(DEFAULT_SPLITTER_SIZES);
    });

    it('stores sizes per area on resize', () => {
        const state = splitter(undefined, {
            type: GLOBAL_SPLITTER_RESIZE,
            area: 'ARR',
            leftSize: 300,
            rightSize: 200,
        });

        expect(state.splitters['ARR']).toEqual({ leftWidth: 300, rightWidth: 200 });
        // Ostatní oblasti zůstávají nedotčené.
        expect(state.splitters['AIP']).toEqual(DEFAULT_SPLITTER_SIZES);
    });

    it('falls back to the global area when the action carries none', () => {
        const state = splitter(undefined, {
            type: GLOBAL_SPLITTER_RESIZE,
            leftSize: 300,
            rightSize: 200,
        });

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual({ leftWidth: 300, rightWidth: 200 });
        expect(state.splitters['undefined']).toBeUndefined();
    });

    it('restores the persisted sizes of each area', () => {
        const state = restore({
            splitters: {
                [SPLITTER_AREA_GLOBAL]: { leftWidth: 400, rightWidth: 300 },
                ARR: { leftWidth: 111, rightWidth: 222 },
            },
        });

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual({ leftWidth: 400, rightWidth: 300 });
        expect(state.splitters['ARR']).toEqual({ leftWidth: 111, rightWidth: 222 });
        expect(state.splitters['AIP']).toEqual(DEFAULT_SPLITTER_SIZES);
    });

    it('restores an unknown area, so a newly added area is not lost', () => {
        const state = restore({ splitters: { NEW_AREA: { leftWidth: 111, rightWidth: 222 } } });

        expect(state.splitters['NEW_AREA']).toEqual({ leftWidth: 111, rightWidth: 222 });
    });

    it('restores the legacy flat format into the global area', () => {
        const state = restore({ leftWidth: 400, rightWidth: 300 });

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual({ leftWidth: 400, rightWidth: 300 });
        expect(state.splitters['ARR']).toEqual(DEFAULT_SPLITTER_SIZES);
    });

    it('replaces sizes that are out of range or not numbers with the defaults', () => {
        const state = restore({
            splitters: {
                [SPLITTER_AREA_GLOBAL]: { leftWidth: -1, rightWidth: 300 },
                ARR: { leftWidth: 4000, rightWidth: 300 },
                AIP: { leftWidth: '250', rightWidth: 300 },
            },
        });

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual({
            leftWidth: DEFAULT_SPLITTER_SIZES.leftWidth,
            rightWidth: 300,
        });
        expect(state.splitters['ARR'].leftWidth).toBe(DEFAULT_SPLITTER_SIZES.leftWidth);
        expect(state.splitters['AIP'].leftWidth).toBe(DEFAULT_SPLITTER_SIZES.leftWidth);
    });

    it('ignores persisted areas without a single usable size', () => {
        const state = restore({
            splitters: {
                [SPLITTER_AREA_GLOBAL]: { leftWidth: 400, rightWidth: 300 },
                // Pozůstatek dřívější chyby ukládání - oblast bez rozměrů.
                undefined: { leftWidth: undefined, rightWidth: undefined },
                JUNK: null,
            },
        });

        expect(state.splitters[SPLITTER_AREA_GLOBAL]).toEqual({ leftWidth: 400, rightWidth: 300 });
        expect(state.splitters['undefined']).toBeUndefined();
        expect(state.splitters['JUNK']).toBeUndefined();
    });

    it('keeps the state unchanged when nothing was persisted', () => {
        const state = splitter(undefined, { type: STORE_STATE_DATA_INIT, storageData: {} });

        expect(state).toEqual(initialState());
    });
});
