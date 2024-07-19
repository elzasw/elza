import {  FluentProvider } from "@fluentui/react-components";
import i18n from "components/i18n";
import { FC } from "react";
import { Modal, Button } from "react-bootstrap";
import AipExplorer from "./AipExplorer";
import { ExplorerMode } from "./ExplorerContext";

type AipExplorerModalWrapperProps = {
    onOk: () => void;
    onClose: () => void;
    mode: ExplorerMode;
}

const AipExplorerModalWrapper = ({onOk, onClose, mode}: AipExplorerModalWrapperProps) => (
    <FluentProvider>
        <Modal.Body>
            <AipExplorer mode={mode}/>
        </Modal.Body>
        <Modal.Footer>
            <Button onClick={onOk} variant="outline-secondary">
                OK
            </Button>
            <Button onClick={onClose} variant="link">
                {i18n('global.action.cancel')}
            </Button>
        </Modal.Footer>
    </FluentProvider>
);

export default AipExplorerModalWrapper;