import { Modal } from 'react-bootstrap';
import { Button } from '../../ui';
import FormInput from 'components/shared/form/FormInput';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    code: { id: 'global.exception.detail.code', defaultMessage: 'Kód chyby' },
    message: { id: 'global.exception.detail.message', defaultMessage: 'Technický popis' },
    stack: { id: 'global.exception.detail.stack', defaultMessage: 'Stack' },
    properties: { id: 'global.exception.detail.properties', defaultMessage: 'Rozšiřující parametry' },
});
import { ModalDialogWrapper } from 'components/shared';
import { ExceptionData } from './Exception';

interface Props<T> {
    data: ExceptionData<T>;
    onClose: () => void;
    title?: React.ReactNode;
}

export default function ExceptionDetail<T>({
    data,
    onClose,
    title
}: Props<T>) {
    const intl = useIntl();
    return (
        <ModalDialogWrapper
            className={'dialog-lg top max-height'}
            title={title}
            onHide={onClose}
        >
            <div>
                <Modal.Body>
                    {data.code &&
                        <FormInput type="text" label={intl.formatMessage(messages.code)} readOnly value={data.code} />
                    }
                    {data.message && (
                        <FormInput
                            label={intl.formatMessage(messages.message)}
                            type="textarea"
                            style={{ height: '5em' }}
                            readOnly
                            value={data.message}
                        />
                    )}
                    {data.stackTrace && (
                        <FormInput
                            label={intl.formatMessage(messages.stack)}
                            type="textarea"
                            style={{ height: '25em' }}
                            readOnly
                            value={data.stackTrace}
                        />
                    )}
                    {data.properties && (
                        <FormInput
                            label={intl.formatMessage(messages.properties)}
                            type="textarea"
                            style={{ height: '10em' }}
                            readOnly
                            value={JSON.stringify(data.properties, null, '  ')}
                        />
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={onClose}>
                        <FormattedMessage {...globalMessages.cancel} />
                    </Button>
                </Modal.Footer>
            </div>
        </ModalDialogWrapper>
    );
}
