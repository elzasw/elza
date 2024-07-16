import i18n from "components/i18n";
import { FC } from "react";
import "./ExplorerDetail.scss";
import { Button } from "@fluentui/react-components";
import { getFileName } from "../utils";
import { isDaoFileFolderVO, useExplorerContext } from "../ExplorerContext";

const ExplorerDetail: FC = () => {
    const {selectedItem, setSelectedItem} = useExplorerContext();

    const DetailRow = ({label, value}: {label: string, value?: any}) => (
        <div className="item-row">
            <div className="label col">
                <b>{label}</b>
            </div>
            {value && <div className="value col">
                {value}
            </div>
            }
        </div>
    );

    const selectFolder = (folder: any) => {
        setSelectedItem(folder);
    }

    const renderParent = () => {
        if(selectedItem?.parent) {
            return (
                <span>
                    <a className="detail-item" onClick={() => selectFolder(selectedItem.parent)}>
                        {renderValue(selectedItem.parent?.label)}
                    </a>
                </span>
            );
        }
        return "-"
    }

    const renderChildren = () => {
        if(isDaoFileFolderVO(selectedItem) && !selectedItem?.childFolders) return "-";
        console.log("asdasd",selectedItem)
        return (
            <>
                {isDaoFileFolderVO(selectedItem) && selectedItem.childFolders?.map((folder, index) => 
                    <span>
                        <a className="detail-item" onClick={() => selectFolder(folder)}>{folder.label}</a>
                        {index != selectedItem?.childFolders.length - 1 && ", "}
                    </span>
                )}
            </>
        );
    }

    const renderValue = (value: string) => {
        if (!value || value == "") {
            return "-";
        }
        return value;
    }

    const renderName = () => {
        return renderValue(isDaoFileFolderVO(selectedItem) ? selectedItem.label : getFileName(selectedItem.fileName));
    }

    if(!selectedItem) {
        return <p>Nebyl vybrám žádný objekt</p>
    }

    const renderFileData = () => {
        return (
            <div className="explorer-detail-body">
                <DetailRow 
                    label={i18n("aip.explorer.detail.name")} 
                    value={renderName()}
                />
                <DetailRow 
                    label={i18n("aip.explorer.detail.checksum")} 
                    value={renderValue(isDaoFileFolderVO(selectedItem) ? null : selectedItem.checksumType)}
                />
                <DetailRow 
                    label={i18n("aip.explorer.detail.format")} 
                    value={renderValue(isDaoFileFolderVO(selectedItem) ? null : selectedItem.mimeType)}
                />
                {/* TODO: @kasparova */}
                <DetailRow 
                    label={i18n("aip.explorer.detail.as")} 
                    // @ts-ignore
                    value={renderValue(isDaoFileFolderVO(selectedItem) ? null : selectedItem.as)}
                />
            </div>
        );
    }

    return (
        <div className="explorer-detail">
            <div className="buttons">
                <Button
                    as="a"
                    className="open-btn"
                    onClick={() => {}}
                    size="small"
                    shape="square"
                >
                    <span>Zobrazit</span>
                </Button>
                <Button
                    as="a"
                    className="open-btn"
                    onClick={() => {}}
                    size="small"
                    shape="square"
                >
                    <span>Stáhnout</span>
                </Button>
            </div>
            
            <h4>{i18n("aip.explorer.detail.title")}</h4>
            <div className="explorer-detail-body">
                {selectedItem && renderFileData()}
            </div> 

            {/* <div className="explorer-detail">
                <h4>Vztahy - reprezentace</h4>
                <p><b>{i18n("aip.explorer.detail.parent")} </b>{renderParent()}</p>
                <p><b>Potomci </b>{renderChildren()}</p>
            </div> */}
        </div>
    );
}

export default ExplorerDetail;