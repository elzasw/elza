require("./Toastr.less")

/**
 *  Toastr.
 *  Pro data využívá ToastrStore.
 *  Pro přidání dat slouží ToastrActions.
 *
 *  Pro inicializaci staci naimportovat: import {Toastr} from 'components/index.jsx';
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

import React from 'react'
import {Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {Alert} from 'react-bootstrap';
import {connect} from 'react-redux'
import {addToastr, removeToastr} from './ToastrActions.jsx'

require('./Toastr.less');

const Toastr = class Toastr extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleDismiss')
    }

    handleDismiss(index) {
        this.dispatch(removeToastr(index));
    }

    static getIconStyle(style) {
        switch (style) {
            case 'success':
                return <Icon glyph="fa-check" />;
            case 'warning':
                return <Icon glyph="fa-exclamation" />;
            case 'info':
                return <Icon glyph="fa-info-circle" />;
            case 'danger':
                return <Icon glyph="fa-exclamation-circle" />;
        }
    }

    render() {
        const rows = this.props.store.toasts.map((t, index) => <Alert
                    key={'toast-' + index}
                    bsStyle={t.style}
                    bsSize={t.size ? t.size : "lg"}
                    className={t.visible && "fade"}
                    closeLabel={i18n('global.action.close')}
                    onDismiss={() => (this.handleDismiss(index))}
                    dismissAfter={t.time}
                >
                    {Toastr.getIconStyle(t.style)}
                    <div className="content">
                        <h4>{t.title}</h4>
                        <div>{t.message}</div>
                    </div>
            </Alert>
        );

        return (
            <div className="toastrAlertBox">
                {rows}
            </div>
        )
    }
};

Toastr.propsTypes = {
    store: React.PropTypes.object.isRequired
};

module.exports = connect((state) => ({store: state.toastr}))(Toastr);
