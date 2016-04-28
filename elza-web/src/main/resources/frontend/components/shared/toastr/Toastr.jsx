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

var React = require('react');

import {Icon, i18n, AbstractReactComponent} from 'components/index.jsx';
import {Alert} from 'react-bootstrap';
import {connect} from 'react-redux'
import {addToastr,removeToastr} from './ToastrActions.jsx'

var Toastr = class Toastr extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.state = {};
        this.bindMethods('handleDismiss')
    }

    handleDismiss(index) {
        this.dispatch(removeToastr(index));
    }

    render() {
        var rows = this.props.store.toasts.map((t, index) => {
            var icon = null;
            switch (t.style) {
                case 'success':
                    icon = <Icon glyph="fa-check" />;
                break;
                case 'warning':
                    icon = <Icon glyph="fa-exclamation" />;
                break;
                case 'info':
                    icon = <Icon glyph="fa-info-circle" />;
                break;
                case 'danger':
                    icon = <Icon glyph="fa-exclamation-circle" />;
                break;
            }
            return (
                <Alert
                    bsStyle={t.style}
                    bsSize={t.size ? t.size : "lg"}
                    key={t.key}
                    className={t.visible && "fade"}
                    closeLabel={i18n('global.action.close')}
                    onDismiss={() => (this.handleDismiss(index))}
                    dismissAfter={t.time}
                >
                    {icon}
                    <div className="content">
                        <h4>{t.title}</h4>
                        <p>{t.message}</p>
                    </div>
                </Alert>
            );
        });

        return (
            <div className="toastrAlertBox">
                {rows}
            </div>
        )
    }
};

function mapStateToProps(state) {
    return {
        store: state.toastr
    }
}

module.exports = connect(mapStateToProps)(Toastr);
