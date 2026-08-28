import {aipsFetchIfNeeded, AREA_AIP, AREA_SELECTED_AIPS} from "actions/aip/aip";
import { AipDetailVO, AipUpdateType } from "elza-api";
import { Ribbon } from "components";
import { Icon, RibbonGroup, i18n } from "components/shared";
import React, { FC } from "react";
import { Button } from "react-bootstrap";
import { useIntl } from "react-intl";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import {Api} from "../../api";
import {useThunkDispatch} from "../../utils/hooks";
import {modalDialogShow} from "../../actions/global/modalDialog";
import AipUpdateTypeForm from "./AipUpdateTypeForm.tsx";
import { updateTypeMessages } from "./messages";

const AipPageRibbon: FC = () => {
    const selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aip =  useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    const reload = () => dispatch(aipsFetchIfNeeded(true));

    /**
     * Akce se nabízejí nad označenými AIPy a zároveň nad AIPem otevřeným v detailu,
     * aby nebylo nutné ho kvůli akci ještě označovat.
     */
    const actionsFor = (aipIds: number[], keyPrefix: string): JSX.Element[] => [
        <Button key={`${keyPrefix}-metadata`}
                onClick={() => Api.aips.aipCreateDaoStructure(aipIds).then(reload)}>
            <Icon glyph="fa-download" />
            <div>
                <span className="btnText">{i18n("aip.actions.metadata")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-deleteMetadata`}
                onClick={() => Api.aips.aipDeleteDaoStructure(aipIds).then(reload)}>
            <Icon glyph="fa-trash" />
            <div>
                <span className="btnText">{i18n("aip.actions.deleteMetadata")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-loadAips`}
                onClick={() => Api.aips.aipDownloadCompleteAip(aipIds).then(reload)}>
            <Icon glyph="fa-cloud-download " />
            <div>
                <span className="btnText">{i18n("aip.actions.loadAips")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-deleteAips`}
                onClick={() => Api.aips.aipDeleteCompleteAip(aipIds).then(reload)}>
            <Icon glyph="fa-trash" />
            <div>
                <span className="btnText">{i18n("aip.actions.deleteAips")}</span>
            </div>
        </Button>,
        // Samostatné tlačítko, protože je to jediná akce, kterou se řeší nedohledaná
        // instituce nebo fond - schovaná ve výběru typu aktualizace by se nenašla.
        <Button key={`${keyPrefix}-remapReferences`}
                onClick={() => Api.aips.aipUpdateAip(AipUpdateType.RemapReferences, aipIds).then(reload)}>
            <Icon glyph="fa-link" />
            <div>
                <span className="btnText">
                    {intl.formatMessage(updateTypeMessages[AipUpdateType.RemapReferences])}
                </span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-updateAips`} onClick={() => handleUpdateAips(aipIds)}>
            <Icon glyph="fa-refresh" />
            <div>
                <span className="btnText">{i18n("aip.actions.updateAips")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-exportAips`}
                onClick={() => Api.aips.aipExportAip(aipIds).then(reload)}>
            <Icon glyph="fa-cloud-upload" />
            <div>
                <span className="btnText">{i18n("aip.actions.exportAips")}</span>
            </div>
        </Button>,
    ];

    const handleUpdateAips = (aipIds: number[]) => {
        dispatch(
            modalDialogShow(
                this,
                i18n('aip.form.update.title'),
                <AipUpdateTypeForm
                    onSubmit={({type}) => {
                        Api.aips.aipUpdateAip(type, aipIds).then(reload);
                    }}
                />,
            ),
        );
    }

    const selectedIds: number[] = selectedAips?.rows?.map((row: AipDetailVO) => row.aipId) ?? [];
    const openedAipId: number | undefined = aip?.data?.aipId;

    const altActions = selectedIds.length > 0 ? actionsFor(selectedIds, "sel") : [];
    // Akce nad otevřeným AIPem se nabízejí jen když není součástí výběru, jinak by
    // v pásu karet byla stejná sada dvakrát.
    const itemActions = openedAipId != null && !selectedIds.includes(openedAipId)
        ? actionsFor([openedAipId], "item")
        : [];

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
