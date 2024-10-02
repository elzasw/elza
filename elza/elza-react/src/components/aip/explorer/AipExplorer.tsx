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

type AipExplorerProps = {
    mode: ExplorerMode;
    onSelect?: (node) => void;
}

const AipExplorer = ({mode, onSelect}: AipExplorerProps) => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));

    if(!aip.id) {
        return (
            <div className="not-selected">
                <p>Nebyl vybrán žádný objekt</p>
            </div>
        );
    }

    return (
        <ExplorerContext mode={mode}>
            <div className="aip-explorer">
                <ExplorerNavigationTab />
                <Divider />
                <Splitter
                    left={<ExplorerTree onSelect={onSelect}/>}
                    center={<ExplorerTable />}
                    right={<ExplorerDetail />}
                    rightSize={370}
                />
            </div>
        </ExplorerContext>
    );
}


export default AipExplorer
