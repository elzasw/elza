import React from 'react';
import ReactDOM from 'react-dom';
import {
    Modal,
    Button,
    Radio,
    FormGroup,
    ControlLabel,
    Form,
    Col,
    Row,
    Grid,
    FormControl
} from 'react-bootstrap';

import {
    AbstractReactComponent,
    i18n,
    FormInput,
    Loading,
    Autocomplete
} from 'components/shared';

class CopyConflictForm extends AbstractReactComponent {
    state = {
        filesConflictResolve: 'USE_TARGET',
        packetsConflictResolve: 'USE_TARGET',
        submitting: false
    };
    handleFormSubmit = e => {
        e.preventDefault();
        this.setState({submitting: true});
        this.props.onSubmit(
            this.state.filesConflictResolve,
            this.state.packetsConflictResolve,
            ()=>{
                this.setState({submitting: false});
            }
        );
    };

    render() {
        const {onClose, packetConflict, fileConflict, scopeError, scopeErrors} = this.props;
        const { submitting } = this.state;

        return (
            <Form>
                <Modal.Body>
                    {scopeError &&
                    <ControlLabel>
                        {i18n('arr.fund.addNode.conflict.scopes', scopeErrors.join(", "))}
                    </ControlLabel>}
                    {scopeError && <br />}
                    {fileConflict &&
                    <ControlLabel>
                        {i18n('arr.fund.addNode.conflict.files')}
                    </ControlLabel>}
                    {fileConflict && <FormGroup>
                        <Radio
                            disabled={submitting}
                            name="selectResolveTypeFile"
                            checked={this.state.filesConflictResolve === 'USE_TARGET'}
                            onChange={e => {
                                this.setState({filesConflictResolve: 'USE_TARGET'});
                            }}
                        >
                            {i18n('arr.fund.addNode.conflict.useTarget')}
                        </Radio>
                        <Radio
                            disabled={submitting}
                            name="selectResolveTypeFile"
                            checked={this.state.filesConflictResolve === 'COPY_AND_RENAME'}
                            onChange={e => {
                                this.setState({filesConflictResolve: 'COPY_AND_RENAME'});
                            }}
                        >
                            {i18n('arr.fund.addNode.conflict.rename')}
                        </Radio>
                    </FormGroup>}
                    {packetConflict &&
                    <ControlLabel>
                        {i18n('arr.fund.addNode.conflict.packet')}
                    </ControlLabel>}
                    {packetConflict && <FormGroup>
                        <Radio
                            disabled={submitting}
                            name="selectResolveTypePacket"
                            checked={this.state.packetsConflictResolve === 'USE_TARGET'}
                            onChange={e => {
                                this.setState({packetsConflictResolve: 'USE_TARGET'});
                            }}
                        >
                            {i18n('arr.fund.addNode.conflict.useTarget')}
                        </Radio>
                        <Radio
                            disabled={submitting}
                            name="selectResolveTypePacket"
                            checked={
                                this.state.packetsConflictResolve === 'COPY_AND_RENAME'
                            }
                            onChange={e => {
                                this.setState({packetsConflictResolve: 'COPY_AND_RENAME'});
                            }}
                        >
                            {i18n('arr.fund.addNode.conflict.rename')}
                        </Radio>
                    </FormGroup>}
                </Modal.Body>
                <Modal.Footer>
                    {!scopeError &&
                    <Button disabled={submitting} type="submit" onClick={this.handleFormSubmit}>
                        {i18n('global.action.store')}
                    </Button>}
                    <Button disabled={submitting} bsStyle="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}
export default CopyConflictForm;
