import PropTypes from 'prop-types';
import React from 'react'
import {createElement} from "react";
import {Alert} from 'react-bootstrap';
import {connect} from 'react-redux'
import {addToastr, removeToastr} from './ToastrActions.jsx'

import './Toastr.scss';
import AbstractReactComponent from "../../AbstractReactComponent";
import Icon from "../icon/Icon";
import i18n from "../../i18n";

/**
 *  Toastr.
 *  Pro data využívá ToastrStore.
 *  Pro přidání dat slouží ToastrActions.
 *
 *  Pro inicializaci staci naimportovat: import {Toastr} from 'components/shared';
 *
 *  Volání je pro typ hlášky:
 *  Danger:
 *      addToastrDanger('title', 'message,...)
 *  Info:
 *      addToastrInfo('title', 'message,...)
 *  Success:
 *      addToastrSuccess('title', 'message,...)
 *  Warning:
 *      addToastrWarning('title', 'message,...)
 **/
class Toastr extends AbstractReactComponent {

    static propTypes = {
        store: PropTypes.object.isRequired
    };

    handleDismiss = (index) => {
        this.props.dispatch(removeToastr(index));
    };

    static getIconStyle(style) {
        switch (style) {
            case 'success':
                return <Icon glyph="fa-check"/>;
            case 'warning':
                return <Icon glyph="fa-exclamation"/>;
            case 'info':
                return <Icon glyph="fa-info-circle"/>;
            case 'danger':
                return <Icon glyph="fa-exclamation-circle"/>;
        }
    }

    render() {
        const rows = this.props.store.toasts.map((t, index) => {
            if (t.time != null) {
                setTimeout(() => this.handleDismiss(index), t.time);
            }

            let message;
            if (t.extended) {
                message = <div>
                    {createElement(t.messageComponent, {key:"message", ...t.messageComponentProps, onClose: () => this.handleDismiss(index)})}
                </div>
            } else {
                message = <div>{t.message}</div>;
            }

            return <Alert
                key={'toast-' + index}
                variant={t.style}
                bsSize={t.size ? t.size : "lg"}
                className={t.visible && "fade"}
                closeLabel={i18n('global.action.close')}
                onClose={() => (this.handleDismiss(index))}
            >
                <div className="icon-container">{Toastr.getIconStyle(t.style)}</div>
                <div className="content">
                    <h4>{t.title}</h4>
                    {message}
                </div>
            </Alert>
        });

        return (
            <div className="toastrAlertBox">
                {rows}
            </div>
        )
    }
}

export default connect((state) => ({store: state.toastr}))(Toastr);
