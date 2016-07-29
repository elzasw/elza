import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {ModalDialogWrapper, AbstractReactComponent} from 'components/index.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {propsEquals} from 'components/Utils.jsx'

require('./ModalDialog.less')

const ModalDialog = class ModalDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClose');
    }

    componentWillReceiveProps(nextProps) {
        /*if (this.props.visible && !nextProps.visible) {
            this.refs.wrapper.dialogWillHide();
        }*/
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        return this.props.items.length  !== nextProps.items.length; // || !propsEquals(this.props.items, nextProps, ['content', 'title']);
    }

    // closeType:
    // DIALOG_CONTENT - vyvolal nějaký prvek uvnitř dialogu, např. tlačítko zavřít atp.
    // DIALOG - vyvolal escape nebo kliknutí na zavírací křížek
    handleClose(closeType) {
        // console.log("_closeType", closeType);
        this.dispatch(modalDialogHide());

        const {items} = this.props
        items.length > 0 && items[0].onClose && items[0].onClose(closeType)
    }

    render() {
        if (this.props.items.length < 1) {
            return <div></div>;
        }
        const dialog = this.props.items[0];

        const children = React.Children.map(dialog.content, (el) => React.cloneElement(el, {
                onClose: this.handleClose.bind(this, "DIALOG_CONTENT")
            })
        );

        return <ModalDialogWrapper className={dialog.dialogClassName} ref='wrapper' title={dialog.title} onHide={this.handleClose.bind(this, "DIALOG")}>
            {children}
        </ModalDialogWrapper>
    }
}

module.exports = connect()(ModalDialog);