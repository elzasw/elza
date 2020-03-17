/**
 * Render Modálního dialogu ze store
 */
import React from 'react';
import {connect} from 'react-redux';
import {modalDialogHide} from 'actions/global/modalDialog.jsx';

import './ModalDialog.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import ModalDialogWrapper from './ModalDialogWrapper';

class ModalDialog extends AbstractReactComponent {
    /**
     *
     * @param closeType <ul>
     *          <li>DIALOG_CONTENT - vyvolal nějaký prvek uvnitř dialogu, např. tlačítko zavřít atp.</li>
     *          <li>DIALOG - vyvolal escape nebo kliknutí na zavírací křížek</li>
     *     </ul>
     */
    handleClose = closeType => {
        // console.log("_closeType", closeType);
        const {items} = this.props;
        this.props.dispatch(modalDialogHide());

        items.length > 0 && items[items.length - 1].onClose && items[items.length - 1].onClose(closeType);
    };

    render() {
        const {items} = this.props;
        if (items.length < 1) {
            return <div></div>;
        }

        const dialogs = items.map((dialog, index) => {
            const visible = index === items.length - 1;
            const children = React.Children.map(dialog.content, el =>
                React.cloneElement(el, {
                    onClose: this.handleClose.bind(this, 'DIALOG_CONTENT'),
                }),
            );

            return (
                <ModalDialogWrapper
                    key={index}
                    className={`${visible ? 'dialog-visible' : 'dialog-hidden'} ${dialog.dialogClassName}`}
                    title={dialog.title}
                    onHide={this.handleClose.bind(this, 'DIALOG')}
                >
                    {children}
                </ModalDialogWrapper>
            );
        });

        return <div>{dialogs}</div>;
    }
}

export default connect()(ModalDialog);
