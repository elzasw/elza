import { describe, it, expect, vi } from 'vitest';
import { DaAipActionItemState, DaAipActionState, DaAipActionType, DaAipActionVO } from 'elza-api';

import { renderWithProviders, screen, act } from 'test/test-utils';
import { AipActionResult } from './AipActionResult';
import { useAipAction } from './useAipAction';

/**
 * Akce nad AIPy se provádí po jednotlivých AIPech a na pozadí. Uživatel se o tom, co se s kterým
 * AIPem stalo, dozví jen tady - a u AIPu, se kterým akce nic neudělala, je podstatný důvod.
 */

type Listener = (message: unknown) => void;

const listeners: Listener[] = [];
const fakeWebsocket = {
    addListener: (listener: Listener) => {
        listeners.push(listener);
        return listener;
    },
    removeListener: (listener: Listener) => {
        const index = listeners.indexOf(listener);
        if (index >= 0) {
            listeners.splice(index, 1);
        }
    },
};

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({
    useWebsocket: () => fakeWebsocket,
}));

const action = (overrides: Partial<DaAipActionVO> = {}): DaAipActionVO => ({
    id: 1,
    actionType: DaAipActionType.DbUpdate,
    state: DaAipActionState.Running,
    createDate: '2026-08-29T08:00:00Z',
    items: [],
    ...overrides,
});

describe('AipActionResult', () => {

    it('u přeskočeného AIPu vypíše důvod, ne jen stav', () => {
        renderWithProviders(<AipActionResult action={action({
            state: DaAipActionState.Finished,
            items: [{
                aipId: 9,
                aipCode: 'AIP-9',
                state: DaAipActionItemState.Skipped,
                message: 'V ELZA není uložený balíček s metadaty.',
            }],
        })} />);

        expect(screen.getByText('AIP-9')).toBeInTheDocument();
        expect(screen.getByText('Přeskočeno')).toBeInTheDocument();
        expect(screen.getByText('V ELZA není uložený balíček s metadaty.')).toBeInTheDocument();
    });

    it('shrnutí počítá jen dokončené položky', () => {
        renderWithProviders(<AipActionResult action={action({
            items: [
                { aipId: 1, aipCode: 'A', state: DaAipActionItemState.Finished },
                { aipId: 2, aipCode: 'B', state: DaAipActionItemState.Error, message: 'Chyba' },
                { aipId: 3, aipCode: 'C', state: DaAipActionItemState.Waiting },
            ],
        })} />);

        // dvě ze tří hotové, z toho jedna chybná
        expect(screen.getByText(/2.*3.*1/)).toBeInTheDocument();
    });
});

/** Komponenta jen pro test - vykreslí stav, který hook drží. */
function ActionProbe({ initial }: { initial: DaAipActionVO }) {
    const { action: current, finished } = useAipAction(initial);
    return (
        <div>
            <span data-testid="state">{current?.state}</span>
            <span data-testid="finished">{String(finished)}</span>
            <span data-testid="items">{current?.items?.length ?? 0}</span>
        </div>
    );
}

describe('useAipAction', () => {

    it('nahradí akci celým snímkem, který přijde po websocketu', () => {
        renderWithProviders(<ActionProbe initial={action({
            items: [{ aipId: 1, aipCode: 'A', state: DaAipActionItemState.Waiting }],
        })} />);

        expect(screen.getByTestId('state')).toHaveTextContent(DaAipActionState.Running);
        expect(screen.getByTestId('finished')).toHaveTextContent('false');

        act(() => {
            listeners.forEach(listener => listener({
                eventType: 'AIP_ACTION_UPDATE',
                action: action({
                    state: DaAipActionState.Error,
                    items: [
                        { aipId: 1, aipCode: 'A', state: DaAipActionItemState.Error, message: 'Chyba' },
                        { aipId: 2, aipCode: 'B', state: DaAipActionItemState.Finished },
                    ],
                }),
            }));
        });

        expect(screen.getByTestId('state')).toHaveTextContent(DaAipActionState.Error);
        expect(screen.getByTestId('finished')).toHaveTextContent('true');
        expect(screen.getByTestId('items')).toHaveTextContent('2');
    });

    it('zprávu o jiné akci ignoruje', () => {
        renderWithProviders(<ActionProbe initial={action()} />);

        act(() => {
            listeners.forEach(listener => listener({
                eventType: 'AIP_ACTION_UPDATE',
                action: action({ id: 99, state: DaAipActionState.Finished }),
            }));
        });

        expect(screen.getByTestId('state')).toHaveTextContent(DaAipActionState.Running);
    });
});
