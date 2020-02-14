import React from 'react';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {Modal, Button, Input} from 'react-bootstrap';
import AbstractReactComponent from "../../AbstractReactComponent";
import FormInput from "../form/FormInput";
import i18n from "../../i18n";

class ExceptionDetail extends AbstractReactComponent {
    render() {
        const {data} = this.props;
        return (
            <div>
                <Modal.Body>
                    <FormInput type="text" label={i18n('global.exception.detail.code')} readOnly value={data.code} />
                    {data.message && <FormInput label={i18n('global.exception.detail.message')}
                                                as="textarea"
                                                style={{height: '5em'}}
                                                readOnly
                                                value={data.message}/>}
                    {data.stackTrace && <FormInput label={i18n("global.exception.detail.stack")}
                                                   as="textarea"
                                                   style={{height: '25em'}}
                                                   readOnly
                                                   value={data.stackTrace}/>}
                    {data.properties && <FormInput label={i18n("global.exception.detail.properties")}
                                                   as="textarea"
                                                   style={{height: '10em'}}
                                                   readOnly
                                                   value={JSON.stringify(data.properties, null, '  ')}/>}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={this.props.onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default ExceptionDetail;
