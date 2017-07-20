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
    packetsConflictResolve: 'USE_TARGET'
  };
  handleSubmit = data => {
    this.props.onSumbit(
      this.state.filesConflictResolve,
      this.state.packetsConflictResolve
    );
  };
  render() {
    const { onClose, packetConflict, fileConflict, scopeError } = this.props;
    return (
      <Form>
        <Modal.Body>
          {fileConflict &&
            <ControlLabel>
              {i18n('arr.fund.addNode.conflict.files')}
            </ControlLabel>}
          <FormGroup>
            <Radio
              name="selectResolveTypeFile"
              checked={this.state.filesConflictResolve === 'USE_TARGET'}
              onChange={e => {
                this.setState({ filesConflictResolve: 'USE_TARGET' });
              }}
            >
              {i18n('arr.fund.addNode.conflict.useTarget')}
            </Radio>
            <Radio
              name="selectResolveTypeFile"
              checked={this.state.filesConflictResolve === 'COPY_AND_RENAME'}
              onChange={e => {
                this.setState({ filesConflictResolve: 'COPY_AND_RENAME' });
              }}
            >
              {i18n('arr.fund.addNode.conflict.rename')}
            </Radio>
          </FormGroup>
          {packetConflict &&
            <ControlLabel>
              {i18n('arr.fund.addNode.conflict.packets')}
            </ControlLabel> &&
            <FormGroup>
              <Radio
                name="selectResolveTypePacket"
                checked={this.state.packetsConflictResolve === 'USE_TARGET'}
                onChange={e => {
                  this.setState({ packetsConflictResolve: 'USE_TARGET' });
                }}
              >
                {i18n('arr.fund.addNode.conflict.useTarget')}
              </Radio>
              <Radio
                name="selectResolveTypePacket"
                checked={
                  this.state.packetsConflictResolve === 'COPY_AND_RENAME'
                }
                onChange={e => {
                  this.setState({ packetsConflictResolve: 'COPY_AND_RENAME' });
                }}
              >
                {i18n('arr.fund.addNode.conflict.rename')}
              </Radio>
            </FormGroup>}
        </Modal.Body>
        <Modal.Footer>
          {!scopeError &&
            <Button type="submit" onClick={this.handleFormSubmit}>
              {i18n('global.action.store')}
            </Button>}
          <Button bsStyle="link" onClick={onClose}>
            {i18n('global.action.cancel')}
          </Button>
        </Modal.Footer>
      </Form>
    );
  }
}
export default CopyConflictForm;
