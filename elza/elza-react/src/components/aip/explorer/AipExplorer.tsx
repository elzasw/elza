import type { ExplorerNode } from "./utils";
import ExplorerTree from "./tree/ExplorerTree";
import "./AipExplorer.scss";
import { Splitter } from "components/shared";``
import ExplorerTable from "./table/ExplorerTable";
import ExplorerDetail from "./detail/ExplorerDetail";
import ExplorerNavigationTab from "./ExplorerNavigationTab";
import { Divider } from "@fluentui/react-components";
import ExplorerContext, { ExplorerMode } from "./ExplorerContext";
import { AREA_AIP } from "actions/aip/aip";
import { AREA_AIP_STRUCTURE } from "actions/aip/exp";
import { useEffect } from "react";
import { useExplorerContext } from "./ExplorerContext";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import { FormattedMessage, defineMessages } from 'react-intl';

// Id je převzaté z legacy katalogu beze změny.
const messages = defineMessages({
    notSelected: { id: 'aip.detail.notSelected', defaultMessage: 'Nebyl vybrán žádný AIP' },
});

type AipExplorerProps = {
    mode: ExplorerMode;
    onSelect?: (node: ExplorerNode) => void;
    selected?: string;
    /** Hide the synthetic root; the sections of the package become the top level. */
    hideRoot?: boolean;
    /**
     * The part the user selected, by UUID - wherever it was selected (tree, table, navigation).
     * The root of the package is reported as no selection.
     */
    onSelectionChange?: (uuid: string | undefined) => void;
}

/** Reports the selection of the explorer out of its context; renders nothing. */
function SelectionReporter({onChange}: {onChange: (uuid: string | undefined) => void}): null {
    const {selectedItem} = useExplorerContext();
    const structure = useSelector((state: AppState) => storeFromArea(state, AREA_AIP_STRUCTURE));
    const uuid: string | undefined = selectedItem?.uuid;
    const rootUuid: string | undefined = structure?.data?.uuid;

    useEffect(() => {
        // nothing is selected until the structure loads - that is not a change to report
        if (selectedItem) {
            onChange(uuid === rootUuid ? undefined : uuid);
        }
    }, [uuid]);

    return null;
}

const AipExplorer = ({mode, onSelect, selected, hideRoot, onSelectionChange}: AipExplorerProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    return (
        <ExplorerContext mode={mode} hideRoot={hideRoot}>
            {onSelectionChange && <SelectionReporter onChange={onSelectionChange}/>}
            <div className="aip-explorer">
                {!aip.id && <div className="not-selected">
                        <p><FormattedMessage {...messages.notSelected} /></p>
                    </div>
                }
                {aip.id && <>
                        <ExplorerNavigationTab />
                        <Divider />
                        <Splitter
                            left={<ExplorerTree onSelect={onSelect}/>}
                            center={<ExplorerTable />}
                            right={<ExplorerDetail selected={selected} />}
                            rightSize={370}
                        />
                    </>
                }
            </div>
        </ExplorerContext>
    );
}


export default AipExplorer
