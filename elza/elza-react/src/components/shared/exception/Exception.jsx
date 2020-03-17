import React from 'react';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';
import {connect} from 'react-redux';
import AbstractReactComponent from '../../AbstractReactComponent';
import NoFocusButton from '../button/NoFocusButton';
import ExceptionDetail from './ExceptionDetail';
import i18n from '../../i18n';

class Exception extends AbstractReactComponent {
    openDetail = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                this.props.title,
                <ExceptionDetail data={this.props.data} />,
                'dialog-lg top max-height',
            ),
        );
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

        if (textRenderer) {
            return textRenderer(data.properties ? data.properties : {}, data.message ? data.message : '');
        }
    };

    render() {
        return (
            <div>
                {this.renderText()}
                {this.renderDetail()}
            </div>
        );
    }
}

export default connect()(Exception);
