import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {Button, Tooltip, OverlayTrigger} from 'react-bootstrap';
import {AbstractReactComponent, Icon, FormInput} from 'components';
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'

import './DatationField.less'

class DatationField extends AbstractReactComponent {
    static PropTypes = {
        label: React.PropTypes.string.isRequired,
        labelTextual: React.PropTypes.string.isRequired,
        labelNote: React.PropTypes.string.isRequired,
        fields: React.PropTypes.object.isRequired
    };

    state = {
        allowedText: this.props.fields.textDate && this.props.fields.textDate.value != null && this.props.fields.textDate.value != "",
        allowedNote: this.props.fields.textDate && this.props.fields.note.value != null && this.props.fields.note.value != "",
    };

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());
    }

    render() {
        const {label, calendarTypes, labelTextual, labelNote, fields} = this.props;
        const {allowedText, allowedNote} = this.state;
        const calendars = calendarTypes ? calendarTypes.items.map(i => <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>) : [];

        const tooltip = <Tooltip id='tt'>
            <b>Formát datace</b><br />
            Století: 20. st. <i>nebo</i> 20.st. <i>nebo</i> 20st<br />
            Rok: 1968<br />
            Měsíc.rok: 8.1968<br />
            Datum: 21.8.1698<br />
            Datum a čas: 21.8.1968 8:23 <i>nebo</i> 21.8.1968 8:23:31<br />
            <b>Intervaly</b><br />
            Jednotlivá hodnota: 1968<br />
            Interval: 21.8.1968 0:00-27.6.1989<br />
            <b>Odhad</b><br />
            Definuje se uzavřením hodnoty do kulatých nebo hranatých závorek: [16.8.1977]<br />
            Při použití znaku "/" pro oddělení intervalu jsou od i do chápány jako odhad.
        </Tooltip>

        return <div className="datation-field">
            <div className="header">
                <label>{label}</label>
                <Button bsStyle="action" className={allowedText ? '' : 'disabledColor'} onClick={() => this.setState({allowedText: !allowedText})}><Icon glyph="fa-font" /></Button>
                <Button bsStyle="action" className={allowedNote ? '' : 'disabledColor'} onClick={() => this.setState({allowedNote: !allowedNote})}><Icon glyph="fa-sticky-note-o" /></Button>
            </div>
            <div className="datation">
                <FormInput componentClass="select" {...fields.calendarTypeId}>
                    {calendars}
                </FormInput>
                <OverlayTrigger overlay={tooltip} placement="bottom">
                    <FormInput type="text" {...fields.valueFrom} />
                </OverlayTrigger>
            </div>
            {allowedText && <FormInput type="text" {...fields.textDate} label={labelTextual} />}
            {allowedNote && <FormInput componentClass="textarea" {...fields.note} label={labelNote} />}
        </div>
    }
}

export default connect(state => ({
    calendarTypes: state.refTables.calendarTypes
}))(DatationField)

