import { describe, expect, it } from 'vitest';
import type { AipDetailVO, LinkedNodeVO, LogicalFilter, TextValueFilter } from 'elza-api';
import { aipEntryFound, aipEntrySearch, aipEntryTarget } from './aipEntryTarget';

const PART = '973b4414-03e8-46dd-9746-a5a5c1e02f67';
const AIP = '1659488e-6b5c-4e3b-99aa-7da151f16ec7';

const link = (nodeId: number, daoCode?: string): LinkedNodeVO => ({ id: nodeId * 10, nodeId, name: 'JP', daoCode });

const aip = (linkedNodes: LinkedNodeVO[], fundId?: number): AipDetailVO => ({
    aipId: 224,
    code: AIP,
    digitalRepositoryId: 22,
    digitalRepositoryCode: 'DAMO',
    fund: fundId != null ? { id: fundId } as AipDetailVO['fund'] : undefined,
    linkedNodes,
});

describe('aipEntryTarget', () => {
    it('opens the only linked unit of description', () => {
        expect(aipEntryTarget(aip([link(7, 'uuid-' + PART)], 1042))).toBe('/node/7');
    });

    it('opens a unit linked through several parts as the only one', () => {
        expect(aipEntryTarget(aip([link(7, 'uuid-' + PART), link(7)], 1042))).toBe('/node/7');
    });

    it('opens the package in its fund when more units are linked', () => {
        expect(aipEntryTarget(aip([link(7), link(8, 'uuid-' + PART)], 1042)))
            .toBe('/fund/1042/aip/224/explorer');
    });

    it('opens the package in its fund when nothing is linked', () => {
        expect(aipEntryTarget(aip([], 1042))).toBe('/fund/1042/aip/224/explorer');
    });

    it('opens the unit linked to the requested part, written in any form', () => {
        const linked = aip([link(7), link(8, 'uuid-' + PART)], 1042);
        expect(aipEntryTarget(linked, PART)).toBe('/node/8');
        expect(aipEntryTarget(linked, 'UUID-' + PART.toUpperCase())).toBe('/node/8');
    });

    it('opens the unit holding the whole package when the requested part has none of its own', () => {
        expect(aipEntryTarget(aip([link(7), link(8, 'uuid-other')], 1042), PART)).toBe('/node/7');
    });

    it('selects a requested part without a single unit in the structure', () => {
        expect(aipEntryTarget(aip([link(7, 'uuid-other'), link(8, 'uuid-another')], 1042), PART))
            .toBe(`/fund/1042/aip/224/explorer?tab=structure&select=${PART}`);
        expect(aipEntryTarget(aip([], 1042), PART))
            .toBe(`/fund/1042/aip/224/explorer?tab=structure&select=${PART}`);
    });

    it('opens an AIP without a known fund in the AIP list', () => {
        expect(aipEntryTarget(aip([]))).toBe('/aip/224');
    });
});

describe('aipEntryFound', () => {
    it('prefers the code exactly as written over its other form', () => {
        const bare = { ...aip([]), aipId: 1 };
        const prefixed = { ...aip([]), aipId: 2, code: 'uuid-' + AIP };
        expect(aipEntryFound([bare, prefixed], 'uuid-' + AIP)?.aipId).toBe(2);
        expect(aipEntryFound([prefixed, bare], AIP)?.aipId).toBe(1);
        expect(aipEntryFound([prefixed], AIP)?.aipId).toBe(2);
        expect(aipEntryFound([], AIP)).toBeUndefined();
    });
});

describe('aipEntrySearch', () => {
    const codes = (aipCode: string) => {
        const any = aipEntrySearch('DAMO', aipCode).filters?.[1] as LogicalFilter;
        return (any.filters as TextValueFilter[]).map(f => f.value);
    };

    it('searches within the digital archive', () => {
        const repository = aipEntrySearch(' DAMO ', AIP).filters?.[0] as TextValueFilter;
        expect(repository.field).toMatchObject({ fieldName: 'digitalRepositoryCode' });
        expect(repository.value).toBe('DAMO');
    });

    it('looks a UUID up with and without the prefix', () => {
        expect(codes(AIP)).toEqual([AIP, 'uuid-' + AIP]);
        expect(codes('uuid-' + AIP)).toEqual(['uuid-' + AIP, AIP]);
    });

    it('looks up another identifier as it is', () => {
        expect(codes('ABC-1')).toEqual(['ABC-1']);
    });
});
