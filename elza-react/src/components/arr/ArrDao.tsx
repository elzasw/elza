import React, { useEffect, useState } from 'react';
import { defineMessages, MessageDescriptor, useIntl } from 'react-intl';
import { Icon, NoFocusButton } from 'components/shared';
import { globalMessages } from 'components/shared/lang/messages';
import { humanFileSize } from 'components/Utils.jsx';
import { showConfirmDialog } from 'components/shared/dialog';
import { addToastrInfo } from 'components/shared/toastr/ToastrActions';
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog';
import { WebApi } from 'actions/index.jsx';
import { useAppThunkDispatch } from 'utils/hooks';
import { ArrDaoFileVO, ArrDaoVO } from 'typings/dao';
import { Fund } from 'typings/store';
import { Button } from '../ui';
import ArrRequestForm from './ArrRequestForm';
import NodeLabel from './NodeLabel';

import './ArrDao.scss';

const messages = defineMessages({
    unlinkConfirm: {
        id: 'arrDao.unlink.confirm',
        defaultMessage: 'Opravdu chcete odpojit digitální entitu od jednotky popisu?',
    },
    unlinkAction: {
        id: 'arrDao.unlink.action',
        defaultMessage: 'Odpojit',
    },
    requestTitle: {
        id: 'arrDao.request.title',
        defaultMessage: 'Požadavek na delimitaci/skartaci',
    },
    openInRepository: {
        id: 'arrDao.action.openInRepository',
        defaultMessage: 'Otevřít v uložišti',
    },
    copyLink: {
        id: 'arrDao.action.copyLink',
        defaultMessage: 'Zkopírovat odkaz',
    },
    linkedTo: {
        id: 'arrDao.linkedTo',
        defaultMessage: 'Připojeno k JP:',
    },
    labelScenario: {
        id: 'arrDao.label.scenario',
        defaultMessage: 'Scénář',
    },
    labelId: {
        id: 'arrDao.label.id',
        defaultMessage: 'ID',
    },
    labelCode: {
        id: 'arrDao.label.code',
        defaultMessage: 'Kód',
    },
    labelFileCount: {
        id: 'arrDao.label.fileCount',
        defaultMessage: 'Počet souborů',
    },
    thumbnailPosition: {
        id: 'arrDao.thumbnail.position',
        defaultMessage: 'Náhled {current}/{total}',
    },
    thumbnailEmpty: {
        id: 'arrDao.thumbnail.empty',
        defaultMessage: 'Náhled není k dispozici',
    },
    thumbnailNotFound: {
        id: 'arrDao.thumbnail.notFound',
        defaultMessage: 'Prvek nebyl nalezen v úložišti',
    },
    fileSelected: {
        id: 'arrDao.file.selected',
        defaultMessage: 'Vybraný soubor',
    },
    fileMimeType: {
        id: 'arrDao.file.mimeType',
        defaultMessage: 'MIME',
    },
    fileSize: {
        id: 'arrDao.file.size',
        defaultMessage: 'Velikost',
    },
    fileDuration: {
        id: 'arrDao.file.duration',
        defaultMessage: 'Délka',
    },
    fileResolution: {
        id: 'arrDao.file.resolution',
        defaultMessage: 'Rozlišení',
    },
    fileSourceResolution: {
        id: 'arrDao.file.sourceResolution',
        defaultMessage: 'Zdrojové rozlišení',
    },
    fileDescription: {
        id: 'arrDao.file.description',
        defaultMessage: 'Popis',
    },
    unitIn: {
        id: 'arrDao.unit.in',
        defaultMessage: 'in',
    },
    unitMm: {
        id: 'arrDao.unit.mm',
        defaultMessage: 'mm',
    },
});

/** Jednotky, ve kterých server posílá zdrojové rozměry (cz.tacr.elza.api.UnitOfMeasure). */
const unitMessages: Record<string, MessageDescriptor> = {
    IN: messages.unitIn,
    MM: messages.unitMm,
};

interface Props {
    dao: ArrDaoVO;
    fund: Fund;
    readMode: boolean;
    /** Vybraný soubor DAO; bez něj se ukáže první soubor entity. */
    daoFile?: ArrDaoFileVO;
    /** Přepnutí náhledu na jiný soubor entity. */
    onSelectFile?: (daoFileId: number) => void;
    onUnlink: () => void;
}

export type ArrDaoProps = Props;

const renderLabel = (label: string, value: React.ReactNode, block = false) => {
    const cls = block ? 'lbl block' : 'lbl';
    const val = <span className="lbl-value">{value}</span>;
    const valFinal = block ? <div className="scrollable">{val}</div> : val;
    return (
        <div title={label + ': ' + value} className={cls}>
            <span className="lbl-name">{label}</span>
            {valFinal}
        </div>
    );
};

export function ArrDao({ dao, fund, readMode, daoFile, onSelectFile, onUnlink }: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const [imageError, setImageError] = useState(false);

    const fileList = dao.fileList || [];
    // Bez vybraného souboru ukazujeme první — detail entity tak nikdy není bez náhledu.
    const file = daoFile ?? fileList[0];

    // Náhled smazaného souboru se nedá stáhnout — 404 na HEAD přepne zobrazení na
    // vysvětlení místo rozbitého obrázku. Síťová chyba soubor za chybějící neoznačí.
    useEffect(() => {
        setImageError(false);
        const url = file?.url;
        if (!url) {
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const res = await fetch(url, { method: 'HEAD' });
                if (!cancelled && res.status === 404) {
                    setImageError(true);
                }
            } catch (e) {
                // Bez odpovědi serveru nevíme nic — stav ponecháme.
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [file?.url]);

    const handleUnlink = async () => {
        const confirmed = await dispatch(
            showConfirmDialog(
                intl.formatMessage(messages.unlinkConfirm),
                undefined,
                intl.formatMessage(messages.unlinkAction),
            ),
        );
        if (confirmed) {
            onUnlink();
        }
    };

    const handleTrash = () => {
        const form = (
            <ArrRequestForm
                fundVersionId={fund.versionId}
                type="DAO"
                onSubmitForm={(send: boolean, data: any) =>
                    WebApi.arrDaoRequestAddDaos(
                        fund.versionId,
                        data.requestId,
                        send,
                        data.description,
                        [dao.id],
                        data.daoType,
                    )
                }
                onSubmitSuccess={() => dispatch(modalDialogHide())}
            />
        );
        dispatch(modalDialogShow(null, intl.formatMessage(messages.requestTitle), form));
    };

    const copyToClipboard = async (url: string) => {
        // ponechej adresu zacinajici na http(s), jinak dopln hosta pred url + osetreni lomitka na zacatku
        const fullPath = /^https?:\/\//.test(url) ? url : `${location.host}/${url.replace(/^\//, '')}`;
        await navigator.clipboard.writeText(fullPath);
        dispatch(addToastrInfo(intl.formatMessage(globalMessages.copyToClipboardFinished)));
    };

    const formatUnit = (unit?: string) => (unit && unitMessages[unit] ? intl.formatMessage(unitMessages[unit]) : '');

    const renderDaoDetail = () => (
        <div className="dao-detail">
            <div key="actions" className="dao-actions">
                {dao.url && (
                    <div key="left" className="left">
                        <a target="_blank" rel="noopener noreferrer" href={dao.url}>
                            <NoFocusButton>
                                <Icon glyph="fa-external-link" />
                                {intl.formatMessage(messages.openInRepository)}
                            </NoFocusButton>
                        </a>
                        <NoFocusButton onClick={() => copyToClipboard(dao.url as string)}>
                            <Icon glyph="fa-paste" />
                            {intl.formatMessage(messages.copyLink)}
                        </NoFocusButton>
                    </div>
                )}
                {!readMode && (
                    <div key="right" className="right">
                        <Button variant="action" disabled={!dao.daoLink} onClick={handleUnlink}>
                            <Icon glyph="fa-unlink" />
                        </Button>
                        {/* fs položky (syntetické záporné id) nemají ArrDao — požadavek nelze založit */}
                        {dao.id > 0 && (
                            <Button variant="action" onClick={handleTrash} disabled={dao.existInArrDaoRequest}>
                                <Icon glyph="fa-trash" />
                            </Button>
                        )}
                    </div>
                )}
            </div>
            <div key="info" className="dao-info">
                {renderLabel(intl.formatMessage(messages.labelId), dao.id)}
                {renderLabel(intl.formatMessage(messages.labelCode), dao.code, true)}
                {renderLabel(
                    intl.formatMessage(messages.labelFileCount),
                    fileList.length + (dao.truncated ? '+' : ''),
                )}
                {dao.daoLink?.scenario &&
                    renderLabel(intl.formatMessage(messages.labelScenario), dao.daoLink.scenario)}
            </div>
        </div>
    );

    const renderThumbnail = (file: ArrDaoFileVO) => {
        const isImage = file.mimetype && file.mimetype.startsWith('image/');
        const hasThumbnail = isImage && file.thumbnailUrl && !imageError;
        const cls = hasThumbnail ? 'thumbnail' : 'thumbnail empty';

        let img: React.ReactNode;
        if (hasThumbnail) {
            img = (
                <>
                    <img className="img-blur" src={file.thumbnailUrl} alt="" />
                    <img src={file.thumbnailUrl} alt="" onError={() => setImageError(true)} />
                </>
            );
        } else if (imageError) {
            img = (
                <div className="empty-img error">
                    <Icon glyph="fa-exclamation-triangle" />
                    <div className="message">{intl.formatMessage(messages.thumbnailNotFound)}</div>
                </div>
            );
        } else {
            img = (
                <div className="empty-img no-preview">
                    <Icon glyph="fa-eye-slash" />
                    <div className="message">{intl.formatMessage(messages.thumbnailEmpty)}</div>
                </div>
            );
        }

        const title = imageError
            ? intl.formatMessage(messages.thumbnailNotFound)
            : hasThumbnail
                ? file.thumbnailUrl
                : intl.formatMessage(messages.thumbnailEmpty);

        return (
            <div title={title} className={cls}>
                {img}
            </div>
        );
    };

    const renderDaoFileDetail = (file: ArrDaoFileVO) => {
        const count = fileList.length;
        const index = fileList.indexOf(file);
        const curr = index + 1;
        const stepFile = (step: number) => {
            const next = fileList[index + step];
            if (next) {
                onSelectFile?.(next.id);
            }
        };

        return (
            <div className="dao-file-detail">
                {file.thumbnailUrl && (
                    <div className="dao-file-thumbnail">
                        <div className="navigation">
                            <div className="title">
                                {intl.formatMessage(messages.thumbnailPosition, { current: curr, total: count })}
                            </div>
                            <div className="arrows">
                                <NoFocusButton disabled={curr <= 1} onClick={() => stepFile(-1)}>
                                    <Icon glyph="fa-chevron-left" />
                                </NoFocusButton>
                                <NoFocusButton disabled={curr >= count} onClick={() => stepFile(1)}>
                                    <Icon glyph="fa-chevron-right" />
                                </NoFocusButton>
                            </div>
                        </div>
                        {renderThumbnail(file)}
                    </div>
                )}
                <div className="dao-file-info">
                    <div className="dao-file-info-title">
                        <div className="title">{intl.formatMessage(messages.fileSelected)}</div>
                        <div className="spacer" />
                        <div className="actions">
                            <a
                                title={intl.formatMessage(messages.openInRepository)}
                                target="_blank"
                                rel="noopener noreferrer"
                                href={file.url}
                            >
                                <NoFocusButton>
                                    <Icon glyph="fa-external-link" />
                                </NoFocusButton>
                            </a>
                            <NoFocusButton
                                title={intl.formatMessage(messages.copyLink)}
                                onClick={() => copyToClipboard(file.url as string)}
                            >
                                <Icon glyph="fa-paste" />
                            </NoFocusButton>
                        </div>
                    </div>
                    <div className="dao-file-info-base">
                        <span className="file">{file.code}</span>
                        {file.mimetype && renderLabel(intl.formatMessage(messages.fileMimeType), file.mimetype)}
                        {file.size && renderLabel(intl.formatMessage(messages.fileSize), humanFileSize(file.size))}
                        {file.duration && renderLabel(intl.formatMessage(messages.fileDuration), file.duration)}
                        {file.imageWidth &&
                            file.imageHeight &&
                            renderLabel(
                                intl.formatMessage(messages.fileResolution),
                                file.imageWidth + ' x ' + file.imageHeight + ' px',
                            )}
                        {file.sourceXDimesionValue &&
                            file.sourceYDimesionValue &&
                            renderLabel(
                                intl.formatMessage(messages.fileSourceResolution),
                                file.sourceXDimesionValue +
                                    formatUnit(file.sourceXDimesionUnit) +
                                    ' x ' +
                                    file.sourceYDimesionValue +
                                    formatUnit(file.sourceYDimesionUnit),
                            )}
                        {file.description &&
                            renderLabel(intl.formatMessage(messages.fileDescription), file.description)}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="dao-container">
            {dao.daoLink && (
                <div key="jp-panel" className="dao-jp-panel">
                    {intl.formatMessage(messages.linkedTo)} <NodeLabel inline node={dao.daoLink.treeNodeClient} />
                </div>
            )}
            <div className="dao-info-container">
                {renderDaoDetail()}
                {file && renderDaoFileDetail(file)}
            </div>
        </div>
    );
}
