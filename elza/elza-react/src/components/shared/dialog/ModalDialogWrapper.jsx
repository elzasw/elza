import React from 'react';
import ReactDOM from 'react-dom';
import {Modal} from 'react-bootstrap';
import {setInputFocus} from 'components/Utils.jsx';

/**
 * Obal modálního dialogu
 */
class ModalDialogWrapper extends React.Component {
    hide = false;

    componentDidMount() {
        this.setState({}, () => {
            if (this.refs.modalBody) {
                const el = ReactDOM.findDOMNode(this.refs.modalBody);
                if (el) {
                    setInputFocus(el, false);
                }
            }
        });
    }

    // dialogWillHide = () => {
    //     this.refs.modal._onHide();
    // };

    /**
     * Zajistí aby se callback na zavření dialogu zavolal vždy jen jednou! Bootstrap bug.
     */
    onHide = e => {
        const {onHide} = this.props;
        onHide && onHide(e);
        // if (!this.hide) {
        //     onHide && onHide(e);
        //     this.hide = true;
        // }
    };

    render() {
        const {title, className, children} = this.props;

        const renderHeader = title !== null;

        return (
            <Modal
                backdrop={'static'}
                className={className}
                ref="modal"
                show={true}
                // onHide={this.onHide}
                maskClosable={false}
            >
                {renderHeader && (
                    <Modal.Header closeButton onHide={this.onHide}>
                        <Modal.Title>{title}</Modal.Title>
                    </Modal.Header>
                )}

                <div ref="modalBody" className="modal-body-container">
                    {children}
                </div>
            </Modal>
        );
    }
}

export default ModalDialogWrapper;
