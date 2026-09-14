import { describe, expect, it, vi } from 'vitest';
import { AipDetailVO } from 'elza-api';

import { renderWithProviders, screen } from 'test/test-utils';
import { AipOverview } from './AipOverview';

vi.mock('../../../api', async (importOriginal) => ({
    ...await importOriginal<typeof import('../../../api')>(),
    serverContextPath: '',
}));

/**
 * The overview is the first thing the user sees of a package: its values, and the download,
 * which only makes sense once the package is on disk.
 */

const detail = (overrides: Partial<AipDetailVO> = {}) => ({
    aipId: 11,
    code: '04ccc520-c5a9-4c9f-a83f-28d91fd37aa7',
    digitalRepositoryId: 1,
    ...overrides,
} as AipDetailVO);

describe('AipOverview', () => {
    it('stažení nabídne jen u balíčku, který je na disku', () => {
        const { unmount } = renderWithProviders(<AipOverview detail={detail()} onOpenProblemFile={vi.fn()} />);
        expect(screen.queryByText('Stáhnout balíček')).toBeNull();
        unmount();

        renderWithProviders(<AipOverview detail={detail({ metadataLoad: true })} onOpenProblemFile={vi.fn()} />);
        expect(screen.getByText('Stáhnout balíček').closest('a'))
            .toHaveAttribute('href', '/api/v1/aip/11/package/download');
    });

    it('vypíše hodnoty balíčku', () => {
        renderWithProviders(<AipOverview detail={detail()} onOpenProblemFile={vi.fn()} />);

        expect(screen.getByText('04ccc520-c5a9-4c9f-a83f-28d91fd37aa7')).toBeInTheDocument();
    });
});
