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
        initialized: false,
        calendars: []
    };

    static validate = (value) => {
        const a = UnitDateConvertor.convertToUnitDate(value);
        // console.log('@@ 1', a, ' @@');
        return a;
    };

    componentDidMount() {
        this.dispatch(calendarTypesFetchIfNeeded());
        this.loadCalendarsAndPokeData();
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(calendarTypesFetchIfNeeded());
        this.loadCalendarsAndPokeData(nextProps);
    }

    loadCalendarsAndPokeData = (props = this.props) => {
        if (props.calendarTypes && props.calendarTypes.fetched && !this.state.initialized && props.calendarTypes.items) {
            if (props.fields.calendarTypeId.value == null || props.fields.calendarTypeId.value === "") {
                props.fields.calendarTypeId.onChange(props.calendarTypes.items[0].id);
            }
            this.setState({
                initialized: true,
                calendars: props.calendarTypes.items.map(i => <option value={i.id} key={i.id}>{i.name.charAt(0)}</option>)
            })
        }
    };

    render() {
        const {label, labelTextual, labelNote, fields} = this.props;
        const {allowedText, allowedNote, calendars, initialized} = this.state;

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
                    <FormInput type="text" {...fields.value} />
                </OverlayTrigger>
            </div>
            {allowedText && <FormInput type="text" {...fields.textDate} label={labelTextual} />}
            {allowedNote && <FormInput componentClass="textarea" {...fields.note} label={labelNote} />}
        </div>
    }
}
class LocalDateTime {

    date;

    constructor(date) {
        this.date = date;
    }

    static parse(string) {
        /*const date = Date.parse(string);
        if (isNaN(date)) {
            throw new Exception("Invalid format");
        }*/
        return new LocalDateTime(new Date(string));
    }
}
class Exception {
    message = null;
    constructor(string) {
        this.message = string;
    }
}

class UnitDate {
    calendarType;
    format;
    textDate;
    valueFrom;
    valueFromEstimate;
    valueTo;
    valueToEstimate;

    formatAppend = (format) => {
        if (this.format == null) {
            this.format = "";
        }
        this.format += format;
    };

    toISO8601 = (date) => {
        const isoString = date.toISOString();
        return isoString.substr(0, 19);
    }
}

class UnitDateConvertor {

    /**
     * Výraz pro detekci stolení
     */
    static EXP_CENTURY = "^(\\d+)((st)|(\\.[ ]?st\\.))$";

    /**
     * Zkratka století
     */
    static CENTURY = "C";

    /**
     * Výraz pro rok
     */
    static EXP_YEAR = "^(\\d+)$";

    /**
     * Zkratka roku
     */
    static YEAR = "Y";

    /**
     * Formát datumu
     */
    static FORMAT_DATE = "d.M.u";

    /**
     * Zkratka datumu
     */
    static DATE = "D";

    /**
     * Formát datumu s časem
     */
    static FORMAT_DATE_TIME = "d.M.u H:mm:ss";

    /**
     * Formát datumu s časem
     */
    static FORMAT_DATE_TIME2 = "d.M.u H:mm";

    /**
     * Zkratka datumu s časem
     */
    static DATE_TIME = "DT";

    /**
     * Formát roku s měsícem
     */
    static FORMAT_YEAR_MONTH = "";///"M.u";

    /**
     * Zkratka roku s měsícem
     */
    static YEAR_MONTH = "YM";

    /**
     * Oddělovač pro interval
     */
    static DEFAULT_INTERVAL_DELIMITER = "-";

    /**
     * Oddělovač pro interval, který vyjadřuje odhad
     */
    static ESTIMATE_INTERVAL_DELIMITER = "/";

    /**
     *
     * @param input
     * @returns {string}
     */
    static FORMATTER_YEAR_MONTH = (input) => {
        const data = input.split('.');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }
        let month = parseInt(data[0]);
        let year = parseInt(data[1]);

        if (isNaN(month) || isNaN(year) || month > 12 || month < 1) {
            throw new Exception('Invalid input.');
        }
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-01T00:00:00'))) {
            throw new Exception('Invalid input.');
        }

        return month + '.' + year;
    };
    /**
     *
     * @param input
     * @returns {string}
     */
    static PARSER_YEAR_MONTH = (input) => {
        const data = input.split('.');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }
        let month = parseInt(data[0]);
        let year = parseInt(data[1]);

        if (isNaN(month) || isNaN(year) || month > 12 || month < 1) {
            throw new Exception('Invalid input.');
        }
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-01T00:00:00Z'))) {
            throw new Exception('Invalid input.');
        }

        return new Date(sYear + '-' + sMonth + '-01T00:00:00Z');
    };

    static make2Digit = (input) => UnitDateConvertor.makeXChar(input, 2, '0');

    static make4Digit = (input) => UnitDateConvertor.makeXChar(input, 4, '0');

    static makeXChar = (s, width, char) => (s.length >= width) ? s : (new Array(width).join(char) + s).slice(-width);

    /**
     *
     * @param input
     * @return {string}
     */
    static FORMATTER_DATE_TIME = (input) => {
        const data = input.split(' ');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }

        const date = data[0].split('.');
        const time = data[1].split(':');
        if (date.length != 3 || time.length != 3) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let hours = parseInt(time[0]);
        let minutes = parseInt(time[1]);
        let secs = parseInt(time[2]);

        if (isNaN(hours) || isNaN(minutes) || isNaN(secs) || hours > 23 || hours < 0 || minutes > 59 || minutes < 0 || secs > 59 || secs < 0) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));
        let sHours = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(hours));
        let sMinutes = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(minutes));
        let sSecs = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(secs));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':' + sSecs))) {
            throw new Exception('Invalid input.');
        }

        return day + '.' + month + '.' + year + ' ' + hours + ':' + minutes + ':' + secs;
    };

    /**
     *
     * @param input
     * @return {string}
     */
    static PARSER_DATE_TIME = (input) => {
        const data = input.split(' ');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }

        const date = data[0].split('.');
        const time = data[1].split(':');
        if (date.length != 3 || time.length != 3) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let hours = parseInt(time[0]);
        let minutes = parseInt(time[1]);
        let secs = parseInt(time[2]);

        if (isNaN(hours) || isNaN(minutes) || isNaN(secs) || hours > 23 || hours < 0 || minutes > 59 || minutes < 0 || secs > 59 || secs < 0) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));
        let sHours = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(hours));
        let sMinutes = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(minutes));
        let sSecs = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(secs));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':' + sSecs))) {
            throw new Exception('Invalid input.');
        }

        return new Date(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':' + sSecs + 'Z')
    };

    /**
     *
     * @param input
     * @return {string}
     */
    static FORMATTER_DATE_TIME2 = (input) => {
        const data = input.split(' ');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }

        const date = data[0].split('.');
        const time = data[1].split(':');
        if (date.length != 3 || time.length != 2) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let hours = parseInt(time[0]);
        let minutes = parseInt(time[1]);

        if (isNaN(hours) || isNaN(minutes) || hours > 23 || hours < 0 || minutes > 59 || minutes < 0) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));
        let sHours = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(hours));
        let sMinutes = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(minutes));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':00'))) {
            throw new Exception('Invalid input.');
        }

        return day + '.' + month + '.' + year + ' ' + hours + ':' + minutes;
    };

    /**
     *
     * @param input
     * @return {string}
     */
    static PARSER_DATE_TIME2 = (input) => {
        const data = input.split(' ');
        if (data.length != 2) {
            throw new Exception('Invalid input.');
        }

        const date = data[0].split('.');
        const time = data[1].split(':');
        if (date.length != 3 || time.length != 2) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let hours = parseInt(time[0]);
        let minutes = parseInt(time[1]);

        if (isNaN(hours) || isNaN(minutes) || hours > 23 || hours < 0 || minutes > 59 || minutes < 0) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));
        let sHours = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(hours));
        let sMinutes = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(minutes));

        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':00'))) {
            throw new Exception('Invalid input.');
        }

        return new Date(sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':00Z');
    };

    /**
     *
     * @param input
     * @return {string}
     */
    static FORMATTER_DATE = (input) => {
        const date = input.split('.');
        if (date.length != 3) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));


        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T00:00:00'))) {
            throw new Exception('Invalid input.');
        }

        return day + '.' + month + '.' + year;
    };

    /**
     *
     * @param input
     * @return {string}
     */
    static PARSER_DATE = (input) => {
        const date = input.split('.');
        if (date.length != 3) {
            throw new Exception('Invalid input.');
        }

        let day = parseInt(date[0]);
        let month = parseInt(date[1]);
        let year = parseInt(date[2]);

        if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
            throw new Exception('Invalid input.');
        }

        let sDay = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(day));
        let sMonth = UnitDateConvertor.make2Digit(UnitDateConvertor.makeString(month));
        let sYear = UnitDateConvertor.make4Digit(UnitDateConvertor.makeString(year));


        if(isNaN(Date.parse(sYear + '-' + sMonth + '-' + sDay + 'T00:00:00'))) {
            throw new Exception('Invalid input.');
        }

        return new Date(sYear + '-' + sMonth + '-' + sDay + 'T00:00:00Z');
    };

    /**
     * Provede konverzi textového vstupu a doplní intervaly do objektu.
     *
     * @param input    textový vstup
     * @return doplněný objekt
     */
    static convertToUnitDate = (input) => {

        let unitdate = new UnitDate();

        unitdate.format = '';

        const normalizedInput = UnitDateConvertor.normalizeInput(input);

        try {
            if (UnitDateConvertor.isInterval(normalizedInput)) {
                unitdate = UnitDateConvertor.parseInterval(normalizedInput, unitdate);

                let from = null;
                if (unitdate.valueFrom != null) {
                    from = LocalDateTime.parse(unitdate.valueFrom);
                }

                let to = null;
                if (unitdate.valueTo != null) {
                    to = LocalDateTime.parse(unitdate.valueTo);
                }

                if (from.date != null && to.date != null && from.date > to.date) {
                    throw new Exception("Neplatný interval ISO datumů: od > do");
                }

            } else {
                const token = UnitDateConvertor.parseToken(normalizedInput, unitdate);
                unitdate.valueFrom = token.dateFrom;
                unitdate.valueFromEstimated = token.estimate;
                unitdate.valueTo = token.dateTo;
                unitdate.valueToEstimated = token.estimate;
            }

            if (unitdate.valueFrom != null) {
                const valueFrom = unitdate.toISO8601(unitdate.valueFrom);
                if (valueFrom.length != 19) {
                    throw new Exception("Neplatná délka ISO datumů");
                }
            }

            if (unitdate.valueTo != null) {
                const valueTo = unitdate.toISO8601(unitdate.valueTo);
                if (valueTo.length != 19) {
                    throw new Exception("Neplatná délka ISO datumů");
                }
            }

        } catch (e) {
            unitdate.format = '';
            throw new Exception("Vstupní řetězec není validní. " + (e && e.message ? e.message : ""));
        }

        return unitdate;
    };

    /**
     * Normalizace závorek (na hranaté) a odstranění bílých, přebytečných znaků.
     *
     * @param input text k normalizaci
     * @return normalizovaný text
     */
    static normalizeInput(input) {
        return input.replace("(", "[").replace(")", "]").trim();
    }

    static makeString(object) {
        if (object == null) return '';
        return '' + object;
    }

    static isEmptyString(string) {
        return UnitDateConvertor.makeString(string).length === 0;
    }

    /**
     * Převede {@link ParUnitdate} na string.
     *
     * @param unitdate datum
     * @return string
     */
    static convertParUnitDateToString(unitdate) {
        let textDate;
        if (UnitDateConvertor.isEmptyString(unitdate.textDate)) {
            try {
                textDate = UnitDateConvertor.convertToString(unitdate);
            } catch (e) {
                textDate = unitdate.textDate;
            }
        } else {
            textDate = unitdate.textDate;
        }
        return textDate;
    }

    /**
     * Provede konverzi formátu do textové podoby.
     */
    static convertToString(unitdate) {

        let format = unitdate.format;

        if (UnitDateConvertor.isInterval(format)) {
            format = UnitDateConvertor.convertInterval(format, unitdate);
        } else {
            format = UnitDateConvertor.convertToken(format, unitdate, true, true);
        }

        return format;
    }

    /**
     * Konverze intervalu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @return výsledný řetězec
     */
    static convertInterval(format, unitdate) {

        const data = format.split(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER);

        let ret;

        if (data.length != 2) {
            throw new Exception("Neplatný interval: " + format);
        }

        const bothEstimate = unitdate.valueFromEstimate && unitdate.valueToEstimate;

        ret = UnitDateConvertor.convertToken(data[0], unitdate, true, !bothEstimate);
        if (bothEstimate) {
            ret += UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER;
        } else {
            ret += UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER;
        }
        ret += UnitDateConvertor.convertToken(data[1], unitdate, false, !bothEstimate);

        return ret;
    }

    /**
     * Přidání odhadu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @param allow    povolit odhad?
     * @return výsledný řetězec
     */
    static addEstimate(format, unitdate, first, allow) {
        if (first) {
            if (unitdate.valueFromEstimate && allow) {
                format = "[" + format + "]";
            }
        } else {
            if (unitdate.valueToEstimate && allow) {
                format = "[" + format + "]";
            }
        }
        return format;
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format     vstupní formát
     * @param unitdate   doplňovaný objekt
     * @param first      zda-li se jedná o první datum
     * @param allow      povolit odhad?
     * @return výsledný řetězec
     */
    static convertToken(format, unitdate, first, allow) {

        if (format === "") {
            return format;
        }

        let ret;
        let canAddEstimate = true;
        switch (format) {
            case UnitDateConvertor.CENTURY:
                ret = UnitDateConvertor.convertCentury(format, unitdate, first);
                canAddEstimate = false;
                break;
            case UnitDateConvertor.YEAR:
                ret = UnitDateConvertor.convertYear(format, unitdate, first);
                break;
            case UnitDateConvertor.YEAR_MONTH:
                ret = UnitDateConvertor.convertYearMonth(format, unitdate, first);
                break;
            case UnitDateConvertor.DATE:
                ret = UnitDateConvertor.convertDate(format, unitdate, first);
                break;
            case UnitDateConvertor.DATE_TIME:
                ret = UnitDateConvertor.convertDateTime(format, unitdate, first);
                break;
            default:
                throw new Exception("Neexistující formát: " + format);
        }

        if (canAddEstimate) {
            ret = UnitDateConvertor.addEstimate(ret, unitdate, first, allow);
        }

        return ret;
    }

    /**
     * Konverze datumu s časem.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    static convertDateTime(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = LocalDateTime.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.DATE_TIME, "" + UnitDateConvertor.FORMATTER_DATE_TIME.format(date));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = LocalDateTime.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.DATE_TIME, "" + UnitDateConvertor.FORMATTER_DATE_TIME.format(date));
            }
        }
        return format;
    }

    /**
     * Konverze datumu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    static convertDate(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = LocalDateTime.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.DATE, "" + UnitDateConvertor.FORMATTER_DATE(date));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = LocalDateTime.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.DATE, "" + UnitDateConvertor.FORMATTER_DATE(date));
            }
        }
        return format;
    }

    /**
     * Konverze roku s měsícem.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    static convertYearMonth(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = LocalDateTime.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.YEAR_MONTH, "" + UnitDateConvertor.FORMATTER_YEAR_MONTH(date));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = LocalDateTime.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.YEAR_MONTH, "" + UnitDateConvertor.FORMATTER_YEAR_MONTH(date));
            }
        }
        return format;
    }

    /**
     * Konverze roku.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    static convertYear(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = LocalDateTime.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.YEAR, "" + date.getFullYear());
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = LocalDateTime.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.YEAR, "" + date.getFullYear());
            }
        }
        return format;
    }

    /**
     * Konverze stolení.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    static convertCentury(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = LocalDateTime.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.CENTURY, (date.getFullYear() / 100 + 1) + ". st.");
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = LocalDateTime.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.CENTURY, (date.getFullYear() / 100) + ". st.");
            }
        }
        return format;
    }

    /**
     * Parsování intervalu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     */
    static parseInterval(input, unitdate) {

        let data;
        if (input.indexOf(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER) !== -1) {
            data = input.split(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER);
        } else if (input.indexOf(UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER) !== -1) {
            data = input.split(UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER);
        } else {
            data = null;
        }


        if (data.length != 2) {
            throw new Exception("Neplatný interval: " + input);
        }

        const estimateBoth = input.indexOf(UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER) !== -1;

        let token = UnitDateConvertor.parseToken(data[0], unitdate);
        unitdate.valueFrom = token.dateFrom;
        unitdate.valueFromEstimated = token.estimate || estimateBoth;
        unitdate.formatAppend(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER);

        token = UnitDateConvertor.parseToken(data[1], unitdate);
        unitdate.valueTo = token.dateTo;
        unitdate.valueToEstimated = token.estimate || estimateBoth;

        return unitdate;
    }

    /**
     * Parsování tokenu.
     *
     * @param tokenString výraz
     * @param unitdate    doplňovaný objekt
     * @return výsledný token
     */
    static parseToken(tokenString, unitdate) {
        if (UnitDateConvertor.isEmptyString(tokenString)) {
            throw new Exception("Nemůže existovat prázdný interval");
        }

        let token = null;

        if (tokenString.charAt(0) == '[' && tokenString.charAt(tokenString.length - 1) == ']') {
            const tokenStringTrim = tokenString.substring(1, tokenString.length - 1);
            token = UnitDateConvertor.parseExpression(tokenStringTrim, unitdate);
            token.estimate = true;
        } else {
            token = UnitDateConvertor.parseExpression(tokenString, unitdate);
        }

        return token;
    }

    /**
     * Parsování výrazu.
     *
     * @param expression výraz
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    static parseExpression(expression, unitdate) {

        // console.log('pars', expression, expression.match(UnitDateConvertor.EXP_CENTURY), expression.match(UnitDateConvertor.EXP_YEAR), UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_YEAR_MONTH, expression), UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE_TIME, expression) || UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE_TIME2, expression), UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE, expression))

        if (expression.match(UnitDateConvertor.EXP_CENTURY)) {
            return UnitDateConvertor.parseCentury(expression, unitdate);
        } else if (expression.match(UnitDateConvertor.EXP_YEAR)) {
            return UnitDateConvertor.parseYear(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_YEAR_MONTH, expression)) {
            return UnitDateConvertor.parseYearMonth(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE_TIME, expression) || UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE_TIME2, expression)) {
            return UnitDateConvertor.parseDateTime(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(UnitDateConvertor.FORMATTER_DATE, expression)) {
            return UnitDateConvertor.parseDate(expression, unitdate);
        } else {
            throw new Exception();
        }

    }

    /**
     * Parsování roku s měsícem.
     *
     * @param yearMonthString rok s měsícem
     * @param unitdate        doplňovaný objekt
     * @return výsledný token
     */
    static parseYearMonth(yearMonthString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.YEAR_MONTH);

        const token = new Token();
        try {
            let date = UnitDateConvertor.PARSER_YEAR_MONTH(yearMonthString);
            token.dateFrom = date;
            let secDate = new Date(date);
            secDate.setMonth(secDate.getMonth()+1);
            secDate.setSeconds(secDate.getSeconds()-1);
            token.dateTo = secDate;
        } catch (e) {
            console.error(e);
            throw e;
        }

        return token;
    }

    /**
     * Parsování datumu s časem.
     *
     * @param dateString datum s časem
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    static parseDateTime(dateString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.DATE_TIME);

        const token = new Token();
        try {
            const date = UnitDateConvertor.PARSER_DATE_TIME(dateString);
            token.dateFrom = date;
            token.dateTo = date;
        } catch (e) {
            const date = UnitDateConvertor.PARSER_DATE_TIME2(dateString);
            token.dateFrom = date;
            let secDate = new Date(date);
            secDate.setSeconds(secDate.getSeconds()+59);
            token.dateTo = secDate;
        }

        return token;
    }

    /**
     * Parsování datumu.
     *
     * @param dateString datum
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    static parseDate(dateString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.DATE);

        const token = new Token();
        try {
            let date = UnitDateConvertor.PARSER_DATE(dateString);
            token.dateFrom = date;
            let secDate = new Date(date);
            secDate.setDate(secDate.getDate() + 1);
            secDate.setSeconds(secDate.getSeconds()-1);
            token.dateTo = secDate;
        } catch (e) {
            console.error(e);
            throw e;
        }

        return token;
    }

    /**
     * Parsování roku.
     *
     * @param yearString rok
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    static parseYear(yearString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.YEAR);
        const token = new Token();
        try {
            const year = parseInt(yearString);
            token.dateFrom = new Date(year, 0, 1, 0, 0);
            token.dateTo = new Date(year, 11, 31, 23, 59, 59); // 11 z důvodu dokumentace 11 = december = 12
        } catch (e) {
            console.error(e);
            throw e;
        }
        return token;
    }

    /**
     * Parsování stolení.
     *
     * @param centuryString stolení
     * @param unitdate      doplňovaný objekt
     * @return výsledný token
     */
    static parseCentury(centuryString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.CENTURY);
        const token = new Token();
        try {

            const matcher = centuryString.match(UnitDateConvertor.EXP_CENTURY);

            let c;

            if (matcher.length > 0) {
                c = parseInt(matcher[0]);
            } else {
                throw new Exception();
            }

            token.dateFrom = new Date((c - 1) * 100 + 1, 1, 1, 0, 0);
            token.dateTo = new Date(c * 100, 12, 31, 23, 59, 59);
            token.estimate = true;

        } catch ( e) {
            console.error(e);
            throw e;
        }
        return token;
    }

    /**
     * Testování, zda-li odpovídá řetězec formátu
     *
     * @param formatter formát
     * @param s         řetězec
     * @return true - lze parsovat
     */
    static tryParseDate(formatter, s) {
        try {
            formatter(s);
            return true;
        } catch (e) {
            return false;
        }
    }

    /**
     * Detekce, zda-li se jedná o interval
     *
     * @param input vstupní řetězec
     * @return true - jedná se o interval
     */
    static isInterval(input) {
        return input.indexOf(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER) !== -1 || input.indexOf(UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER) !== -1;
    }

}

window.q =  UnitDateConvertor;

/**
 * Pomocná třída pro reprezentaci jednoho výrazu.
 */
class Token {
    dateFrom = null;
    dateTo = null;
    estimate = false;
}


export default connect(state => ({
    calendarTypes: state.refTables.calendarTypes
}))(DatationField)

