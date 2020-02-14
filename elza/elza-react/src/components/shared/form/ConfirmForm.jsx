/**
 * Formulář synchronizace DAOS na AS.
 */
import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Button, Form, Modal} from 'react-bootstrap';
import {reduxForm} from "redux-form";
import HorizontalLoader from "../loading/HorizontalLoader";

import './ConfirmForm.scss';

class ConfirmForm extends AbstractReactComponent {

    static propTypes = {

    };

    static defaultProps = {
        locked: false
    };

    constructor(props) {
        super(props);
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {onClose, handleSubmit, submitting, confirmMessage, submittingMessage, submitTitle} = this.props;

        let content;

        if (submitting) {
            content = <HorizontalLoader text={submittingMessage} />;
        } else {
            content = <Modal.Body className="message">
                {confirmMessage}
            </Modal.Body>
        }

        return (
            <Form onSubmit={handleSubmit}>
                <div className="confirm-form-container">
                    {content}
                    <Modal.Footer>
                        <Button disabled={submitting} type="submit">{submitTitle ? submitTitle : i18n('global.action.store')}</Button>
                        <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                    </Modal.Footer>
                </div>
            </Form>
        )
    }
}

export default reduxForm({
    form: 'confirmForm',
    fields: []
})(ConfirmForm);
