import { describe, it, expect } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { enMessages, expectTranslated } from 'test/i18n-catalog';
import { server } from 'test/mocks/server';
import ApField from './ApField';

/**
 * Popisky způsobů vyhledávání jsou deskriptory react-intl vybírané za běhu
 * podle stavu. Dokud se nepředávaly <FormattedMessage>, React na vykreslení
 * objektu padal (chyba #31) a shodil celý dialog "Vytvoření uživatele".
 */
describe('ApField', () => {
    const searchTypeIds = [
        'apField.searchType.USERNAME',
        'apField.searchType.USERNAME_AND_PARTY',
        'apField.searchType.PARTY_RIGHT_LIKE',
        'apField.searchType.PARTY_FULLTEXT',
        'apField.searchType.create.FULLTEXT',
        'apField.searchType.create.RIGHT_LIKE',
    ];

    beforeEach(() => {
        server.use(http.get('/api/registry/recordTypes', () => HttpResponse.json([])));
    });

    it('vykreslí popisek zvoleného způsobu vyhledávání místo deskriptoru', async () => {
        renderWithProviders(<ApField onChange={() => undefined} />);

        expect(await screen.findByText('vyhledání dle username')).toBeInTheDocument();
    });

    it('rozbalená nabídka nese popisky všech způsobů vyhledávání', async () => {
        renderWithProviders(<ApField onChange={() => undefined} />);

        fireEvent.click(await screen.findByRole('button', { name: 'vyhledání dle username' }));

        for (const label of [
            'vyhledání dle username i dle osoby',
            'vyhledání dle osoby - pravostranné',
            'vyhledání dle osoby - fulltext',
        ]) {
            expect(await screen.findByRole('button', { name: label })).toBeInTheDocument();
        }
    });

    it('v režimu založení začíná na pravostranném hledání', async () => {
        renderWithProviders(<ApField isCreate onChange={() => undefined} />);

        expect(await screen.findByText('pravostranné hledání')).toBeInTheDocument();
    });

    it('má anglický překlad pro všechny způsoby vyhledávání', () => {
        expectTranslated(searchTypeIds);
    });

    it('vykreslí anglický popisek, když je zvolena angličtina', async () => {
        renderWithProviders(<ApField onChange={() => undefined} />, {
            locale: 'en',
            messages: enMessages,
        });

        expect(
            await screen.findByText(enMessages['apField.searchType.USERNAME']),
        ).toBeInTheDocument();
    });
});
