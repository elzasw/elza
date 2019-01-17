/**
 * @Author Jiří Štěpina
 *
 * Validační a normalizační funkce
 * Pro input je zapotřebí připravit handler. Toto je pouze návrh, jak by mohl vypadat. Služilo to jen pro testování
 *  handleKeyUp(e){
 *      if ((!e.ctrlKey || (e.ctrlKey && e.keyCode === 86)) && $.inArray(e.keyCode,[8,16,17,46,37,39])===-1)
 *          e.target.value = normalizeString(e.target.value);
 *  }
 * samozřejmně se nesmí zapomenout na napojení inputu na handler
 *
 * ---
 *
 * @author Petr Compel
 * @since 17.2.2016
 **/

/**
 * Ajax je pro validaci datace, i18n překlad
 **/
import i18n from "./i18n";
//import AjaxUtils from "./AjaxUtils";

/**
 * Validace zda je číslo celým číslem kladným i záporným  v intervalu clého čísla JAVA
 * @param number int
 **/
var MIN_INTEGER = -Math.pow(2, 31);
var MAX_INTEGER = Math.pow(2, 31) - 1;

export function validateInt(number) {
    if ((number % 1) !== 0)
        return i18n('validate.validateInt.notInt');
    if (number < MIN_INTEGER || number > MAX_INTEGER)
        return i18n('validate.validateInt.outOfRange');
    return null;
};

export function validateDuration(duration) {
    const number = fromDuration(duration);
    if (number < 0 || number > MAX_INTEGER)
        return i18n('validate.validateDuration.outOfRange');
    return null;
};

/**
 * Validace souřadnice typu bod.
 * @param value
 */
export function validateCoordinatePoint(value) {
    if (value.indexOf("POINT") === 0) {
        let left = value.indexOf('(') + 1;
        let right = value.indexOf(')');
        if ((right - left) == 0) {
            return i18n('subNodeForm.validate.value.notEmpty');
        }
        let data = value.substr(left, value.indexOf(')') - left).split(' ');
        if (value === '' || value === ' ' || data.length != 2 || data[0] == null || data[0] == '' || data[1] == null || data[1] == '') {
            return i18n("subNodeForm.errorPointCoordinates");
        } else {
            return null;
        }
    }
    return null;
};

/**
 * Validace zda je číslo desetinným číslem kladným i záporným v intervalu desetinného čísla JAVA
 * @param doubleNumber decimal(18,6)
 **/
export function validateDouble(doubleNumber) {
    const stringNumber = '' + doubleNumber;
    if (stringNumber.replace(",", "").length > 18 || (stringNumber.indexOf(",") !== -1 && stringNumber.substr(stringNumber.indexOf(",") + 1).length > 6))
        return i18n('validate.validateDouble.outOfRange');
    return null;
};

/**
 * Validace zda je datace ve správném formátu
 * @param dateNumber datace
 * @param callback funkce
 * volání funkce: validateDate('19.st.', function (message){console.log(message); });
 * @deprecated Nepoužívá se - pokud se bude znovu používat nutno vyextrahovat do extra souboru
 **/
/*export function validateDate(dateNumber, callback = function () {
}) {
    AjaxUtils.ajaxGet('/api/validate/unitDate', {value: dateNumber})
        .then(json=> {
            var message = null;
            if (json['valid'] == false)
                message = json['message'];
            callback(message);
        });
};*/

/**
 * Normalizace čísla - pouze povolené celé číslo - (přes string rychlejší než ukládat do pole)
 * @param number int
 **/
export function normalizeInt(number) {
    var pole = ('' + number);
    var output = '';
    for (var i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '-') || pole[i] === "0" || parseInt(pole[i])) {
            output += pole[i];
        }
    }
    return output;
}

/**
 * Normalizace délky trvání.
 * @param duration
 * @returns {string}
 */
export function normalizeDuration(duration) {
    const pole = ('' + duration);
    let allow = '';
    let k = 0;
    for (let i = 0, len = pole.length; i < len; i++) {
        if (parseInt(pole[i]) || pole[i] === "0" || pole[i] === ':') {
            if (pole[i] === ':') {
                k++;
            }
            if (k < 3) {
                allow += pole[i];
            }
        }
    }
    return allow;
}

/**
 * Převedení délky trvání na číslo v sekundách.
 * @param duration
 * @returns {string}
 */
export function fromDuration(duration) {
    const pole = ('' + duration);
    let allow = '';
    for (let i = 0, len = pole.length; i < len; i++) {
        if (parseInt(pole[i]) || pole[i] === "0" || pole[i] === ':') {
            allow += pole[i];
        }
    }

    const data = allow.split(':');
    let result = 0;

    for (let i = 0; i < data.length; i++) {
        result += data[data.length - (i + 1)] * Math.pow(60, i);
    }

    return '' + result;
}

/**
 * Převedení počtu sekund na délku trvání,
 * @param number
 * @returns {string}
 */
export function toDuration(number) {
    number = Number(number);
    let h = Math.floor(number / 3600);
    let m = Math.floor(number % 3600 / 60);
    let s = Math.floor(number % 3600 % 60);
    return pad2(h) + ":" + pad2(m) + ":" + pad2(s);
}

/**
 * Normalizace délky řetězce délky trvání.
 *
 * @param duration délka trvání
 * @returns {string} normalizovaná délka trvání
 */
export function normalizeDurationLength(duration) {
    return toDuration(fromDuration(duration));
}

/**
 * Je délka řetězce délky trvání normalizovaná?
 *
 * @param duration délka trvání
 * @returns {boolean} true, pokud je validní
 */
export function isNormalizeDurationLength(duration) {
    return /^(([0-9]{2})|([1-9][0-9]{2,}):[0-9]{2}:[0-9]{2})$/.test(duration);
}

/**
 * Zarovnání na 2 cifry.
 * @param number
 * @returns {string}
 */
function pad2(number) {
    return (number < 10 ? '0' : '') + number
}

/**
 * Přeformátování datumu do YYYY-MM-DD řetězce.
 *
 * @param date
 * @returns {string}
 */
export function formatDate(date) {
    let month = date.getMonth() + 1,
        day = date.getDate(),
        year = date.getFullYear();
    return [year, pad2(month), pad2(day)].join('-');
}

/**
 * Normalizace čísla - lze pouze číslice a nahrazení '.' za ',' kdy náhodou - (přes string rychlejší než ukládat do pole)
 * @param number decimal(18,6)
 **/
export function normalizeDouble(number) {
    var pole = '' + number;
    var output = '';
    var existComa = false;
    for (var i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '-') || parseInt(pole[i]) || pole[i] === '0' || ((pole[i] == ',' || pole[i] == '.') && !existComa && (existComa = true))) {
            output += pole[i] == '.' ? ',' : pole[i];
        }
    }
    return output;
}


/**
 * Částečná normalizace - vrací čísla s '.' nikoliv ','
 * Normalizace čísla - lze pouze číslice - (přes string rychlejší než ukládat do pole)
 * @param number decimal(18,6)
 **/
export function normalizeDoubleWithDot(number) {
    var pole = '' + number;
    var output = '';
    var existComa = false;
    for (var i = 0, len = pole.length; i < len; i++) {
        if ((i === 0 && pole[i] === '-') || parseInt(pole[i]) || pole[i] === '0' || ((pole[i] == ',' || pole[i] == '.') && !existComa && (existComa = true))) {
            output += pole[i]
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
 * test validačních a normalizačních funkcí
 **/
function testValidations() {

    const FORMAT_EACH = 5;
    const FORMAT_GROUP = 3;
    const FORMAT_LINE = 1;
    const FORMAT_RETURN = -1;

    /** Běžný test pro funkčnost */
    var basicTest = (testValues, test, format = FORMAT_EACH, exceptedResults = null) => {
        let testFunction = (item, index) => {
            let start = performance.now();
            let result = test(item);
            let end = performance.now();
            let ret = [item, typeof item, result, end - start];
            if (format === FORMAT_EACH) {
                if (exceptedResults !== null) {
                    return result !== exceptedResults[index] ? console.error(ret) : console.log(ret);
                }
                return console.log(ret);
            } else {
                return ret;
            }
        };
        let testing = testValues.map(testFunction);
        if (format === FORMAT_EACH) {
            console.debug(' @@@ TEST RUN @@@ - ' + test.name);
        } else if (format === FORMAT_GROUP) {
            console.debug(test.name);
            console.debug(testing);
        } else if (format === FORMAT_LINE) {
            console.debug(test.name, testing);
        } else {
            return testing;
        }
    };

    /** Rychlostní test */
    var performanceTest = (testValues, test, oldTest = null, times, format = FORMAT_RETURN) => {
        let testFunction = (item) => {
            var i;
            if (!oldTest) {
                let start = performance.now();
                for (i = 0; i < times; i++) test(item);
                let end = performance.now();

                let ret = [String(item).substr(0, 20), end - start];
                if (format === FORMAT_EACH) {
                    return console.log(ret);
                } else {
                    return ret;
                }
            } else {
                let oldStart = performance.now();
                for (i = 0; i < times; i++) oldTest(item);
                let oldEnd = performance.now();

                let start = performance.now();
                for (i = 0; i < times; i++) test(item);
                let end = performance.now();

                let ret = [String(item).substr(0, 20), end - start, oldEnd - oldStart];
                if (format === FORMAT_EACH) {
                    return console.log(ret);
                } else {
                    return ret;
                }
            }
        };
        let testing = testValues.map(testFunction);
        if (format === FORMAT_EACH) {
            console.debug(' @@@ TEST RUN @@@ - ' + test.name);
        } else if (format === FORMAT_GROUP) {
            console.debug(test.name);
            console.debug(testing);
        } else if (format === FORMAT_LINE) {
            console.debug(test.name, testing);
        } else {
            return testing;
        }
    };

    /** Porovnávací test */
    var compareTest = (testValues, test, oldTest, times = 1000) => {
        var results = [];
        for (var l = testValues.length; l > 0; l--) {
            results.push([0, 0]);
        }
        performanceTest(testValues, test, oldTest, times).map((item, index) => {
            results[index][0] += item[1];
            results[index][1] += item[2];
        });

        results.map((item, index) => {
            console.debug(testValues[index], 'new: ' + item[0] / times, 'old:' + item[1] / times, 'compare:' + (item[0] / times - item[1] / times));
        })
    };

    /** Testovací data */
    const intTestValidationValues = [
        12,
        12.4,
        -12,
        -12.4,
        -121212312312,
        121212312312,
        -12121231230000000000012,
        121212312310000000000002,
        {},
        '0',
        '-0'
    ];
    const intTestNormalizationValues = [
        12,
        12.4,
        -12,
        -12.4,
        '12,4',
        '-12,4',
        -121212312312,
        121212312312,
        '12,4a5',
        '12.4a5'
    ]
    const doubleTestValidationValues = [
        '12',
        '12,5',
        '-12',
        '-1,42',
        '101231234567894561',
        '1012312345678945611',
        '-101231234567894561',
        '-1012312345678945611',
        '-10123123467,894571',
        '0,3',
        '-0,3'
    ];
    const doubleTestNormalizationValues = [
        12,
        12.4,
        -12,
        -12.4,
        '12,4',
        '-12,4',
        -121212312312,
        121212312312,
        '12,4a5',
        '12.4a5',
        '-12,4a5',
        '-12.4a5',
        '0,3',
        '-0,3'
    ];
    const dateTestValues = [
        '19.st',
        '19.st.',
        1958,
        '1958/5',
        '25.3.1958',
        '25.3.1958 18:23',
        '25. 3. 1958 18:23',
        '13. červen 1953'
    ];
    const stringTestValues = [
        'fij iofwhe urh wuioeh wofhsdjk nsdjkvnifgnweuif nwuif nweuif',
        'fij iofwhe urh wuioeh wofhsdjk nsdjkvnifgnweuif nwuif nweuifgh fherjklfh wejklf jklv bnsdjklfn sjkfhnwekjfwefh weuirfh weuif sjkn sdjkfnjk hrjkl hsdjklfbh sdjkfb weflh bweuilfh eklvnsdjkln bweuifh weuifbh eruigberhilgb fbsdb weuiwerilb fbslkf wehfweih weoů fhweůh fweůohfůqwogl hfelwfe'
    ];

    /** Testování funkčnosti */
    basicTest(intTestValidationValues, validateInt, FORMAT_EACH, [
        null,
        "Nejedná se o celé číslo",
        null,
        "Nejedná se o celé číslo",
        "Zadané číslo je mimo rozsah celého čísla typu INT",
        "Zadané číslo je mimo rozsah celého čísla typu INT",
        "Zadané číslo je mimo rozsah celého čísla typu INT",
        "Zadané číslo je mimo rozsah celého čísla typu INT",
        "Nejedná se o celé číslo",
        null,
        null
    ]);
    basicTest(doubleTestValidationValues, validateDouble);
    basicTest(dateTestValues, validateDate);
    basicTest(intTestNormalizationValues, normalizeInt);
    basicTest(doubleTestNormalizationValues, normalizeDouble);
    basicTest(stringTestValues, normalizeString);
}

//testValidations();
