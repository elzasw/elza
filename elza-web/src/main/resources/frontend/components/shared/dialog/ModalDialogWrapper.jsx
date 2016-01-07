/**
 * Modální dialog.
 */

import React from 'react';

import {i18n} from 'components';
import {Modal} from 'react-bootstrap';

var ModalDialogWrapper = class ModalDialogWrapper extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <Modal show={true} onHide={this.props.onHide}>
                <Modal.Header closeButton onHide={this.props.onHide}>
                    <Modal.Title>{this.props.title}</Modal.Title>
                </Modal.Header>

                <div ref="modalBody">
                    {this.props.children}
                </div>
            </Modal>
        );
    }
}

module.exports = ModalDialogWrapper;
