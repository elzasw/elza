import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {i18n, FormInput} from 'components'
import {Modal, Button, Input} from 'react-bootstrap';

var ExceptionDetail = class ExceptionDetail extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    render() {
        const {data} = this.props;
        return (
            <div>
                <Modal.Body>
                    <FormInput type="text" label={i18n('global.exception.detail.code')} readOnly value={data.code} />
                    {data.message && <FormInput type="text" label={i18n('global.exception.detail.message')} readOnly
                                                value={data.message}/>}
                    {data.devMessage && <FormInput label={i18n("global.exception.detail.stack")}
                                                   componentClass="textarea"
                                                   style={{height: '30em'}}
                                                   readOnly
                                                   value={data.devMessage}/>}
                    {data.properties && <FormInput label={i18n("global.exception.detail.properties")}
                                                   componentClass="textarea"
                                                   style={{height: '10em'}}
                                                   readOnly
                                                   value={JSON.stringify(data.properties, null, '  ')}/>}
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={this.props.onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};

module.exports = ExceptionDetail;
