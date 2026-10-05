import { describe, it, expect, vi } from 'vitest';

// the page module pulls in the whole arrangement page stack; only the address parsing is tested
vi.mock('./ArrParentPage', () => ({ default: class {} }));
vi.mock('../../components/index', () => ({ Ribbon: (): null => null }));
vi.mock('../../components/arr/aip/assignment/AipBulkConnectPanel', () => ({ default: (): null => null }));

import { parseAipIds } from './ArrAipConnectPage';

describe('parseAipIds', () => {
    it('reads the packages from the address and skips what is not an id', () => {
        expect(parseAipIds('?aips=1,3,x,,-2,4.5,7')).toEqual([1, 3, 7]);
        expect(parseAipIds('')).toEqual([]);
    });
});
