import i18n from "components/i18n";
import { FC } from "react";
import "./ExplorerDetail.scss";
import { AREA_EXPLORER_ITEM, setExplorerItem } from "actions/aip/exp";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { useThunkDispatch } from "utils/hooks";

const ExplorerDetail: FC = () => {
    const {data} = useSelector((state: AppState) => storeFromArea(state, AREA_EXPLORER_ITEM));
    const dispatch = useThunkDispatch();
    
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

    const handleSelect = (folder) => {
        dispatch(setExplorerItem(folder.daoFileFolderId, folder));
    }

    const renderChildren = () => {
        if(!data?.childFolders) return "-";
        return <>{data?.childFolders?.map((folder) => <a onClick={() => handleSelect(folder)}>{folder.label}, </a>)}</>
    }

    if(!data) {
        return <span>Nebyl vybrám žádný objekt</span>
    }
    if(data.daoFileFolderId) {
        return  (
            <div className="explorer-detail">
                <h4>Vztahy</h4>
                <p><b>{i18n("aip.explorer.detail.parent")} </b>
                    {data.parentFolder &&
                        <a onClick={() => handleSelect(data.parentFolder)}>
                            {data.parentFolder?.label}
                        </a>
                    }
                </p>
                <p><b>Potomci </b>{renderChildren()}</p>
            </div>
        );
    }

    return (
        <div className="explorer-detail">
            <h4>{i18n("aip.explorer.detail.title")}</h4>
            <div className="explorer-detail-body">
                <DetailRow label={i18n("aip.explorer.detail.name")} value={data.fileName}/>
                <DetailRow label={i18n("aip.explorer.detail.checksum")} value={data.checksum}/>
                <DetailRow label={i18n("aip.explorer.detail.format")} value={data.mimeType}/>
                <DetailRow label={i18n("aip.explorer.detail.as")} value="nasd"/>
            </div>
        </div>
    );
}

export default ExplorerDetail;