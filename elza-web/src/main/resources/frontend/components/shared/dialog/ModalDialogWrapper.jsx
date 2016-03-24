/**
 * Modální dialog.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {i18n} from 'components';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils'

var ModalDialogWrapper = class ModalDialogWrapper extends React.Component {
    constructor(props) {
        super(props);

        this.dialogWillHide = this.dialogWillHide.bind(this);
    }

    componentDidMount() {
        this.setState({}, () => {
            if (this.refs.modalBody) {
                var el = ReactDOM.findDOMNode(this.refs.modalBody);
                if (el) {
                    setInputFocus(el, false);
                }
            }
        })
    }

    dialogWillHide() {
        this.refs.modal._onHide();
    }

    render() {
        return (
            <Modal className={this.props.className} ref='modal' show={true} onHide={this.props.onHide}>
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
