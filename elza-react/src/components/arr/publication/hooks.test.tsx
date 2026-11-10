import { PropsWithChildren } from 'react';
import { Provider } from 'react-redux';
import { describe, expect, it } from 'vitest';
import { ConnectionType, PublicationType } from 'elza-api';
import { createTestStore, renderHook } from 'test/test-utils';
import { useCanUsePublicationType } from './hooks';

const FUND_ID = 1;
const OTHER_FUND_ID = 2;

function createType(allowPermExport: boolean, allowPermPublication: boolean): PublicationType {
    return {
        id: 1,
        name: 'Portál',
        code: 'portal',
        retentionCount: 5,
        active: true,
        allowPermExport,
        allowPermPublication,
        connectionType: ConnectionType.Production,
    };
}

function renderCanUse(permissionsMap: Record<string, unknown>, fundId = FUND_ID) {
    const store = createTestStore({
        userDetail: { id: 1, username: 'test', userPermissions: {}, permissionsMap, fetched: true, fetching: false },
    });
    const wrapper = ({ children }: PropsWithChildren) => <Provider store={store}>{children}</Provider>;
    return renderHook(() => useCanUsePublicationType(fundId), { wrapper }).result.current;
}

describe('useCanUsePublicationType', () => {
    it('správce AS smí publikovat bez ohledu na zaškrtnutá oprávnění', () => {
        const canUse = renderCanUse({ FUND_ADMIN: { permission: 'FUND_ADMIN' } });

        expect(canUse(createType(false, false))).toBe(true);
        expect(canUse(createType(true, false))).toBe(true);
    });

    it('bez zaškrtnutých oprávnění nesmí publikovat běžný uživatel', () => {
        const canUse = renderCanUse({
            FUND_EXPORT: { permission: 'FUND_EXPORT', fundIdsMap: { [FUND_ID]: true } },
        });

        expect(canUse(createType(false, false))).toBe(false);
    });

    it('oprávnění k exportu stačí jen pro AS, ke kterému je přidělené', () => {
        const permissionsMap = {
            FUND_EXPORT: { permission: 'FUND_EXPORT', fundIdsMap: { [FUND_ID]: true } },
        };

        expect(renderCanUse(permissionsMap)(createType(true, false))).toBe(true);
        expect(renderCanUse(permissionsMap, OTHER_FUND_ID)(createType(true, false))).toBe(false);
        expect(renderCanUse(permissionsMap)(createType(false, true))).toBe(false);
    });

    it('oprávnění k publikaci stačí jen pro AS, ke kterému je přidělené', () => {
        const permissionsMap = {
            FUND_PUBLISH: { permission: 'FUND_PUBLISH', fundIdsMap: { [FUND_ID]: true } },
        };

        expect(renderCanUse(permissionsMap)(createType(false, true))).toBe(true);
        expect(renderCanUse(permissionsMap, OTHER_FUND_ID)(createType(false, true))).toBe(false);
        expect(renderCanUse(permissionsMap)(createType(true, false))).toBe(false);
    });
});
