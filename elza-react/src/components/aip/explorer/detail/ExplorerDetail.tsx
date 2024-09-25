import i18n from "components/i18n";
import { FC, useEffect } from "react";
import "./ExplorerDetail.scss";
import { Button,  } from "@fluentui/react-components";
import {  findNodeByUUID,  } from "../utils";
import {  useExplorerContext } from "../ExplorerContext";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {AREA_AIP, aipFetchIfNeeded, aipsFetchIfNeeded} from "actions/aip/aip";
import { useThunkDispatch } from "utils/hooks";
import {getConnectedToJP} from "../../utils.tsx";
import {Api} from "../../../../api";
import {fetchAipStructureIfNeeded} from "../../../../actions/aip/exp.ts";

const ExplorerDetail: FC = () => {
    const {selectedItem, setSelectedItem, structure} = useExplorerContext();
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const dispatch = useThunkDispatch();

    const fetchData = () => {
        dispatch(aipFetchIfNeeded(aip.id));
    }

    useEffect(() => {
        fetchData()
    }, [aip.id]);

    console.log('aip.data :>> ', aip.data);


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

    const selectFolder = (id) => {
        const {node} = findNodeByUUID(structure, id);
        setSelectedItem(node);
    }

    const renderValue = (value: string) => {
        if (!value || value == "") {
            return "-";
        }
        return value;
    }

    const renderName = () => {
        return selectedItem.label || selectedItem.fileName;
    }

    if(!selectedItem) {
        return <p>Nebyl vybrám žádný objekt</p>
    }
    const renderRepresentationParent = () => {
        return (
            <span>
                <a className="detail-item" onClick={() => selectFolder(selectedItem.parentFolder.uuid)}>
                    {renderValue(selectedItem.parentFolder?.label)}
                </a>
            </span>
        );
    }

    const renderLogicalParent = () => {
        return (
            <span>
                <a className="detail-item" onClick={() => selectFolder(selectedItem.parentFolderLogical.uuid)}>
                    {renderValue(selectedItem.parentFolderLogical?.label)}
                </a>
            </span>
        );
    }

    const handleDeleteLink = (linkId: number) => {
        Api.aips.aipDeleteDaoLink(linkId).then(() => {
            dispatch(fetchAipStructureIfNeeded(aip.id, true));
        });
    }

    const handleDownloadComponent = (daoFileid: number) => {
        // Api.aips.aipDownloadComponent(daoFileid).then(url => {
        //     dispatch(downloadFile(getFullPath(url)));
        // });
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
                    value={renderValue(selectedItem.checksumType)}
                />
                <DetailRow
                    label={i18n("aip.explorer.detail.format")}
                    value={renderValue(selectedItem.mimeType)}
                />
                <DetailRow
                    label={i18n("aip.explorer.detail.as")}
                    // @ts-ignore
                    value={getConnectedToJP(selectedItem.linkedNodes, aip.data?.fund.id, handleDeleteLink)}
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
                {selectedItem.daoFileId && aip.data.completeAipLoad && <Button
                    as="a"
                    className="open-btn"
                    onClick={() => handleDownloadComponent(selectedItem.daoFileId)}
                    size="small"
                    shape="square"
                >
                    <span>Stáhnout</span>
                </Button>}
            </div>

            <h4>{i18n("aip.explorer.detail.title")}</h4>
            <div className="explorer-detail-body">
                {selectedItem && renderFileData()}
            </div>

            <div>
                <h4>Vztahy - reprezentace</h4>
                {/* <p><b>{i18n("aip.explorer.detail.parent")} </b>{renderParent()}</p> */}
                <p><b>{i18n("aip.explorer.detail.parent")} </b> {selectedItem.parentFolder ? renderRepresentationParent() : "-"}</p>
                {/* <p><b>Potomci </b>{renderChildren()}</p> */}
                <h4>Vztahy - logická struktura</h4>
                <p><b>{i18n("aip.explorer.detail.parent")} </b>{ selectedItem.parentFolderLogical ? renderLogicalParent() : "-"}</p>
            </div>
        </div>
    );
}

export default ExplorerDetail;
