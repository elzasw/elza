import { i18n } from 'components/shared';
import { Form, Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import HorizontalLoader from '../loading/HorizontalLoader';
import { Form as FinalForm } from 'react-final-form';

import './ConfirmForm.scss';

export interface Props {
    onClose?: () => void;
    onSubmit?: () => void | Promise<unknown>;
    onSubmitSuccess?: (result?: unknown) => void;
    confirmMessage: string;
    submittingMessage: string;
    submitTitle: string;
}

export default function ConfirmDialog({
    onClose,
    onSubmit,
    onSubmitSuccess,
    confirmMessage,
    submittingMessage,
    submitTitle = i18n('global.action.store'),
}: Props) {
    const handleFormSubmit = async () => {
        const result = await onSubmit?.();
        onSubmitSuccess?.(result);
    };

    return (
        <Form>
            <FinalForm onSubmit={handleFormSubmit}>
                {({ handleSubmit, submitting }) => {
                    return <div className="confirm-form-container">
                        {submitting ?
                            <HorizontalLoader text={submittingMessage} /> :
                            <Modal.Body className="message">{confirmMessage}</Modal.Body>
                        }
                        <Modal.Footer>
                            <Button disabled={submitting} variant="outline-secondary" onClick={handleSubmit}>
                                {submitTitle}
                            </Button>
                            <Button variant="link" onClick={onClose}>
                                {i18n('global.action.cancel')}
                            </Button>
                        </Modal.Footer>
                    </div>
                }}
            </FinalForm>
        </Form>
    );
}
