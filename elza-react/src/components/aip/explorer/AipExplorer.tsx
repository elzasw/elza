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
import {i18n} from 'components/shared'

type AipExplorerProps = {
    mode: ExplorerMode;
    onSelect?: (node: ExplorerNode) => void;
    selected?: string;
}

const AipExplorer = ({mode, onSelect, selected}: AipExplorerProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    return (
        <ExplorerContext mode={mode}>
            <div className="aip-explorer">
                {!aip.id && <div className="not-selected">
                        <p>{i18n("aip.detail.notSelected")}</p>
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
