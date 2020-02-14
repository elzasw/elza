import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {Button, Tooltip, OverlayTrigger} from 'react-bootstrap';
import {TooltipTrigger, AbstractReactComponent, Icon, FormInput, i18n} from 'components/shared';
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'

import './DatationField.scss'

class DatationField extends AbstractReactComponent {
    static propTypes = {
        label: PropTypes.string.isRequired,
        labelTextual: PropTypes.string.isRequired,
        labelNote: PropTypes.string.isRequired,
        fields: PropTypes.object.isRequired
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

    static reduxValidate = (val) => {
        let errors = null;
        if (val && val.value) {
            let datation, err;
            try {
                datation = DatationField.validate(val.value);
            } catch (e) {
                err = e;
            }
            if (!datation) {
                errors = {
                    value: err && err.message ? err.message : ' '
                };
            }

            if (!val.calendarTypeId) {
                if (errors) {
                    errors.calendarTypeId = i18n('global.validation.required');
                } else {
                    errors = {
                        calendarTypeId: i18n('global.validation.required')
                    };
                }
            }
        }
        return errors;
    };

    componentDidMount() {
        this.props.dispatch(calendarTypesFetchIfNeeded());
        this.loadCalendarsAndPokeData();
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(calendarTypesFetchIfNeeded());
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

        const tooltipText = i18n("^dataType.unitdate.format");
        const tooltip = tooltipText ? <div dangerouslySetInnerHTML={{__html: tooltipText}}></div> : null;

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
                <TooltipTrigger
                    content={tooltip}
                    holdOnHover
                    placement="vertical"
                >
                    <FormInput type="text" {...fields.value} />
                </TooltipTrigger>
            </div>
            {allowedText && <FormInput type="text" {...fields.textDate} label={labelTextual} />}
            {allowedNote && <FormInput componentClass="textarea" {...fields.note} label={labelNote} />}
        </div>
    }
}


class Exception {
    message = null;
    hasUserMessage = false;
    constructor(string, hasUserMessage = false) {
        this.message = string;
        this.hasUserMessage = hasUserMessage;
    }
}

const makeString = (object) => object == null ? '' : '' + object;

const makeXChar = (s, width, char) => (s.length >= width) ? s : (new Array(width).join(char) + s).slice(-width);

const make2Digit = (input) => makeXChar(input, 2, '0');

const make4Digit = (input) => makeXChar(input, 4, '0');


class DT {

    static FORMATS = {
        _DT: '_DT', // CLASS format - input is this class
        DATE: "d.M.u",
        DATE_TIME: "d.M.u H:mm:ss",
        DATE_TIME2: "d.M.u H:mm",
        YEAR_MONTH: "M.u",
        YEAR: "u"
    };

    jsDate;

    constructor(input = null, format = DT.FORMATS._DT) {
        let dateString;
        let validationArray = [];

        if (input === null && format == DT.FORMATS._DT) {
            this.jsDate = new Date();
            return;
        }

        switch (format) {
            case DT.FORMATS._DT: {
                this.jsDate = new Date(input.jsDate);
                return;
            }
            case DT.FORMATS.YEAR:
            case DT.FORMATS.YEAR_MONTH: {
                const hasMonth = format == DT.FORMATS.YEAR_MONTH;

                let data, month, year;
                if (hasMonth) {
                    data = input.split('.');
                    if (data.length != 2) {
                        throw new Exception('Invalid input.');
                    }
                    month = parseInt(data[0]);
                } else {
                    data = [null, input];
                    month = 1;
                }

                year = parseInt(data[1]);
                validationArray.push(year);
                hasMonth && validationArray.push(month);

                if ((hasMonth && (isNaN(month) || month > 12 || month < 1)) || isNaN(year)) {
                    throw new Exception('Invalid input.');
                }

                let sMonth = make2Digit(makeString(month));
                let sYear = make4Digit(makeString(year));

                if(isNaN(Date.parse(sYear + '-' + sMonth + '-01T00:00:00Z'))) {
                    throw new Exception('Invalid input.');
                }

                dateString = sYear + '-' + sMonth + '-01T00:00:00Z';
                break;
            }
            case DT.FORMATS.DATE:
            case DT.FORMATS.DATE_TIME:
            case DT.FORMATS.DATE_TIME2: {
                const hasSeconds = format == DT.FORMATS.DATE_TIME;
                const hasTime = hasSeconds || format == DT.FORMATS.DATE_TIME2;
                let data, date, time;
                if (hasTime) {
                    data = input.split(' ');
                    if (data.length != 2) {
                        throw new Exception('Invalid input.');
                    }

                    time = data[1].split(':');
                } else {
                    data = [input];
                }

                date = data[0].split('.');

                if (date.length != 3 || (hasTime && time.length != 3)) {
                    throw new Exception('Invalid input.');
                }

                let day = parseInt(date[0]);
                let month = parseInt(date[1]);
                let year = parseInt(date[2]);

                if (isNaN(day) || isNaN(month) || isNaN(year) || month > 12 || month < 1 || day > 31 || day < 1) {
                    throw new Exception('Invalid input.');
                }
                validationArray.push(year, month, day);

                let hours, minutes, secs;
                if (hasTime) {
                    hours = parseInt(time[0]);
                    minutes = parseInt(time[1]);
                    secs = parseInt(time[2]);

                    if (isNaN(hours) || isNaN(minutes) || isNaN(secs) || hours > 23 || hours < 0 || minutes > 59 || minutes < 0 || secs > 59 || secs < 0) {
                        throw new Exception('Invalid input.');
                    }
                    validationArray.push(hours, minutes, secs)
                } else {
                    hours = 0;
                    minutes = 0;
                    secs = 0;
                }

                let sDay = make2Digit(makeString(day));
                let sMonth = make2Digit(makeString(month));
                let sYear = make4Digit(makeString(year));
                let sHours = make2Digit(makeString(hours));
                let sMinutes = make2Digit(makeString(minutes));
                let sSecs = make2Digit(makeString(secs));

                dateString = sYear + '-' + sMonth + '-' + sDay + 'T' + sHours + ':' + sMinutes + ':' + sSecs;
                break;
            }
            default:{
                throw new Exception(i18n('global.validation.datation.invalidFormat', format), true);
            }
        }
        if(isNaN(Date.parse(dateString))) {
            throw new Exception('Invalid input.');
        }
        this.jsDate = new Date(dateString);
        DT.validateDate(this.jsDate, validationArray);
    }

    static getDateArray = (i) => [i.getFullYear(), i.getMonth()+1, i.getDate(), i.getHours(), i.getMinutes(), i.getSeconds(), i.getMilliseconds()];

    static validateDate = (date, arr) => {
        const dateArr = DT.getDateArray(date);
        const assertTrue = (arg) => {
            if (!arg) {
                throw new Exception('Invalid input.')
            }
        };

        let index = 0;
        for (let a of arr) {
            assertTrue(a === dateArr[index++])
        }
    };

    static parse = (string) => {
        return new DT(makeString(string).replace('T', ' '), DT.FORMATS.DATE_TIME);
    };

    format = (format) => {
        switch (format) {
            case DT.FORMATS.YEAR:
                return this.getYear();
            case DT.FORMATS.YEAR_MONTH:
                return this.getMonth() + '.' + this.getYear();
            case DT.FORMATS.DATE:
                return this.getDay()  + '.' + this.getMonth() + '.' + this.getYear();
            case DT.FORMATS.DATE_TIME2:
                return this.getDay()  + '.' + this.getMonth() + '.' + this.getYear() + ' ' + this.getHours() + ':' + this.getMinutes();
            case DT.FORMATS.DATE_TIME:
                return this.getDay()  + '.' + this.getMonth() + '.' + this.getYear() + ' ' + this.getHours() + ':' + this.getMinutes() + ':' + this.getSeconds();
        }
    };

    isEqual = (anotherMyTime) => this.jsDate.getTime() === anotherMyTime.jsDate.getTime();
    isBefore = (anotherMyTime) => this.jsDate < anotherMyTime.jsDate;
    isAfter = (anotherMyTime) => this.jsDate > anotherMyTime.jsDate;

    plusMilliseconds = (i) => this.jsDate.setMilliseconds(this.jsDate.getMilliseconds() + i);
    plusSeconds = (i) => this.jsDate.setSeconds(this.jsDate.getSeconds() + i);
    plusMinutes = (i) => this.jsDate.setMinutes(this.jsDate.getMinutes() + i);
    plusHours = (i) => this.jsDate.setHours(this.jsDate.getHours() + i);
    plusDays = (i) => this.jsDate.setDate(this.jsDate.getDate() + i);
    plusMonths = (i) => this.jsDate.setMonth(this.jsDate.getMonth() + i);
    plusYears = (i) => this.jsDate.setFullYear(this.jsDate.getFullYear() + i);

    minusMilliseconds = (i) => this.jsDate.setMilliseconds(this.jsDate.getMilliseconds() - i);
    minusSeconds = (i) => this.jsDate.setSeconds(this.jsDate.getSeconds() - i);
    minusMinutes = (i) => this.jsDate.setMinutes(this.jsDate.getMinutes() - i);
    minusHours = (i) => this.jsDate.setHours(this.jsDate.getHours() - i);
    minusDays = (i) => this.jsDate.setDate(this.jsDate.getDate() - i);
    minusMonths = (i) => this.jsDate.setMonth(this.jsDate.getMonth() - i);
    minusYears = (i) => this.jsDate.setFullYear(this.jsDate.getFullYear() - i);

    setMilliseconds = (i) => this.jsDate.setMilliseconds(i);
    setSeconds = (i) => this.jsDate.setSeconds(i);
    setMinutes = (i) => this.jsDate.setMinutes(i);
    setHours = (i) => this.jsDate.setHours(i);
    setDay = (i) => this.jsDate.setDate(i);
    setMonth = (i) => this.jsDate.setMonth(i-1);
    setYear = (i) => this.jsDate.setFullYear(i);

    getMilliseconds = () => this.jsDate.getMilliseconds();
    getSeconds = () => this.jsDate.getSeconds();
    getMinutes = () => this.jsDate.getMinutes();
    getHours = () => this.jsDate.getHours();
    getDay = () => this.jsDate.getDate();
    getMonth = () => this.jsDate.getMonth()+1;
    getYear = () => this.jsDate.getFullYear();

    toISO8601 = () => this.jsDate.toISOString().substr(0, 19);

    setDateArray = (arr) => {
        if (!arr || !arr.length || arr.length < 1) {
            throw new Exception("Invalid array input.")
        }
        this.jsDate = new Date(...arr);
        this.setYear(arr[0]);
        if (arr.length > 1) {
            this.setMonth(arr[1]);
        }
        DT.validateDate(this.jsDate, arr);
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
     * Zkratka datumu
     */
    static DATE = "D";

    /**
     * Zkratka datumu s časem
     */
    static DATE_TIME = "DT";

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

    static convertToUnitDate = (input) => {

        let unitdate = new UnitDate();

        unitdate.format = '';

        const normalizedInput = UnitDateConvertor.normalizeInput(input);

        try {
            if (UnitDateConvertor.isInterval(normalizedInput)) {
                unitdate = UnitDateConvertor.parseInterval(normalizedInput, unitdate);

                let from = null;
                if (unitdate.valueFrom != null) {
                    from = unitdate.valueFrom;
                }

                let to = null;
                if (unitdate.valueTo != null) {
                    to = unitdate.valueTo;
                }

                if (from != null && to != null && from.isAfter(to)) {
                    throw new Exception(i18n('global.validation.datation.invalidInterval'));
                }

            } else {
                const token = UnitDateConvertor.parseToken(normalizedInput, unitdate);
                unitdate.valueFrom = token.dateFrom;
                unitdate.valueFromEstimated = token.estimate;
                unitdate.valueTo = token.dateTo;
                unitdate.valueToEstimated = token.estimate;
            }

            if (unitdate.valueFrom != null) {
                const valueFrom = unitdate.valueFrom.toISO8601();
                if (valueFrom.length != 19) {
                    throw new Exception(i18n('global.validation.datation.invalidISODateLength'));
                }
            }

            if (unitdate.valueTo != null) {
                const valueTo = unitdate.valueTo.toISO8601();
                if (valueTo.length != 19) {
                    throw new Exception(i18n('global.validation.datation.invalidISODateLength'));
                }
            }

        } catch (e) {
            unitdate.format = '';
            console.log(e);
            throw new Exception(e && e.message && e.hasUserMessage ? e.message : i18n('global.validation.datation.invalid'));
        }

        return unitdate;
    };

    /**
     * Normalizace závorek (na hranaté) a odstranění bílých, přebytečných znaků.
     *
     * @param input text k normalizaci
     * @return {string} normalizovaný text
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
     * @return {string} výsledný řetězec
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
     * @return {string} výsledný řetězec
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
     * @return {string} výsledný řetězec
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
     * @return {string} výsledný řetězec
     */
    static convertDateTime(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = DT.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.DATE_TIME, "" + date.format(DT.FORMATS.DATE_TIME));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = DT.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.DATE_TIME, "" + date.format(DT.FORMATS.DATE_TIME));
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
     * @return {string} výsledný řetězec
     */
    static convertDate(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = DT.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.DATE, "" + date.format(DT.FORMATS.DATE));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = DT.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.DATE, "" + date.format(DT.FORMATS.DATE));
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
     * @return {string} výsledný řetězec
     */
    static convertYearMonth(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = DT.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.YEAR_MONTH, "" + date.format(DT.FORMATS.YEAR_MONTH));
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = DT.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.YEAR_MONTH, "" + date.format(DT.FORMATS.YEAR_MONTH));
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
     * @return {string} výsledný řetězec
     */
    static convertYear(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = DT.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.YEAR, "" + date.getYear());
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = DT.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.YEAR, "" + date.getYear());
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
     * @return {string} výsledný řetězec
     */
    static convertCentury(format, unitdate, first) {
        if (first) {
            if (unitdate.valueFrom != null) {
                const date = DT.parse(unitdate.valueFrom);
                return format.replace(UnitDateConvertor.CENTURY, (date.getYear() / 100 + 1) + ". st.");
            }
        } else {
            if (unitdate.valueTo != null) {
                const date = DT.parse(unitdate.valueTo);
                return format.replace(UnitDateConvertor.CENTURY, (date.getYear() / 100) + ". st.");
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
     * @return {Token} výsledný token
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
     * @return {Token} výsledný token
     */
    static parseExpression(expression, unitdate) {

        if (expression.match(UnitDateConvertor.EXP_CENTURY)) {
            return UnitDateConvertor.parseCentury(expression, unitdate);
        } else if (expression.match(UnitDateConvertor.EXP_YEAR)) {
            return UnitDateConvertor.parseYear(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(DT.FORMATS.YEAR_MONTH, expression)) {
            return UnitDateConvertor.parseYearMonth(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(DT.FORMATS.DATE_TIME, expression) || UnitDateConvertor.tryParseDate(DT.FORMATS.DATE_TIME2, expression)) {
            return UnitDateConvertor.parseDateTime(expression, unitdate);
        } else if (UnitDateConvertor.tryParseDate(DT.FORMATS.DATE, expression)) {
            return UnitDateConvertor.parseDate(expression, unitdate);
        } else {
            throw new Exception('Unknown format in parseException');
        }
    }

    /**
     * Parsování roku s měsícem.
     *
     * @param yearMonthString rok s měsícem
     * @param unitdate        doplňovaný objekt
     * @return {Token} výsledný token
     */
    static parseYearMonth(yearMonthString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.YEAR_MONTH);

        const token = new Token();
        try {
            let date = new DT(yearMonthString, DT.FORMATS.YEAR_MONTH);
            token.dateFrom = date;
            let secDate = new DT(date);
            secDate.plusMonths(1);
            secDate.minusSeconds(1);
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
     * @return {Token} výsledný token
     */
    static parseDateTime(dateString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.DATE_TIME);

        const token = new Token();
        try {
            const date = new DT(dateString, DT.FORMATS.DATE_TIME);
            token.dateFrom = date;
            token.dateTo = date;
        } catch (e) {
            const date = new DT(dateString, DT.FORMATS.DATE_TIME2);
            token.dateFrom = date;
            let secDate = new DT(date);
            secDate.plusSeconds(59);
            token.dateTo = secDate;
        }

        return token;
    }

    /**
     * Parsování datumu.
     *
     * @param dateString datum
     * @param unitdate   doplňovaný objekt
     * @return {Token} výsledný token
     */
    static parseDate(dateString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.DATE);

        const token = new Token();
        try {
            let date = new DT(dateString, DT.FORMATS.DATE);
            token.dateFrom = date;
            let secDate = new DT(date);
            secDate.plusDays(1);
            secDate.minusSeconds(1);
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
     * @return {Token} výsledný token
     */
    static parseYear(yearString, unitdate) {
        unitdate.formatAppend(UnitDateConvertor.YEAR);
        const token = new Token();
        try {
            const year = parseInt(yearString);
            const yearDate = new DT();
            yearDate.setDateArray([year, 1, 1, 0, 0]);
            token.dateFrom = new DT(yearDate);
            yearDate.plusYears(1);
            yearDate.minusSeconds(1);
            token.dateTo = yearDate;
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
     * @return {Token} výsledný token
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
                throw new Exception('Cannot parse century');
            }
            token.dateFrom = new DT();
            token.dateFrom.setDateArray([(c - 1) * 100 + 1, 1, 1, 0, 0]);
            token.dateTo = new DT();
            token.dateTo.setDateArray([c * 100, 12, 31, 23, 59, 59]);
            token.estimate = true;

        } catch (e) {
            console.error(e);
            throw e;
        }
        return token;
    }

    /**
     * Testování, zda-li odpovídá řetězec formátu
     *
     * @param format formát
     * @param s         řetězec
     * @return {boolean} - lze parsovat
     */
    static tryParseDate(format, s) {
        try {
            new DT(s, format);
            return true;
        } catch (e) {
            return false;
        }
    }

    /**
     * Detekce, zda-li se jedná o interval
     *
     * @param input vstupní řetězec
     * @return {boolean} - jedná se o interval
     */
    static isInterval(input) {
        return input.indexOf(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER) !== -1 || input.indexOf(UnitDateConvertor.ESTIMATE_INTERVAL_DELIMITER) !== -1;
    }

}

/**
 * Pomocná třída pro reprezentaci jednoho výrazu.
 */
class Token {
    dateFrom = null;
    dateTo = null;
    estimate = false;
}

// window.q = DT;

// console.log('1', new DT('27.2.2015', DT.FORMATS.DATE))

export default connect(state => ({
    calendarTypes: state.refTables.calendarTypes
}))(DatationField)
