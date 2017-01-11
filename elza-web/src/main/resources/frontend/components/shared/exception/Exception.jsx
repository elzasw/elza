import React from 'react';
import {AbstractReactComponent, ExceptionDetail, NoFocusButton} from 'components/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {i18n} from 'components'
import {connect} from 'react-redux'

var Exception = class Exception extends AbstractReactComponent {

    constructor(props) {
        super(props);
    }

    openDetail = () => {
        this.dispatch(modalDialogShow(this, this.props.title, <ExceptionDetail data={this.props.data} />, "dialog-lg top max-height"));
    };

    renderDetail = () => {
        return (
            <div>
                <NoFocusButton onClick={this.openDetail}>{i18n('global.exception.detail')}</NoFocusButton>
            </div>
        );
    };

    renderText = () => {
        const {textRenderer, data} = this.props;

        if (textRenderer && data.properties) {
            return textRenderer(data.properties);
        }
    };

    render() {
        return (
            <div>
                {this.renderText()}
                {this.renderDetail()}
            </div>
        )
    }
};

module.exports = connect()(Exception);
