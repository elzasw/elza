import { FormattedMessage } from "react-intl";
import { globalMessages } from "components/shared/lang";
import { Modal, Button } from "react-bootstrap";
import AipExplorer from "./AipExplorer";
import { ExplorerMode } from "./ExplorerContext";

type AipExplorerModalWrapperProps = {
    onOk: () => void;
    mode: ExplorerMode;
    selected?: string;
}

const AipExplorerModalWrapper = ({onOk, mode, selected}: AipExplorerModalWrapperProps) => (
    <>
        <Modal.Body>
            <AipExplorer mode={mode} selected={selected}/>
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={onOk} variant="outline-secondary">
                <FormattedMessage {...globalMessages.ok} />
            </Button>
            <Button onClick={onOk} variant="link">
                <FormattedMessage {...globalMessages.cancel} />
            </Button>
        </Modal.Footer>
    </>
);

export default AipExplorerModalWrapper;
