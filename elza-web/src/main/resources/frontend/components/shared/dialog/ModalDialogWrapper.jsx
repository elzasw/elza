import React from 'react';
import ReactDOM from 'react-dom';
import {i18n} from 'components/index.jsx';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils.jsx'

/**
 * Obal modálního dialogu
 */
export default class ModalDialogWrapper extends React.Component {
    componentDidMount() {
        this.setState({}, () => {
            if (this.refs.modalBody) {
                const el = ReactDOM.findDOMNode(this.refs.modalBody);
                if (el) {
                    setInputFocus(el, false);
                }
            }
        })
    }

    dialogWillHide = () => {
        this.refs.modal._onHide();
    };

    render() {
        const {title, onHide, className, children} = this.props;

        const renderHeader = title !== null;

        return (
            <Modal backdrop='static' className={className} ref='modal' show={true} onHide={onHide}>
                {renderHeader && <Modal.Header closeButton onHide={onHide}>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>}

                <div ref="modalBody" className="modal-body-container">
                    {children}
                </div>
            </Modal>
        );
    }
}
