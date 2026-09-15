import { describe, expect, it, vi } from 'vitest';
import { AipFieldName } from 'elza-api';

import { createTestStore } from 'test/test-utils';
import { buildFilter } from 'components/aip/filter/aipFilterModel';
import { AipFilterEntry } from 'typings/store';

/**
 * Skok na balíček v seznamu AIP.
 *
 * Kde balíček ve stránkovaném seznamu leží, ví jen server, takže se v seznamu musí projevit to,
 * co vrátí: stránka, kterou vybral, ne ta, o kterou klient požádal.
 */

const findByFilter = vi.fn();

vi.mock('../../api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../api')>();
    return {
        ...actual,
        Api: {aips: {aipFindByFilter: (...args: unknown[]) => findByFilter(...args)}},
    };
});

// až po mocku, aby akce sáhla na podvrženou Api
const { aipsFocus, DEFAULT_PAGE_SIZE } = await import('./aip');

const fundFilter: AipFilterEntry = {
    id: 'fund',
    field: AipFieldName.Fund,
    filter: buildFilter(AipFieldName.Fund, 'ref', {operation: 'EQ', value: 106}),
    invisible: true,
};

const response = (data: Record<string, unknown>) => {
    findByFilter.mockResolvedValue({data: {count: 0, rows: [], ...data}});
};

const aipList = (store: ReturnType<typeof createTestStore>) =>
    (store.getState() as Record<string, any>).app.aipList;

describe('aipsFocus', () => {
    it('otevře stránku, na které balíček podle serveru leží', async () => {
        response({offset: 50, focusFound: true, count: 137, rows: [{aipId: 7, code: 'AIP-7'}]});
        const store = createTestStore();

        const found = await store.dispatch(aipsFocus(7, [fundFilter], 25) as never);

        expect(found).toBe(true);
        expect(aipList(store).filter.from).toBe(50);
        expect(aipList(store).rows).toEqual([{aipId: 7, code: 'AIP-7'}]);
        // stránka nese jen svůj kus, celkový počet musí zůstat ze serveru
        expect(aipList(store).count).toBe(137);
    });

    it('pošle hledaný balíček i podmínky obrazovky, stránku od začátku', async () => {
        response({offset: 0, focusFound: true});
        const store = createTestStore();

        await store.dispatch(aipsFocus(7, [fundFilter], 25) as never);

        const calls = findByFilter.mock.calls;
        const [params, focusAipId] = calls[calls.length - 1];
        expect(focusAipId).toBe(7);
        expect(params).toMatchObject({offset: 0, size: 25, filters: [fundFilter.filter]});
    });

    it('balíček mimo filtr vrátí false a seznam zůstane na první stránce', async () => {
        response({offset: 0, focusFound: false, count: 4});
        const store = createTestStore();

        const found = await store.dispatch(aipsFocus(7, [fundFilter], 25) as never);

        expect(found).toBe(false);
        expect(aipList(store).filter.from).toBe(0);
    });

    it('bez velikosti stránky se použije výchozí', async () => {
        response({offset: 0, focusFound: true});
        const store = createTestStore();

        await store.dispatch(aipsFocus(7, []) as never);

        const calls = findByFilter.mock.calls;
        expect(calls[calls.length - 1][0]).toMatchObject({size: DEFAULT_PAGE_SIZE});
        expect(aipList(store).filter.pageSize).toBe(DEFAULT_PAGE_SIZE);
    });
});
