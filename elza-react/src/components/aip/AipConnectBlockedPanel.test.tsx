import { describe, it, expect } from 'vitest';
import { AipConnectBlockedVO } from 'elza-api';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { AipConnectBlockedPanel } from './AipConnectBlockedPanel';

/**
 * Napojení už napojený AIP odmítne. U výběru padesáti AIPů by uživatel po stisku tlačítka jen
 * viděl chybu a nevěděl, kterého AIPu se týká - proto se to říká předem a na požádání i po kusech.
 */

const blocked = (count: number): AipConnectBlockedVO[] =>
    Array.from({ length: count }, (_, i) => ({
        aipId: i + 1,
        aipCode: `AIP-${i + 1}`,
        reason: 'AIP je již připojen k jiné jednotce popisu a úložiště neumožňuje více vazeb.',
    }));

describe('AipConnectBlockedPanel', () => {

    it('nic nevykreslí, když nic nebrání', () => {
        const { container } = renderWithProviders(<AipConnectBlockedPanel blocked={[]} />);
        expect(container).toBeEmptyDOMElement();
    });

    it('řekne, kolika AIPů se to týká', () => {
        renderWithProviders(<AipConnectBlockedPanel blocked={blocked(50)} />);
        expect(screen.getByText(/\b50\b/)).toBeInTheDocument();
    });

    it('seznam AIPů ukáže až na vyžádání', () => {
        renderWithProviders(<AipConnectBlockedPanel blocked={blocked(2)} />);

        expect(screen.queryByText('AIP-1')).toBeNull();

        fireEvent.click(screen.getByText('Zobrazit seznam'));

        expect(screen.getByText('AIP-1')).toBeInTheDocument();
        expect(screen.getByText('AIP-2')).toBeInTheDocument();
    });
});
