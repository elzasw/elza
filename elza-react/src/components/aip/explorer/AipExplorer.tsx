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
}

const AipExplorer = ({mode, onSelect, selected, hideRoot}: AipExplorerProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    return (
        <ExplorerContext mode={mode} hideRoot={hideRoot}>
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
