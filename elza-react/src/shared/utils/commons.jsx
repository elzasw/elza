/**
 * Převod datumu do řetězce - v budoucnu při více locale nahradit metodou pracující s locale.
 * @param date {Date} datum
 * @return {String} datum
 */
export function dateToDateString(date) {
    if (!date) {
        return null;
    }

    let dd = date.getDate().toString();
    let mm = (date.getMonth() + 1).toString();
    let yyyy = date.getFullYear().toString();
    return (dd[1] ? dd : '0' + dd[0]) + '.' + (mm[1] ? mm : '0' + mm[0]) + '.' + yyyy;
}

function _dtpad(number) {
    let r = String(number);
    if (r.length === 1) {
        r = '0' + r;
    }
    return r;
}

/**
 * Převede datum na lokální datum v UTC.
 * @param date
 * @return {*}
 */
export function dateToLocalUTC(date) {
    if (!date) {
        return date;
    }

    return date.getFullYear()
        + '-' + _dtpad(date.getMonth() + 1)
        + '-' + _dtpad(date.getDate())
        + 'T00'
        + ':00'
        + ':00'
        + '.000';
}

/**
 * Převede datum a čas na lokální datum a čas v UTC.
 * @param date
 * @return {*}
 */
export function dateTimeToLocalUTC(date) {
    if (!date) {
        return date;
    }

    return date.getFullYear()
        + '-' + _dtpad(date.getMonth() + 1)
        + '-' + _dtpad(date.getDate())
        + 'T' + _dtpad(date.getHours())
        + ':' + _dtpad(date.getMinutes())
        + ':' + _dtpad(date.getSeconds())
        + '.' + _dtpad(date.getMilliseconds());
}

// NECHAT - pro testování !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// var x = new Date();
// console.log("@@@", x)
//
// console.log("@@@ DATE")
// var l = dateToLocalUTC(x);
// console.log("dateToLocalUTC", l);
// console.log("localUTCToDate", localUTCToDate(l));
// console.log("dateToLocalUTC", dateToLocalUTC(localUTCToDate(l)));
//
// console.log("@@@ DATE TIME")
// var m = dateTimeToLocalUTC(x);
// console.log("dateTimeToLocalUTC", m);
// console.log("localUTCToDateTime", localUTCToDateTime(m));
// console.log("dateTimeToLocalUTC", dateTimeToLocalUTC(localUTCToDateTime(m)));

/**
 * Převede utc zápis braný jako local date do Date instance javascriptu BEZ času.
 * @param str
 */
export function localUTCToDate(str) {
    return localUTCToDateTime(str);
}

/**
 * Převede utc zápis braný jako local date time do Date instance javascriptu VČETNĚ času.
 * @param str
 */
export function localUTCToDateTime(str) {
    if (!str) {
        return null;
    }
    if (str instanceof Date) {
        return str;
    }

    const dt = str.split(/[: T-]/).map(parseFloat);
    const seconds = dt[5] ? Math.floor(dt[5]) : 0;
    const milliseconds = dt[5] ? ((dt[5] - seconds) * 1000) : 0;
    return new Date(dt[0], dt[1] - 1, dt[2], dt[3] || 0, dt[4] || 0, seconds, milliseconds);
}

/**
 * Převod Obejct:Date do řetězce včetně času
 * @param date {Date} datum a čas
 * @return {String} datum
 */
export function dateToDateTimeString(date) {
    let dd = date.getDate().toString();
    let mm = (date.getMonth() + 1).toString();
    let yyyy = date.getFullYear().toString();
    let hh = date.getHours().toString();
    let ii = date.getMinutes().toString();
    /** Formátování - místo 01 = 1 **/
    const f = (col) => (col[1] ? col : '0' + col[0]);
    return f(dd) + '.' + f(mm) + '.' + yyyy + ' ' + f(hh) + ':' + f(ii);
}

/**
 * Normalizace čísla - pouze povolené celé číslo
 * @param number string
 **/
export function normalizeInt(number) {
    let pole = ('' + number);
    let output = '';
    for (let i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '-') || pole[i] === '0' || parseInt(pole[i])) {
            output += pole[i];
        }
    }
    return output;
}

/**
 * Normalizace telefonního čísla - pouze povolené celé číslo a +
 * @param number string
 **/
export function normalizePhone(number) {
    if (!number) {
        return number;
    }
    let pole = ('' + number);
    let output = '';
    for (let i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '+') || pole[i] === '0' || parseInt(pole[i])) {
            output += pole[i];
        }
    }
    return output;
}


/**
 * Normalizace textu na delku výchozí 255 znaků
 * @param text String
 * @param allowedLength int
 **/
export function normalizeString(text, allowedLength = 255) {
    if (text.length > allowedLength) {
        text = text.substr(0, allowedLength);
    }
    return text;
}

/**
 * Provede normalizaci hodnoty tak, aby byla čitelná pro uživatele, tedy naformátuje v jeho locales.
 * @param value hodnota
 * @return {*} hodnota pro uživatele
 */
// TODO [stanekpa] - vyřešit locales!!!
export function normalizeDoubleForUser(value) {
    if (value) {
        if (typeof value === 'string') {
            return value.replace(/\./g, ',');
        } else {
            return '' + value;
        }
    } else {
        return value;
    }
}

/**
 * Normalizace čísla - lze pouze číslice a nahrazení ',' za '.' kdy náhodou - (přes string rychlejší než ukládat do pole)
 * @param number decimal(18,6)
 **/
export function normalizeDouble(number) {
    const pole = '' + number;
    let output = '';
    let existComa = false;
    for (let i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '-') || parseInt(pole[i]) || pole[i] === '0' || ((pole[i] === ',' || pole[i] === '.') && !existComa && (existComa = true))) {
            output += pole[i] === ',' ? '.' : pole[i];
        }
    }
    return output;
}
