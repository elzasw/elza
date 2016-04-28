import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {ModalDialogWrapper, AbstractReactComponent} from 'components/index.jsx';
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {propsEquals} from 'components/Utils.jsx'

require ('./ModalDialog.less')

var ModalDialog = class extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClose');
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.visible && !nextProps.visible) {
            this.refs.wrapper.dialogWillHide();
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['visible', 'content', 'title']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    handleClose() {
        this.dispatch(modalDialogHide());

        const {onClose} = this.props
        onClose && onClose()
    }

    render() {
        if (!this.props.visible) {
            return <div></div>;
        }
        var style = {};

        var children = React.Children.map(this.props.content, (el) => {
            return React.cloneElement(el, {
                onClose: this.handleClose
            })
        });

        return (
            <ModalDialogWrapper className={this.props.dialogClassName} ref='wrapper' title={this.props.title} onHide={this.handleClose}>
                {children}
            </ModalDialogWrapper>
        )
    }
}

function mapStateToProps(state) {
    return {
        ...state
    }
}

module.exports = connect(mapStateToProps)(ModalDialog);