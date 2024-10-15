import i18n from "components/i18n";
import { Modal, Button } from "react-bootstrap";
import AipExplorer from "./AipExplorer";
import { ExplorerMode } from "./ExplorerContext";

type AipExplorerModalWrapperProps = {
    onOk: () => void;
    onClose: () => void;
    mode: ExplorerMode;
    selected?: string;
}

const AipExplorerModalWrapper = ({onOk, onClose, mode, selected}: AipExplorerModalWrapperProps) => (
    <>
        <Modal.Body>
            <AipExplorer mode={mode} selected={selected}/>
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={onOk} variant="outline-secondary">
                OK
            </Button>
            <Button onClick={onClose} variant="link">
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </>
);

export default AipExplorerModalWrapper;
