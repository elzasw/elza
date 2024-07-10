import { FC } from "react"
import ExplorerTree from "./tree/ExplorerTree";
import "./AipExplorer.scss";
import { Splitter } from "components/shared";
import ExplorerTable from "./table/ExplorerTable";
import ExplorerDetail from "./detail/ExplorerDetail";
import ExplorerNavigationTab from "./ExplorerNavigationTab";

const AipExplorer: FC = () => {

    return (
        <>
            <ExplorerNavigationTab />
            <Splitter 
                left={<ExplorerTree />}
                center={<ExplorerTable />}
                right={<ExplorerDetail />}
            />
        </>
    );
}

export default AipExplorer