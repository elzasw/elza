import { describe, it, expect, vi } from 'vitest';

import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import {
    AiContextNode,
    AiContextType,
    AiDisplayBlockType,
    AiNodeUpdateProposalsBlock as AiNodeUpdateProposalsBlockVO,
    AiProposalChange,
} from 'elza-api';
import { AiNodeUpdateProposalsBlock } from './AiNodeUpdateProposalsBlock';

function blockWith(change: AiProposalChange): AiNodeUpdateProposalsBlockVO {
    const target: AiContextNode = { type: AiContextType.Node, nodeId: 7 };
    return {
        type: AiDisplayBlockType.NodeUpdateProposals,
        nodes: [{ node: { label: 'Kronika', target }, changes: [change] }],
    };
}

const operation = { kind: 'UPDATE', itemTypeName: 'Role entit', newValue: 'Plzeň' };

/**
 * A change blocked by a description rule carries the error the write would
 * fail with; the card shows it as any other server error and sends the same
 * text to the AI when the user asks it to clarify the change.
 */
describe('AiNodeUpdateProposalsBlock', () => {
    it('renders a blocked error as the server error message and quotes it when clarifying', () => {
        const onClarify = vi.fn();
        const change: AiProposalChange = {
            changeKey: '0/0/0',
            reason: 'Původce je Plzeň',
            state: 'BLOCKED',
            operations: [operation],
            blockedError: {
                type: 'RegistryCode',
                code: 'FOREIGN_ENTITY_INVALID_SUBTYPE',
                properties: { apName: 'Plzeň', itemName: 'Role entit: autoři', apTypeName: 'Geografický objekt' },
            },
        };

        renderWithProviders(<AiNodeUpdateProposalsBlock block={blockWith(change)} requestId={1} onClarify={onClarify} />);

        const message = screen.getByText(/Entitu „Plzeň“ nelze použít v prvku popisu „Role entit: autoři“/);
        expect(message.textContent).toContain('„Geografický objekt“');

        fireEvent.click(screen.getByRole('button', { name: /Upřesnit/ }));
        expect(onClarify).toHaveBeenCalledWith(message.textContent);
    });

    it('renders a plain blocked reason as text', () => {
        const change: AiProposalChange = {
            changeKey: '0/0/0',
            reason: 'Původce je Plzeň',
            state: 'BLOCKED',
            operations: [operation],
            blockedReason: 'Prvek popisu mezitím někdo změnil.',
        };

        renderWithProviders(<AiNodeUpdateProposalsBlock block={blockWith(change)} requestId={1} />);

        expect(screen.getByText('Prvek popisu mezitím někdo změnil.')).toBeInTheDocument();
    });
});
