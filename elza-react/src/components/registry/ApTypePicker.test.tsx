import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';

import { fireEvent, renderWithProviders, screen, waitFor } from 'test/test-utils';
import { server } from 'test/mocks/server';
import { ApTypePicker } from './ApTypePicker';
import { ApTypeVO } from 'api/ApTypeVO';

/**
 * The class picker shows the classes the rule set of the scope offers: a root that cannot be assigned
 * there and its assignable subclass.
 */

const tree = [
    {
        id: 1,
        code: 'PERSON',
        name: 'Osoba',
        addRecord: false,
        children: [{ id: 2, code: 'PERSON_INDIVIDUAL', name: 'Fyzická osoba', addRecord: true, children: [] as ApTypeVO[] }],
    },
];

const serveTree = () => {
    const scopeIds: (string | null)[] = [];
    server.use(
        http.get('/api/registry/recordTypes', ({ request }) => {
            scopeIds.push(new URL(request.url).searchParams.get('scopeId'));
            return HttpResponse.json(tree);
        }),
    );
    return scopeIds;
};

describe('ApTypePicker', () => {
    it('is disabled until a scope is chosen', () => {
        renderWithProviders(<ApTypePicker label="Podtřída" onChange={vi.fn()} />);

        expect(screen.getByRole('combobox')).toBeDisabled();
        expect(screen.getByPlaceholderText('Nejprve vyberte oblast')).toBeInTheDocument();
    });

    it('offers the classes of the scope and lets choose only assignable ones', async () => {
        const scopeIds = serveTree();
        const onChange = vi.fn();
        renderWithProviders(<ApTypePicker label="Podtřída" scopeId={7} onChange={onChange} />);

        await waitFor(() => expect(scopeIds).toEqual(['7']));
        const combobox = screen.getByRole('combobox');
        await waitFor(() => expect(combobox).toBeEnabled());
        fireEvent.click(combobox);

        const root = await screen.findByRole('option', { name: 'Osoba' });
        expect(root).toHaveAttribute('aria-disabled', 'true');
        fireEvent.click(root);
        expect(onChange).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('option', { name: 'Fyzická osoba' }));
        expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ id: 2, code: 'PERSON_INDIVIDUAL' }));
    });

    it('filters the classes by the typed name', async () => {
        serveTree();
        renderWithProviders(<ApTypePicker label="Podtřída" scopeId={7} onChange={vi.fn()} />);

        const combobox = screen.getByRole('combobox');
        await waitFor(() => expect(combobox).toBeEnabled());
        fireEvent.change(combobox, { target: { value: 'fyz' } });
        fireEvent.click(combobox);

        expect(await screen.findByRole('option', { name: 'Fyzická osoba' })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: 'Osoba' })).toBeNull();
    });

    it('clears a class the scope does not allow', async () => {
        serveTree();
        const onChange = vi.fn();
        renderWithProviders(<ApTypePicker label="Podtřída" scopeId={7} value={99} onChange={onChange} />);

        await waitFor(() => expect(onChange).toHaveBeenCalledWith(undefined));
    });
});
