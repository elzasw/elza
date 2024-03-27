import { Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import FormInput from 'components/shared/form/FormInput';
import i18n from '../../i18n';
import { ModalDialogWrapper } from 'components/shared';
import { ExceptionData } from './Exception';

interface Props<T> {
    data: ExceptionData<T>;
    onClose: () => void;
    title?: string;
}

export default function ExceptionDetail<T>({
    data,
    onClose,
    title
}: Props<T>) {
    return (
        <ModalDialogWrapper
            className={'dialog-lg top max-height'}
            title={title}
            onHide={onClose}
        >
            <div>
                <Modal.Body>
                    {data.code &&
                        <FormInput type="text" label={i18n('global.exception.detail.code')} readOnly value={data.code} />
                    }
                    {data.message && (
                        <FormInput
                            label={i18n('global.exception.detail.message')}
                            as="textarea"
                            style={{ height: '5em' }}
                            readOnly
                            value={data.message}
                        />
                    )}
                    {data.stackTrace && (
                        <FormInput
                            label={i18n('global.exception.detail.stack')}
                            as="textarea"
                            style={{ height: '25em' }}
                            readOnly
                            value={data.stackTrace}
                        />
                    )}
                    {data.properties && (
                        <FormInput
                            label={i18n('global.exception.detail.properties')}
                            as="textarea"
                            style={{ height: '10em' }}
                            readOnly
                            value={JSON.stringify(data.properties, null, '  ')}
                        />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </div>
        </ModalDialogWrapper>
    );
}
