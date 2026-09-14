import React from 'react';
import {Button} from '../../ui';
import {Form, FormCheck, FormGroup, FormLabel, Modal} from 'react-bootstrap';

import {AbstractReactComponent, Icon} from 'components/shared';
import { FormattedMessage, injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { arrPanelMessages } from '../panelMessages';

class CopyConflictForm extends AbstractReactComponent {
    state = {
        filesConflictResolve: 'USE_TARGET',
        structuresConflictResolve: 'USE_TARGET',
        submitting: false,
    };
    handleFormSubmit = e => {
        e.preventDefault();
        this.setState({submitting: true});
        this.props.onSubmit(this.state.filesConflictResolve, this.state.structuresConflictResolve, () => {
            this.setState({submitting: false});
        });
    };

    render() {
        const {
            onClose,
            packetConflict,
            fileConflict,
            scopeError,
            scopeErrors,
            fileConflicts,
            packetConflicts,
        } = this.props;
        const {submitting} = this.state;

        return (
            <Form>
                <Modal.Body>
                    {scopeError && (
                        <FormLabel>
                            {this.props.intl.formatMessage(arrPanelMessages.fundAddNodeConflictScopes, { 0: scopeErrors ? scopeErrors.join(', ') : '' })}
                        </FormLabel>
                    )}
                    {scopeError && <br />}
                    {fileConflict && (
                        <FormLabel>
                            {<FormattedMessage {...arrPanelMessages.fundAddNodeConflictFiles} />}{' '}
                            <Icon
                                style={{cursor: 'pointer'}}
                                title={fileConflicts && fileConflicts.join(', ')}
                                glyph="fa-info-circle"
                            />
                        </FormLabel>
                    )}
                    {fileConflict && (
                        <FormGroup>
                            <FormCheck
                                type={'radio'}
                                disabled={submitting}
                                name="selectResolveTypeFile"
                                checked={this.state.filesConflictResolve === 'USE_TARGET'}
                                onChange={e => {
                                    this.setState({filesConflictResolve: 'USE_TARGET'});
                                }}
                                label={<FormattedMessage {...arrPanelMessages.fundAddNodeConflictUseTarget} />}
                            />
                            <FormCheck
                                type={'radio'}
                                disabled={submitting}
                                name="selectResolveTypeFile"
                                checked={this.state.filesConflictResolve === 'COPY_AND_RENAME'}
                                onChange={e => {
                                    this.setState({filesConflictResolve: 'COPY_AND_RENAME'});
                                }}
                                label={<FormattedMessage {...arrPanelMessages.fundAddNodeConflictRename} />}
                            />
                        </FormGroup>
                    )}
                    {packetConflict && (
                        <FormLabel>
                            {<FormattedMessage {...arrPanelMessages.fundAddNodeConflictStructure} />}{' '}
                            <Icon
                                style={{cursor: 'pointer'}}
                                title={packetConflicts && packetConflicts.join(', ')}
                                glyph="fa-info-circle"
                            />
                        </FormLabel>
                    )}
                    {packetConflict && (
                        <FormGroup>
                            <FormCheck
                                type={'radio'}
                                disabled={submitting}
                                name="selectResolveTypePacket"
                                checked={this.state.structuresConflictResolve === 'USE_TARGET'}
                                onChange={e => {
                                    this.setState({structuresConflictResolve: 'USE_TARGET'});
                                }}
                                label={<FormattedMessage {...arrPanelMessages.fundAddNodeConflictUseTarget} />}
                            />
                            <FormCheck
                                type={'radio'}
                                disabled={submitting}
                                name="selectResolveTypePacket"
                                checked={this.state.structuresConflictResolve === 'COPY_AND_RENAME'}
                                onChange={e => {
                                    this.setState({structuresConflictResolve: 'COPY_AND_RENAME'});
                                }}
                                label={<FormattedMessage {...arrPanelMessages.fundAddNodeConflictRename} />}
                            />
                        </FormGroup>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    {!scopeError && (
                        <Button
                            disabled={submitting}
                            variant="outline-secondary"
                            type="submit"
                            onClick={this.handleFormSubmit}
                        >
                            {<FormattedMessage {...globalMessages.save} />}
                        </Button>
                    )}
                    <Button disabled={submitting} variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

export default injectIntl(CopyConflictForm);
