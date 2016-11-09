import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {ModalDialogWrapper, AbstractReactComponent} from 'components/index.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {propsEquals} from 'components/Utils.jsx'

import './ModalDialog.less'

/**
 * Render Modálního dialogu ze store
 */
class ModalDialog extends AbstractReactComponent {

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        return this.props.items.length  !== nextProps.items.length; // || !propsEquals(this.props.items, nextProps, ['content', 'title']);
    }

    /**
     *
     * @param closeType <ul>
     *          <li>DIALOG_CONTENT - vyvolal nějaký prvek uvnitř dialogu, např. tlačítko zavřít atp.</li>
     *          <li>DIALOG - vyvolal escape nebo kliknutí na zavírací křížek</li>
     *     </ul>
     */
    handleClose = (closeType) => {
        // console.log("_closeType", closeType);
        this.dispatch(modalDialogHide());

        const {items} = this.props;
        items.length > 0 && items[0].onClose && items[0].onClose(closeType)
    };

    render() {
        const {items} = this.props;
        if (items.length < 1) {
            return <div></div>;
        }

        const dialogs = items.map((dialog, index) => {
            const visible = index === items.length - 1;
            const children = React.Children.map(dialog.content, (el) => React.cloneElement(el, {
                    onClose: this.handleClose.bind(this, "DIALOG_CONTENT")
                })
            );

            return (
                <ModalDialogWrapper key={index} className={`${visible ? "dialog-visible" : "dialog-hidden"} ${dialog.dialogClassName}`} title={dialog.title} onHide={this.handleClose.bind(this, "DIALOG")}>
                    {children}
                </ModalDialogWrapper>
            )
        });

        return <div>
            {dialogs}
        </div>
    }
}

export default connect()(ModalDialog);