import PropTypes from 'prop-types';
import React, {createElement} from 'react';
import {Alert} from 'react-bootstrap';
import {connect} from 'react-redux';
import {removeToastr} from './ToastrActions.jsx';

import './Toastr.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import Icon from '../icon/Icon';
import i18n from '../../i18n';

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
        store: PropTypes.object.isRequired,
    };

    handleDismiss = (key) => {
        this.props.dispatch(removeToastr(key));
    };

    static getIconStyle(style) {
        switch (style) {
            case 'success':
                return <Icon glyph="fa-check-circle" />;
            case 'warning':
                return <Icon glyph="fa-exclamation-triangle" />;
            case 'info':
                return <Icon glyph="fa-info-circle" />;
            case 'danger':
                return <Icon glyph="fa-exclamation-circle" />;
            default:
                return null;
        }
    }

    render() {
        const rows = this.props.store.toasts.map((toast) => {
            if (toast.time != null) {
                setTimeout(() => this.handleDismiss(toast.key), toast.time);
            }

            let message;
            if (toast.extended) {
                message = (
                    <div>
                        {createElement(toast.messageComponent, {
                            key: 'message',
                            ...toast.messageComponentProps,
                            onClose: () => this.handleDismiss(toast.key),
                        })}
                    </div>
                );
            } else {
                message = <div>{toast.message}</div>;
            }

            return (
                <Alert
                    key={'toast-' + toast.key}
                    variant={toast.style}
                    className={toast.visible && 'fade'}
                    closeLabel={i18n('global.action.close')}
                    onClose={() => this.handleDismiss(toast.key)}
                    dismissible
                >
                    <div className="icon-container">{Toastr.getIconStyle(toast.style)}</div>
                    <div className="content">
                        <h4>{toast.title}</h4>
                        {message}
                    </div>
                </Alert>
            );
        });

        return <div className="toastrAlertBox">{rows}</div>;
    }
}

export default connect(state => ({store: state.toastr}))(Toastr);
