import React from 'react';
import ReactDOM from 'react-dom';
import {i18n} from 'components/index.jsx';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils.jsx'

/**
 * Obal modálního dialogu
 */
export default class ModalDialogWrapper extends React.Component {

    constructor(props) {
        super(props);
        this.hide = false;
    }

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

    /**
     * Zajistí aby se callback na zavření dialogu zavolal vždy jen jednou! Bootstrap bug.
     */
    onHide = (e) => {
        const {onHide} = this.props;
        if (!this.hide) {
            onHide(e);
            this.hide = true;
        }
    };

    render() {
        const {title, onHide, className, children} = this.props;

        const renderHeader = title !== null;

        return (
            <Modal backdrop='static' className={className} ref='modal' show={true} onHide={this.onHide}>
                {renderHeader && <Modal.Header closeButton onHide={this.onHide}>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>}

                <div ref="modalBody" className="modal-body-container">
                    {children}
                </div>
            </Modal>
        );
    }
}
