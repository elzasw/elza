import {aipsFetchIfNeeded, AREA_AIP, AREA_SELECTED_AIPS} from "actions/aip/aip";
import { Ribbon } from "components";
import { Icon, RibbonGroup, i18n } from "components/shared";
import React, { FC } from "react";
import { Button } from "react-bootstrap";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {Api} from "../../api";
import {useThunkDispatch} from "../../utils/hooks";
import {modalDialogShow} from "../../actions/global/modalDialog";
import AipUpdateTypeForm from "./AipUpdateTypeForm.tsx";

const AipPageRibbon: FC = () => {
    const selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aip =  useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const dispatch = useThunkDispatch();

    const handleLoadMetadata = () => {
        let aipIds = selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipCreateDaoStructure(aipIds).then(() => {
            dispatch(aipsFetchIfNeeded(true))
        });
    }
    const handleDeleteMetadata = () => {
        let aipIds = selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDeleteDaoStructure(aipIds).then(() => {
            dispatch(aipsFetchIfNeeded(true))
        });
    }
    const handleLoadAips = () => {
        let aipIds = selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDownloadCompleteAip(aipIds).then(() => {
            dispatch(aipsFetchIfNeeded(true))
        });
    }
    const handleDeleteAips = () => {
        let aipIds = selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipDeleteCompleteAip(aipIds).then(() => {
            dispatch(aipsFetchIfNeeded(true))
        });
    }
    const handleUpdateAips = () => {
        dispatch(
            modalDialogShow(
                this,
                i18n('aip.form.update.title'),
                <AipUpdateTypeForm
                    onSubmit={({type}) => {
                        let aipIds = selectedAips.rows.map(aip => aip.aipId)
                        Api.aips.aipUpdateAip(type, aipIds).then(() => {
                            dispatch(aipsFetchIfNeeded(true))
                        });
                    }}
                />,
            ),
        );
    }
    const handleExportAips = () => {
        let aipIds = selectedAips.rows.map(aip => aip.aipId)
        Api.aips.aipExportAip(aipIds).then(() => {
            dispatch(aipsFetchIfNeeded(true))
        });
    }

    const altActions = [];
    const itemActions = [];


    if (selectedAips?.rows?.length > 0) {
        altActions.push(
            <Button key="metadata" onClick={handleLoadMetadata}>
                <Icon glyph="fa-download" />
                <div>
                    <span className="btnText">{i18n("aip.actions.metadata")}</span>
                </div>
            </Button>
        );
        altActions.push(
            <Button key="deleteMetadata" onClick={handleDeleteMetadata}>
                <Icon glyph="fa-trash" />
                <div>
                    <span className="btnText">{i18n("aip.actions.deleteMetadata")}</span>
                </div>
            </Button>
        );
        altActions.push(
            <Button key="loadAips" onClick={handleLoadAips}>
                <Icon glyph="fa-cloud-download " />
                <div>
                    <span className="btnText">{i18n("aip.actions.loadAips")}</span>
                </div>
            </Button>
        );
        altActions.push(
            <Button key="deleteAips" onClick={handleDeleteAips}>
                <Icon glyph="fa-trash" />
                <div>
                    <span className="btnText">{i18n("aip.actions.deleteAips")}</span>
                </div>
            </Button>
        );
        altActions.push(
            <Button key="updateAips" onClick={handleUpdateAips}>
                <Icon glyph="fa-refresh" />
                <div>
                    <span className="btnText">{i18n("aip.actions.updateAips")}</span>
                </div>
            </Button>
        );
        altActions.push(
            <Button key="exportAips" onClick={handleExportAips}>
                <Icon glyph="fa-cloud-upload" />
                <div>
                    <span className="btnText">{i18n("aip.actions.exportAips")}</span>
                </div>
            </Button>
        );
    }

    let itemSection: React.ReactNode;
    if (itemActions.length > 0) {
        itemSection = (
            <RibbonGroup key="item-actions" className="small">
                {itemActions}
            </RibbonGroup>
        );
    }

    let altSection: React.ReactNode;
    if (altActions.length > 0) {
        altSection = (
            <RibbonGroup key="alt-actions" className="small">
                {altActions}
            </RibbonGroup>
        );
    }

    return (
        <div>
            <Ribbon altSection={altSection} itemSection={itemSection}/>
        </div>
    );
}

export default AipPageRibbon;
