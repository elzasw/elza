import { useEffect } from 'react';
import {
    Body1,
    Button,
    Caption1,
    Subtitle2,
    Tooltip,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import {
    ArrowDownloadRegular,
    DocumentDataRegular,
    DocumentPdfRegular,
    DocumentRegular,
    DocumentTextRegular,
    FolderZipRegular,
} from '@fluentui/react-icons';
import { defineMessages, useIntl } from 'react-intl';
import { fetchFundOutputFilesIfNeeded } from 'actions/arr/fundOutputFiles';
import { UrlFactory } from 'actions/index';
import { downloadFile } from 'actions/global/download';
import { useAppThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { objectById } from 'stores/app/utils';

const messages = defineMessages({
    title: {
        id: 'arr.output.files.title',
        defaultMessage: 'Vygenerované soubory',
    },
    downloadAll: {
        id: 'arr.output.files.downloadAll',
        defaultMessage: 'Stáhnout vše',
    },
    download: {
        id: 'arr.output.files.download',
        defaultMessage: 'Stáhnout soubor',
    },
    empty: {
        id: 'arr.output.files.empty',
        defaultMessage: 'Žádné soubory',
    },
});

const useStyles = makeStyles({
    root: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalS,
    },
    header: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        columnGap: tokens.spacingHorizontalS,
    },
    list: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalXS,
    },
    item: {
        display: 'flex',
        alignItems: 'center',
        columnGap: tokens.spacingHorizontalM,
        padding: tokens.spacingHorizontalS,
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground1,
        border: `1px solid ${tokens.colorNeutralStroke2}`,
        ':hover': {
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    fileIcon: {
        fontSize: '24px',
        color: tokens.colorNeutralForeground2,
        flexShrink: 0,
        display: 'flex',
    },
    fileInfo: {
        display: 'flex',
        flexDirection: 'column',
        flex: '1 1 auto',
        minWidth: 0,
    },
    fileName: {
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    fileMeta: {
        color: tokens.colorNeutralForeground3,
    },
    empty: {
        color: tokens.colorNeutralForeground3,
        padding: tokens.spacingVerticalS,
    },
});

interface OutputFile {
    id: number;
    name: string;
    fileName: string;
    mimeType: string;
    fileSize: number;
}

interface Props {
    outputId: number;
    versionId: number;
    outputResultIds: number[];
}

const getFileIcon = (mimeType: string) => {
    switch (mimeType) {
        case 'application/pdf':
            return <DocumentPdfRegular />;
        case 'application/zip':
            return <FolderZipRegular />;
        case 'text/plain':
            return <DocumentTextRegular />;
        case 'text/xml':
        case 'application/xml':
            return <DocumentDataRegular />;
        default:
            return <DocumentRegular />;
    }
};

const formatFileSize = (bytes: number): string => {
    if (bytes == null) {
        return '';
    }
    if (bytes < 1024) {
        return `${bytes} B`;
    }
    const units = ['kB', 'MB', 'GB'];
    let size = bytes / 1024;
    let unitIndex = 0;
    while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex += 1;
    }
    return `${size.toFixed(1)} ${units[unitIndex]}`;
};

const getExtension = (fileName: string) => fileName.slice(fileName.lastIndexOf('.') + 1).toUpperCase();

/**
 * Vylepšené zobrazení vygenerovaných souborů výstupu (Fluent). Alternativa ke starší
 * komponentě FundOutputFiles.jsx – načítá si data i akce sama z Reduxu.
 */
export function FundOutputFiles({ outputId, versionId, outputResultIds }: Props) {
    const styles = useStyles();
    const { formatMessage } = useIntl();
    const dispatch = useAppThunkDispatch();

    const fundOutputFiles = useAppSelector(({ arrRegion }: any) => {
        const fund = objectById<any, any>(arrRegion.funds, versionId, 'versionId');
        return fund?.fundOutput?.fundOutputFiles;
    });

    useEffect(() => {
        dispatch(fetchFundOutputFilesIfNeeded(versionId, outputId));
    }, [dispatch, versionId, outputId, fundOutputFiles?.currentDataKey]);

    const handleDownload = (fileId: number) => {
        dispatch(downloadFile(UrlFactory.downloadDmsFile(fileId)));
    };

    const handleDownloadAll = () => {
        if (outputResultIds.length === 1) {
            dispatch(downloadFile(UrlFactory.downloadOutputResult(outputResultIds[0])));
        } else {
            dispatch(downloadFile(UrlFactory.downloadOutputResults(outputId)));
        }
    };

    const files: OutputFile[] = fundOutputFiles?.fetched ? fundOutputFiles.data.rows : [];

    return (
        <div className={styles.root}>
            <div className={styles.header}>
                <Subtitle2>{formatMessage(messages.title)}</Subtitle2>
                {files.length > 1 && (
                    <Button
                        appearance="primary"
                        icon={<ArrowDownloadRegular />}
                        onClick={handleDownloadAll}
                    >
                        {formatMessage(messages.downloadAll)}
                    </Button>
                )}
            </div>

            {files.length === 0 ? (
                <Caption1 className={styles.empty}>{formatMessage(messages.empty)}</Caption1>
            ) : (
                <div className={styles.list}>
                    {files.map(file => (
                        <div key={file.id} className={styles.item}>
                            <span className={styles.fileIcon}>{getFileIcon(file.mimeType)}</span>
                            <span className={styles.fileInfo}>
                                <Body1 className={styles.fileName} title={file.name}>
                                    {file.name}
                                </Body1>
                                <Caption1 className={styles.fileMeta}>
                                    {[getExtension(file.fileName), formatFileSize(file.fileSize)]
                                        .filter(Boolean)
                                        .join(' · ')}
                                </Caption1>
                            </span>
                            <Tooltip relationship="label" content={formatMessage(messages.download)}>
                                <Button
                                    appearance="subtle"
                                    icon={<ArrowDownloadRegular />}
                                    aria-label={formatMessage(messages.download)}
                                    onClick={() => handleDownload(file.id)}
                                />
                            </Tooltip>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
