import { Modal } from "react-bootstrap";
import { FormattedMessage } from "react-intl";
import { Button } from "../../ui";
import { globalMessages } from "components/shared/lang";
import { StructureEdit } from "./StructureEdit";
import { StructureView } from "./StructureView";

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
                {readMode ? (
                    <StructureView
                        fundId={fundId}
                        fundVersionId={fundVersionId}
                        structureObjectId={structureObjectId}
                        plain={true}
                    />
                ) : (
                    <StructureEdit
                        fundId={fundId}
                        fundVersionId={fundVersionId}
                        structureObjectId={structureObjectId}
                        plain={true}
                    />
                )}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    <FormattedMessage {...globalMessages.close} />
                </Button>
            </Modal.Footer>
        </div>
    );
}
