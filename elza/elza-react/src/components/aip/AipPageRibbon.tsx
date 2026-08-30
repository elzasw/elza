import {aipFetchIfNeeded, aipsFetchIfNeeded, AREA_AIP, AREA_SELECTED_AIPS} from "actions/aip/aip";
import { AipDetailVO } from "elza-api";
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
import { runAipAction } from "./AipActionRunner";
import { useIntl } from "react-intl";

const AipPageRibbon: FC = () => {
    const selectedAips = useSelector((state: AppState) => storeFromArea(state, AREA_SELECTED_AIPS));
    const aip =  useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    /** Akce mění i AIP otevřený v detailu, panel se proto načte znovu spolu se seznamem. */
    /** Akce běží na pozadí; dialog ukáže její průběh a po dokončení se seznam načte znovu. */
    const run = (title: string, request: () => Promise<{ data: import("elza-api").DaAipActionVO }>) =>
        runAipAction(dispatch, intl, title, request as never, reload);

    const reload = () => {
        dispatch(aipsFetchIfNeeded(true));
        if (aip?.id != null) {
            dispatch(aipFetchIfNeeded(aip.id, true));
        }
    };

    /**
     * Akce se nabízejí nad označenými AIPy a zároveň nad AIPem otevřeným v detailu,
     * aby nebylo nutné ho kvůli akci ještě označovat.
     *
     * Akce je dostupná, pokud dává smysl aspoň pro jeden z předaných AIPů; u ostatních
     * ji server přeskočí. Tím se z pásu karet pozná, co lze v daném stavu udělat.
     */
    const actionsFor = (aips: AipDetailVO[], keyPrefix: string): JSX.Element[] => {
    const aipIds = aips.map(a => a.aipId);
    const some = (predicate: (aip: AipDetailVO) => boolean) => aips.some(predicate);

    const canLoadMetadata = some(a => a.metadataLoad !== true);
    const canDeleteMetadata = some(a => a.metadataLoad === true);
    const canLoadCompleteAip = some(a => a.metadataLoad === true && a.completeAipLoad !== true);
    const canDeleteCompleteAip = some(a => a.completeAipLoad === true);
    const canExport = some(a => (a.linkedNodes?.length ?? 0) > 0);

    return [
        <Button key={`${keyPrefix}-metadata`} disabled={!canLoadMetadata}
                onClick={() => run(i18n("aip.actions.metadata"), () => Api.aips.aipCreateDaoStructure(aipIds))}>
            <Icon glyph="fa-download" />
            <div>
                <span className="btnText">{i18n("aip.actions.metadata")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-deleteMetadata`} disabled={!canDeleteMetadata}
                onClick={() => run(i18n("aip.actions.deleteMetadata"), () => Api.aips.aipDeleteDaoStructure(aipIds))}>
            <Icon glyph="fa-trash" />
            <div>
                <span className="btnText">{i18n("aip.actions.deleteMetadata")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-loadAips`} disabled={!canLoadCompleteAip}
                onClick={() => run(i18n("aip.actions.loadAips"), () => Api.aips.aipDownloadCompleteAip(aipIds))}>
            <Icon glyph="fa-cloud-download " />
            <div>
                <span className="btnText">{i18n("aip.actions.loadAips")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-deleteAips`} disabled={!canDeleteCompleteAip}
                onClick={() => run(i18n("aip.actions.deleteAips"), () => Api.aips.aipDeleteCompleteAip(aipIds))}>
            <Icon glyph="fa-trash" />
            <div>
                <span className="btnText">{i18n("aip.actions.deleteAips")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-updateAips`} onClick={() => handleUpdateAips(aips)}>
            <Icon glyph="fa-refresh" />
            <div>
                <span className="btnText">{i18n("aip.actions.updateAips")}</span>
            </div>
        </Button>,
        <Button key={`${keyPrefix}-exportAips`} disabled={!canExport}
                onClick={() => run(i18n("aip.actions.exportAips"), () => Api.aips.aipExportAip(aipIds))}>
            <Icon glyph="fa-cloud-upload" />
            <div>
                <span className="btnText">{i18n("aip.actions.exportAips")}</span>
            </div>
        </Button>,
    ];
    };

    const handleUpdateAips = (aips: AipDetailVO[]) => {
        dispatch(
            modalDialogShow(
                this,
                i18n('aip.form.update.title'),
                <AipUpdateTypeForm
                    aips={aips}
                    onSubmit={({type}) => {
                        run(i18n("aip.form.update.title"), () => Api.aips.aipUpdateAip(type, aips.map(a => a.aipId)));
                    }}
                />,
            ),
        );
    }

    const selectedRows: AipDetailVO[] = selectedAips?.rows ?? [];
    const openedAip: AipDetailVO | undefined = aip?.data;

    const altActions = selectedRows.length > 0 ? actionsFor(selectedRows, "sel") : [];
    // Zaškrtnutý výběr má přednost; akce nad otevřeným AIPem se nabízejí, jen když není
    // zaškrtnuto nic, jinak by v pásu karet byla stejná sada dvakrát.
    const itemActions = selectedRows.length === 0 && openedAip != null
        ? actionsFor([openedAip], "item")
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
