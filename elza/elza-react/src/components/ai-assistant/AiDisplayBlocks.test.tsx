import React from 'react';
import { describe, it, expect } from 'vitest';

import { renderWithProviders, screen } from 'test/test-utils';
import { AiContextType, AiDisplayBlock, AiDisplayBlockType } from 'elza-api';
import { AiDisplayBlocks } from './AiDisplayBlocks';
import { recordLinkPath } from './recordLinks';

/**
 * Rendering of answer blocks: record links in the markdown (`ap:`/`node:`/`fund:`
 * targets, the provider protocol's way of citing a record of this archive)
 * become in-app links, web links stay external, and the RECORD_CITATIONS block
 * lists the linked records as navigation links.
 */
describe('AiDisplayBlocks', () => {
    it('maps record references onto in-app routes', () => {
        expect(recordLinkPath('ap:12')).toBe('/entity/12');
        expect(recordLinkPath('node:7')).toBe('/node/7');
        expect(recordLinkPath('fund:3')).toBe('/fund/3/tree');
        expect(recordLinkPath('https://stands.nacr.cz/zp')).toBeNull();
        expect(recordLinkPath('ap:x')).toBeNull();
    });

    it('turns record links in markdown into in-app links and keeps web links external', () => {
        const block = {
            type: AiDisplayBlockType.Markdown,
            content:
                'Entita [klokani (savci)](ap:12) je v registru, úroveň [Kronika](node:7); ' +
                'pravidlo [ZP2015](https://stands.nacr.cz/zp).',
        } as AiDisplayBlock;

        renderWithProviders(<AiDisplayBlocks blocks={[block]} />);

        expect(screen.getByText('klokani (savci)').closest('a')).toHaveAttribute('href', '/entity/12');
        expect(screen.getByText('Kronika').closest('a')).toHaveAttribute('href', '/node/7');
        const external = screen.getByText('ZP2015').closest('a');
        expect(external).toHaveAttribute('href', 'https://stands.nacr.cz/zp');
        expect(external).toHaveAttribute('target', '_blank');
    });

    it('renders the record citations block with the answer labels and a generic fallback', () => {
        const block = {
            type: AiDisplayBlockType.RecordCitations,
            records: [
                { label: 'klokani (savci)', target: { type: AiContextType.Accesspoint, accessPointId: 12 } },
                { target: { type: AiContextType.Node, nodeId: 7 } },
            ],
        } as unknown as AiDisplayBlock;

        renderWithProviders(<AiDisplayBlocks blocks={[block]} />);

        expect(screen.getByText('Záznamy')).toBeInTheDocument();
        expect(screen.getByText('klokani (savci)')).toBeInTheDocument();
        // A record link without text falls back to the generic "open" label.
        expect(screen.getByText('Zobrazit záznam')).toBeInTheDocument();
    });
});
