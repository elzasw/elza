import React, { FC } from "react";
import { Button } from "../../ui";
//@ts-ignore
import { Icon } from "components/shared";
import { defineMessages, useIntl } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny; apostrofy kolem placeholderu
// nahrazeny českými uvozovkami, jinak by ICU hodnotu nedosadilo.
const messages = defineMessages({
    downloadFile: { id: 'global.action.download.file', defaultMessage: 'Stáhnout soubor „{0}“' },
});
import { File } from "./types";

export const FileGridItem:FC<{
    file: File,
    onDownload: (fileId: number) => void,
}> = ({
    file,
    onDownload,
}) => {
    const intl = useIntl();

    const handleDownload = () => {
        onDownload && onDownload(file.id);
    }

    const getFileIcon = (type: string) => ({
            'application/pdf': 'fa-file-pdf-o',
            'application/zip': 'fa-file-zip-o',
            'text/plain': 'fa-file-text-o',
            'text/xml': 'fa-file-code-o',
        }[type] || 'fa-file-o')

    const getFileExtension = (fileName: string) => 
        fileName.substr(fileName.lastIndexOf('.') + 1)

    return <div className="file-grid-item">
        <div className="file-icon" style={{marginRight: "10px"}}>
            <Icon glyph={getFileIcon(file.mimeType)}/>
        </div>
        <div className="file-description-container">
            <div className="file-name" title={file.name}>{file.name}</div>
            <div className="file-file-name">{getFileExtension(file.fileName).toUpperCase()}</div>
            {/*<div className="file-file-name">{getFileType(file.mimeType)}</div>*/}
        </div>
        {onDownload && 
            <div className="file-actions-overlay">
                <div className="file-actions-container">
                    <Button 
                        variant="action" 
                        className="file-action-button" 
                        onClick={handleDownload}
                        title={intl.formatMessage(messages.downloadFile, { 0: file.fileName })}
                    >
                        <Icon
                            glyph="fa-download"
                        />
                    </Button>
                </div>
            </div>
        }
    </div>
}
