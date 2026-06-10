import { useRef, useState } from "react";
import { Modal } from "react-bootstrap";
import { FormattedMessage, defineMessages } from "react-intl";
import { Api } from "api/api";
import { modalDialogHide } from "actions/global/modalDialog";
import { structureTypeInvalidate } from "actions/arr/structureType";
import { useAppThunkDispatch } from "utils/hooks";
import { Button } from "../../ui";
import { MultiStructureEdit, MultiStructureEditHandle } from "./MultiStructureEdit";

const messages = defineMessages({
    update: { id: "global.action.update", defaultMessage: "Uložit" },
    cancel: { id: "global.action.cancel", defaultMessage: "Zrušit" },
});

interface Props {
    fundId: number;
    fundVersionId: number;
    structureTypeCode: string;
    structureObjectIds: number[];
    onClose?: () => void;
}

export type { Props as UpdateMultipleStructureDataFormProps };

export function UpdateMultipleStructureDataForm({
    fundId,
    fundVersionId,
    structureTypeCode,
    structureObjectIds,
    onClose,
}: Props) {
    const dispatch = useAppThunkDispatch();
    const editRef = useRef<MultiStructureEditHandle | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleSubmit() {
        const payload = editRef.current?.buildPayload();
        if (!payload) {
            return;
        }
        setIsSubmitting(true);
        try {
            await Api.structure.sdoUpdateObjects(fundId, structureTypeCode, payload);
            dispatch(modalDialogHide());
            dispatch(structureTypeInvalidate());
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div>
            <Modal.Body>
                <MultiStructureEdit
                    ref={editRef}
                    fundId={fundId}
                    fundVersionId={fundVersionId}
                    structureObjectId={structureObjectIds[0]}
                    structureObjectIds={structureObjectIds}
                />
            </Modal.Body>
            <Modal.Footer>
                <Button variant="outline-secondary" disabled={isSubmitting} onClick={handleSubmit}>
                    <FormattedMessage {...messages.update} />
                </Button>
                <Button variant="link" disabled={isSubmitting} onClick={onClose}>
                    <FormattedMessage {...messages.cancel} />
                </Button>
            </Modal.Footer>
        </div>
    );
}
