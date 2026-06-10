import { Modal } from "react-bootstrap";
import { FormattedMessage, defineMessages } from "react-intl";
import { Button } from "../../ui";
import { StructureEdit } from "./StructureEdit";

const messages = defineMessages({
    close: { id: "global.action.close", defaultMessage: "Zavřít" },
});

interface Props {
    fundVersionId: number;
    fundId: number;
    structureObjectId: number;
    readMode?: boolean;
    onClose?: () => void;
}

export type { Props as UpdateStructureDataFormProps };

export function UpdateStructureDataForm({
    fundVersionId,
    fundId,
    structureObjectId,
    readMode = false,
    onClose,
}: Props) {
    return (
        <div>
            <Modal.Body>
                <StructureEdit
                    fundId={fundId}
                    fundVersionId={fundVersionId}
                    structureObjectId={structureObjectId}
                    readMode={readMode}
                    plain={true}
                />
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...messages.close} />
                </Button>
            </Modal.Footer>
        </div>
    );
}
