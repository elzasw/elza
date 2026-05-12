import { describe, it, expect, vi, beforeEach } from 'vitest';

/**
 * Mock the Api object so the test doesn't pull in the full Redux/component
 * graph (which has a load-order issue around `getServerContextPath()` being
 * called at module top-level in MapPage). The unit under test only needs
 * to route to the right `Api.node.*` method based on identifier shape.
 */
// vi.hoisted lifts these declarations above the hoisted vi.mock call, so the
// factory can capture them without hitting the const-TDZ.
const { mockGetNodeInfoByUuid, mockGetNodeInfoById } = vi.hoisted(() => ({
    mockGetNodeInfoByUuid: vi.fn(),
    mockGetNodeInfoById: vi.fn(),
}));

vi.mock('../api', () => ({
    Api: {
        node: {
            nodeGetNodeInfoByUuid: mockGetNodeInfoByUuid,
            nodeGetNodeInfoById: mockGetNodeInfoById,
        },
    },
}));

import { fetchNodeInfo } from './fetchNodeInfo';

const NODE_INFO_FIXTURE = {
    id: 42,
    version: 7,
    uuid: 'a4f1b2c3-1111-2222-3333-444455556666',
    fundId: 5,
    fundVersionId: 11,
};

describe('fetchNodeInfo', () => {
    beforeEach(() => {
        mockGetNodeInfoByUuid.mockReset();
        mockGetNodeInfoById.mockReset();
        mockGetNodeInfoByUuid.mockResolvedValue({ data: NODE_INFO_FIXTURE });
        mockGetNodeInfoById.mockResolvedValue({ data: NODE_INFO_FIXTURE });
    });

    it('routes a 36-char UUID to nodeGetNodeInfoByUuid', async () => {
        const result = await fetchNodeInfo(NODE_INFO_FIXTURE.uuid);

        expect(mockGetNodeInfoByUuid).toHaveBeenCalledTimes(1);
        expect(mockGetNodeInfoByUuid).toHaveBeenCalledWith(NODE_INFO_FIXTURE.uuid);
        expect(mockGetNodeInfoById).not.toHaveBeenCalled();
        expect(result).toEqual(NODE_INFO_FIXTURE);
    });

    it('routes a numeric string to nodeGetNodeInfoById after parseInt', async () => {
        const result = await fetchNodeInfo('42');

        expect(mockGetNodeInfoById).toHaveBeenCalledTimes(1);
        expect(mockGetNodeInfoById).toHaveBeenCalledWith(42);
        expect(mockGetNodeInfoByUuid).not.toHaveBeenCalled();
        expect(result).toEqual(NODE_INFO_FIXTURE);
    });
});
