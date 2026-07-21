import React from 'react';
import { describe, it, expect } from 'vitest';

import { renderWithProviders, screen } from 'test/test-utils';
import { AiContextNode, AiContextType, AiRequestActivity } from 'elza-api';
import { AiRequestActivities } from './AiRequestActivities';

/**
 * Rendering of the tool-activity feed: a provider-internal step shows its
 * localized label/summary and a ref link, while a client tool with no label
 * falls back to the locally localized tool name.
 */
describe('AiRequestActivities', () => {
    it('renders a provider-internal step with its label, summary and a ref link', () => {
        const target: AiContextNode = { type: AiContextType.Node, nodeId: 88 };
        const activity: AiRequestActivity = {
            id: 'toolu_3',
            kind: 'TOOL_CALL',
            tool: 'get_archival_description',
            label: 'Načtení archivního popisu',
            summary: 'úroveň 88',
            state: 'DONE',
            startDate: '2026-07-15T09:00:00Z',
            links: [{ target }],
        };

        renderWithProviders(<AiRequestActivities activities={[activity]} />);

        // The provider label wins over the raw snake_case tool name.
        expect(screen.getByText('Načtení archivního popisu')).toBeInTheDocument();
        expect(screen.getByText('úroveň 88')).toBeInTheDocument();
        // A ref link carries no name — it renders the generic "open" label.
        expect(screen.getByText('Zobrazit záznam')).toBeInTheDocument();
    });

    it('renders a client tool by its locally localized name when no label is given', () => {
        const activity: AiRequestActivity = {
            id: 'c1',
            kind: 'TOOL_CALL',
            tool: 'searchNodes',
            state: 'DONE',
            query: 'Krnov',
            resultCount: 3,
            startDate: '2026-07-15T09:00:00Z',
        };

        renderWithProviders(<AiRequestActivities activities={[activity]} />);

        expect(screen.getByText('Vyhledávání v archivním popisu')).toBeInTheDocument();
        expect(screen.getByText('Krnov')).toBeInTheDocument();
    });
});
