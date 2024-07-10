import {  FluentProvider } from "@fluentui/react-components";
import i18n from "components/i18n";
import { FC } from "react";
import { Modal, Button } from "react-bootstrap";
import AipExplorer from "./AipExplorer";

type AipExplorerModalWrapperProps = {
    onOk: () => void;
    onClose: () => void;
}

const AipExplorerModalWrapper: FC = ({onOk, onClose}: AipExplorerModalWrapperProps) => {

    return (
        <FluentProvider>
            <Modal.Body>
               <AipExplorer />
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
}

export default AipExplorerModalWrapper;