import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {ModalDialogWrapper, AbstractReactComponent} from 'components';
import {modalDialogHide} from 'actions/global/modalDialog'

var ModalDialog = class extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClose');
    }

    handleClose() {
        this.dispatch(modalDialogHide());
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
            <ModalDialogWrapper title={this.props.title} onHide={this.handleClose}>
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