/**
 * Formulář synchronizace DAOS na AS.
 */
import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../../ui';
import {reduxForm} from 'redux-form';
import HorizontalLoader from '../loading/HorizontalLoader';

import './ConfirmForm.scss';

class ConfirmForm extends AbstractReactComponent {
    static propTypes = {};

    static defaultProps = {
        locked: false,
    };

    componentDidMount() {}

    UNSAFE_componentWillReceiveProps(nextProps) {}

    render() {
        const {onClose, handleSubmit, submitting, confirmMessage, submittingMessage, submitTitle} = this.props;

        let content;

        if (submitting) {
            content = <HorizontalLoader text={submittingMessage} />;
        } else {
            content = <Modal.Body className="message">{confirmMessage}</Modal.Body>;
        }

        return (
            <Form onSubmit={handleSubmit}>
                <div className="confirm-form-container">
                    {content}
                    <Modal.Footer>
                        <Button disabled={submitting} type="submit">
                            {submitTitle ? submitTitle : i18n('global.action.store')}
                        </Button>
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </div>
            </Form>
        );
    }
}

export default reduxForm({
    form: 'confirmForm',
    fields: [],
})(ConfirmForm);
