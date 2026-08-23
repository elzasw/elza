import { describe, it, expect, vi, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { server } from 'test/mocks/server';
import ModalDialog from 'components/shared/dialog/ModalDialog';
import { ArrDaoFileVO, ArrDaoVO } from 'typings/dao';
import { Fund } from 'typings/store';
import { ArrDao } from './ArrDao';

/**
 * Detail digitální entity: odpojení je za potvrzením a náhled nedostupného
 * souboru se místo rozbitého obrázku vysvětlí.
 *
 * Vlastní texty komponenty přicházejí z jejích `defaultMessage`. Potvrzovací
 * dialog (MultiButtonDialog) je zatím na starém `i18n()` nad `window.messages`,
 * proto se jeho popisky musí naplnit ručně.
 */

const UNLINK_CONFIRM = 'Opravdu chcete odpojit digitální entitu od jednotky popisu?';
const THUMBNAIL_NOT_FOUND = 'Prvek nebyl nalezen v úložišti';

const legacyDialogMessages: Record<string, string> = {
    'confirmDialog.default.title': 'Potvrzení',
    'global.action.store': 'Ok',
    'global.action.cancel': 'Storno',
};

const fund = { versionId: 1 } as Fund;

const daoFile: ArrDaoFileVO = {
    id: 10,
    code: 'file-10',
    mimetype: 'image/jpeg',
    url: '/api/dao/file/10',
    thumbnailUrl: '/api/dao/file/10/thumb',
};

const dao: ArrDaoVO = {
    id: 5,
    code: 'dao-5',
    fileCount: 1,
    fileList: [daoFile],
    daoLink: { id: 7, treeNodeClient: { id: 30, version: 1, name: 'JP 30', referenceMark: ['1'] } },
};

beforeEach(() => {
    (window as unknown as { messages: Record<string, string> }).messages = legacyDialogMessages;
});

describe('ArrDao', () => {
    it('odpojí digitální entitu až po potvrzení', async () => {
        const onUnlink = vi.fn();

        renderWithProviders(
            <>
                <ArrDao dao={dao} fund={fund} readMode={false} onUnlink={onUnlink} />
                <ModalDialog />
            </>,
        );

        fireEvent.click(document.querySelector('.dao-actions .right button') as HTMLElement);

        // Dotaz se ptá dřív, než se cokoli odpojí.
        await screen.findByText(UNLINK_CONFIRM);
        expect(onUnlink).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', { name: 'Odpojit' }));

        await vi.waitFor(() => expect(onUnlink).toHaveBeenCalledTimes(1));
    });

    it('storno v potvrzení nechá vazbu být', async () => {
        const onUnlink = vi.fn();

        renderWithProviders(
            <>
                <ArrDao dao={dao} fund={fund} readMode={false} onUnlink={onUnlink} />
                <ModalDialog />
            </>,
        );

        fireEvent.click(document.querySelector('.dao-actions .right button') as HTMLElement);
        fireEvent.click(await screen.findByRole('button', { name: 'Storno' }));

        await vi.waitFor(() => expect(screen.queryByText(UNLINK_CONFIRM)).not.toBeInTheDocument());
        expect(onUnlink).not.toHaveBeenCalled();
    });

    it('v režimu čtení odpojení nenabízí', () => {
        renderWithProviders(<ArrDao dao={dao} fund={fund} readMode onUnlink={vi.fn()} />);

        expect(document.querySelector('.dao-actions .right')).toBeNull();
    });

    it('u smazaného souboru zobrazí místo náhledu vysvětlení', async () => {
        server.use(http.head('/api/dao/file/10', () => new HttpResponse(null, { status: 404 })));

        renderWithProviders(<ArrDao dao={dao} fund={fund} readMode daoFile={daoFile} onUnlink={vi.fn()} />);

        await screen.findByText(THUMBNAIL_NOT_FOUND);
        expect(document.querySelector('.thumbnail img')).toBeNull();
    });

    it('dostupný soubor náhled zobrazí', async () => {
        server.use(http.head('/api/dao/file/10', () => new HttpResponse(null, { status: 200 })));

        renderWithProviders(<ArrDao dao={dao} fund={fund} readMode daoFile={daoFile} onUnlink={vi.fn()} />);

        await vi.waitFor(() =>
            expect(document.querySelector('.thumbnail img')).toHaveAttribute('src', daoFile.thumbnailUrl as string),
        );
        expect(screen.queryByText(THUMBNAIL_NOT_FOUND)).not.toBeInTheDocument();
    });

    it('zdrojové rozměry popíše jednotkou ze serveru', () => {
        const file: ArrDaoFileVO = {
            ...daoFile,
            thumbnailUrl: undefined,
            sourceXDimesionValue: 210,
            sourceXDimesionUnit: 'MM',
            sourceYDimesionValue: 297,
            sourceYDimesionUnit: 'MM',
        };

        renderWithProviders(
            <ArrDao
                dao={{ ...dao, fileList: [file] }}
                fund={fund}
                readMode
                daoFile={file}
                onUnlink={vi.fn()}
            />,
        );

        expect(screen.getByText('210mm x 297mm')).toBeInTheDocument();
    });
});
