import { describe, it, expect, vi } from 'vitest';

import { render } from 'test/test-utils';
import { getConnectedToJP } from './AipCells';

/**
 * Vygenerovaný AipDetailVO inicializuje linkedNodes na prázdné pole, takže se pole vždy
 * přenese; samotná jeho přítomnost proto neznamená, že je AIP na něco napojený.
 */

const icon = (container: HTMLElement) => container.querySelector('span.icon')?.className ?? '';

describe('getConnectedToJP', () => {
    it('prázdný seznam vazeb není napojení', () => {
        const { container } = render(<>{getConnectedToJP([], 1, vi.fn())}</>);

        expect(icon(container)).toContain('fa-close');
        expect(icon(container)).not.toContain('fa-check');
    });

    it('chybějící seznam vazeb není napojení', () => {
        const { container } = render(<>{getConnectedToJP(undefined, 1, vi.fn())}</>);

        expect(icon(container)).toContain('fa-close');
    });

    it('napojený AIP vypíše jednotky popisu', () => {
        const { container, getByText } = render(
            <>{getConnectedToJP([{id: 3, nodeId: 7, name: 'Složka A'}], 1, vi.fn())}</>,
        );

        expect(icon(container)).toContain('fa-check');
        expect(getByText('Složka A')).toBeInTheDocument();
    });
});
