import i18n from "components/i18n";
import { FC, useEffect, useState } from "react";
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
import {Api, getFullPath} from "../../../../api";
import {fetchAipStructureIfNeeded} from "../../../../actions/aip/exp.ts";
import {downloadFile} from "../../../../actions/global/download";
import {AipsApiAxiosParamCreator} from "elza-api";
import { AREA_AIP_STRUCTURE } from "actions/aip/exp.ts";
import CrossTabHelper, { CrossTabEventType, getThisLayout } from "../../../CrossTabHelper";
import {WebApi} from 'actions/WebApi';
import { DaoViewRequestInfoVO } from "api/DaoViewRequestInfoVO.ts";

const ExplorerDetail: FC<{selected?: string;}> = ({selected}) => {
    const {selectedItem, setSelectedItem} = useExplorerContext();
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const structure = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const dispatch = useThunkDispatch();
    const [node, setNode] = useState(selectedItem);

    const fetchData = () => {
        dispatch(aipFetchIfNeeded(aip.id));
    }

    useEffect(() => {
        fetchData()
    }, [aip.id]);

    useEffect(() => {
        if(!structure?.data) return;

        if(!selectedItem) {
            setSelectedItem(structure.data);
        } else {
            const result = findNodeByUUID(structure.data, selectedItem.uuid);
            if(result && result.node) {
                setSelectedItem(result.node);
            }
        }
        if (structure) {
            structure.parent = null;
        }
    }, [structure]);

    useEffect(() => {
        if(selectedItem) {
            setNode(selectedItem)
        }
    }, [selectedItem]);

    useEffect(() => {
        if (selected && structure.data) {
            const result = findNodeByUUID(structure.data, selected);
            if (result && result.node) {
                setSelectedItem(result.node);
            }
        }
    }, [structure.data, selected]);


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
        const result = findNodeByUUID(structure.data, id);
        if(result) {
            setSelectedItem(result.node);
        }
    }

    const renderValue = (value: string) => {
        if (!value || value == "") {
            return "-";
        }
        return value;
    }

    const renderName = () => {
        return node.label || node.filename;
    }

    if(!node) {
        return <p>Nebyl vybrám žádný objekt</p>
    }
    const renderRepresentationParent = () => {
        return (
            <span>
                <a className="detail-item" onClick={() => selectFolder(node.parentFolder.uuid)}>
                    {renderValue(node.parentFolder?.label)}
                </a>
            </span>
        );
    }

    const renderLogicalParent = () => {
        return (
            <span>
                <a className="detail-item" onClick={() => selectFolder(node.parentFolderLogical.uuid)}>
                    {renderValue(node.parentFolderLogical?.label)}
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
        AipsApiAxiosParamCreator().aipDownloadComponent(daoFileid).then(response => {
            const { url } = response;
            dispatch(downloadFile(getFullPath(url)));
        });
    }

    const handleOpenComponent = () => {
        const thisLayout = getThisLayout();

        WebApi.getDaoViewRequestInfo(selectedItem.daoId).then((result: DaoViewRequestInfoVO) => {
            if (thisLayout) {
                CrossTabHelper.sendEvent(
                    thisLayout, {
                        type: CrossTabEventType.DISPLAY_COMPONENT,
                        data: {
                            viewUrl: result.viewUrl,
                            request: {
                                type: "ViewRequest",
                                daoId: result.daoId,
                                entityRef: result.entityRef
                            }
                        }}
                    );
                }
            }
        )
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
                    value={renderValue(node.checksumType)}
                />
                <DetailRow
                    label={i18n("aip.explorer.detail.format")}
                    value={renderValue(node.mimeType)}
                />
                {aip.data?.fund &&
                    <DetailRow
                    label={i18n("aip.explorer.detail.as")}
                    // @ts-ignore
                    value={getConnectedToJP(node.linkedNodes, aip.data?.fund.id, handleDeleteLink)}
                    />}
            </div>
        );
    }

    return (
        <div className="explorer-detail">
            <div className="buttons">
                {node.daoFileId && aip.data.metadataLoad && <Button
                    as="a"
                    className="open-btn"
                    onClick={handleOpenComponent}
                    size="small"
                    shape="square"
                >
                    <span>Zobrazit</span>
                </Button>}
                {node.daoFileId && aip.data.completeAipLoad && <Button
                    as="a"
                    className="open-btn"
                    onClick={() => handleDownloadComponent(node.daoFileId)}
                    size="small"
                    shape="square"
                >
                    <span>Stáhnout</span>
                </Button>}
            </div>

            <h4>{i18n("aip.explorer.detail.title")}</h4>
            <div className="explorer-detail-body">
                {node && renderFileData()}
            </div>

            <div>
                <h4>Vztahy - reprezentace</h4>
                {/* <p><b>{i18n("aip.explorer.detail.parent")} </b>{renderParent()}</p> */}
                <p><b>{i18n("aip.explorer.detail.parent")} </b> {node.parentFolder ? renderRepresentationParent() : "-"}</p>
                {/* <p><b>Potomci </b>{renderChildren()}</p> */}
                <h4>Vztahy - logická struktura</h4>
                <p><b>{i18n("aip.explorer.detail.parent")} </b>{ node.parentFolderLogical ? renderLogicalParent() : "-"}</p>
            </div>
        </div>
    );
}

export default ExplorerDetail;
