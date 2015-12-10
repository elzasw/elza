require("./Toastr.less")

/**
 *  Toastr.
 *  Pro data využívá ToastrStore.
 *  Pro přidání dat slouží ToastrActions.
 *
 *  Pro inicializaci staci naimportovat: import {Toastr} from 'components'
 *
 *  Volání je pro typ hlášky:
 *  Danger:
 *      Toastr.Actions.danger({title: 'Hlavička', message: 'Zpráva'});
 *  Info:
 *      Toastr.Actions.info({title: 'Hlavička', message: 'Zpráva'});
 *  Success:
 *      Toastr.Actions.success({title: 'Hlavička', message: 'Zpráva'});
 *  Warning:
 *      Toastr.Actions.warning({title: 'Hlavička', message: 'Zpráva'});
 *  Clear:
 *      Toastr.Actions.clear({title: 'Hlavička', message: 'Zpráva'});
**/

var React = require('react');
var assign = require('react/lib/Object.assign');

import {i18n} from 'components';
import {Alert, Button, Glyphicon} from 'react-bootstrap';
var ToastrStore = require('./ToastrStore');
var ToastrActions = require('./ToastrActions');

module.exports = class Toastr extends React.Component {
    constructor(props) {
        super(props);
        this.state = assign({}, {toasters: ToastrStore.getInitialData()});
        this.lastId = 1;
    }
    componentDidMount() {
        this.unsubscribe = ToastrStore.listen(this.onToastr.bind(this));
    }
    componentWillUnmount() {
        this.unsubscribe();
    }
    onToastr(data) {
        this.setState(assign({}, {toasters: data}));
    }
    handleAlertDismiss(t) {
        ToastrActions.clear(t.id);
    }
    render() {

        var rows = this.state.toasters.map((t) => {
            var icon = null;
            switch (t.type) {
                case 'success':
                    icon = <Glyphicon glyph="ok" />;
                break;
                case 'warning':
                    icon = <Glyphicon glyph="warning-sign" />;
                break;
                case 'info':
                    icon = <Glyphicon glyph="info-sign" />;
                break;
                case 'danger':
                    icon = <Glyphicon glyph="exclamation-sign" />;
                break;
            }
            return (
                <Alert
                    bsStyle={t.type}
                    bsSize="lg"
                    key={t.key}
                    className={t.visible && "fade"}
                    closeLabel={i18n('global.action.close')}
                    onDismiss={this.handleAlertDismiss.bind(null,t)}
                    dismissAfter={t.dismissAfter}
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
}
