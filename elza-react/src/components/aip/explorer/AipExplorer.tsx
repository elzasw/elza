import { FC } from "react"
import ExplorerTree from "./tree/ExplorerTree";
import "./AipExplorer.scss";
import { Splitter } from "components/shared";
import ExplorerTable from "./table/ExplorerTable";
import ExplorerDetail from "./detail/ExplorerDetail";
import ExplorerNavigationTab from "./ExplorerNavigationTab";
import { Divider } from "@fluentui/react-components";
import ExplorerContext from "./ExplorerContext";

const AipExplorer: FC = () =>  (
    <ExplorerContext>
        <ExplorerNavigationTab />
        <Divider />
        <Splitter
            left={<ExplorerTree />}
            center={<ExplorerTable />}
            right={<ExplorerDetail />}
            rightSize={370}
        />
    </ExplorerContext>
);


export default AipExplorer