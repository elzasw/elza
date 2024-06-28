import { AREA_AIP } from "actions/aip/aip";
import i18n from "components/i18n";
import { FC } from "react"
import { Modal, Button, Row, Col} from "react-bootstrap";
import { useSelector } from "react-redux";
import { storeFromArea } from "shared/utils";
import { AppState } from "typings/store";
import AipTree from "./AipTree";
import { useThunkDispatch } from "utils/hooks";
import AipFileTable from "./AipFileTable";
import { FluentProvider } from "@fluentui/react-components";
import "./AipExplorer.scss";
import AipFileDetail from "./AipFileDetail";

const AipExplorer: FC = () => {
    const aip = useSelector((state: AppState) => storeFromArea(state, AREA_AIP));
    const dispatch = useThunkDispatch();

    return (
        <Row>
            {/* <Col>
                <AipTree />
            </Col> */}
            {/* <Col>
                <AipFileTable />
            </Col> */}
            <Col>
                <AipFileDetail />
            </Col>
        </Row>
    );
}

export default AipExplorer