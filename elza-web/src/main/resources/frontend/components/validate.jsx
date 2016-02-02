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
**/

/**
 * Ajax je pro validaci datace
**/
import {AjaxUtils} from 'components';

/**
 * Validace zda je číslo celým číslem kladným i záporným  v intervalu clého čísla JAVA
 * @param number int
**/
var MIN_INTEGER = -Math.pow(2,31);
var MAX_INTEGER = Math.pow(2,31)-1;

var validateInt = function (number) {
    if ((number % 1) !== 0)
        return 'Nejedná se o celé číslo';
    if (number < MIN_INTEGER || number > MAX_INTEGER)
        return 'Zadané číslo je mimo rozsah celého čísla typu INT';
    return null;
}

/**
 * Validace zda je číslo desetinným číslem kladným i záporným v intervalu desetinného čísla JAVA
 * @param doubleNumber decimal(18,6)
**/
var validateDouble = function (doubleNumber) {
    if ((''+doubleNumber).replace(",","").length > 18 ||  ((''+doubleNumber).indexOf(",") !==-1 && (''+doubleNumber).substr((''+doubleNumber).indexOf(",")+1).length > 6))
        return 'Zadané číslo je mimo rozsah desetinného čísla typu Decimal(18,6)';
    return null;
}


/**
 * Validace zda je datace ve správném formátu
 * @param datace
 * volání funkce: validateDate('19.st.', function (message){console.log(message); });
**/
var validateDate = function (dateNumber, callback = function (){}) {
    AjaxUtils.ajaxGet('/api/validate/unitDate', {value: dateNumber})
            .then(json=>{
                var message = null;
                if (json['valid'] == false)
                    message = json['message'];
                callback(message);
            });
}

/**
 * Normalizace čísla - pouze povolené celé číslo
 * @param number int
 **/
export function normalizeInt(number) {
    var pole = (''+number).split('');
    var output = [];
    for( var i = 0, len = pole.length; i < len; i++ ) {
        if( (i===0 && pole[i] === '-') || pole[i] === "0" ||  parseInt(pole[i])) {
            output.push(pole[i]);
        }
    }
    return output.join('');
}

/**
 * Normalizace čísla - lze pouze číslice a nahrazení '.' za ',' kdy náhodou
 * @param number decimal(18,6)
 **/
export function normalizeDouble(number) {
    var pole = (''+number).split('');
    var output = [];
    for( var i = 0, len = pole.length; i < len; i++ ) {
        if (pole[i] == '.') {
            pole[i] = ',';
        }
        
        if( (i===0 && pole[i] === '-') || parseInt(pole[i]) || pole[i]==='0' || (pole[i] == ',' && output.join('').indexOf(',') === -1)) {
            output.push(pole[i]);
        }
    }
    return output.join('');
}

/**
 * Normalizace textu na delku výchozí 255 znaků
 * @param text String
 * @param delka int
 **/
export function normalizeString(text, delka = 255) {
    if (text.length>delka) {
        text = text.substr(0,delka);
    }
    return text;
}


/**
 * test validačních a noramlizačních funkcí
**/
function testValidace(){

    var testCislo = 12;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = 12.4;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = -12;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = -12.4;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = -121212312312;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = 121212312312;
    console.log('validateInt('+testCislo+')', validateInt(testCislo));
    var testCislo = '0';
    console.log('validateInt('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-0';
    console.log('validateInt('+testCislo+')', validateDouble(testCislo));


    var testCislo = '12';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '12,5';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-12';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-1,42';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '101231234567894561';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '1012312345678945611';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-10123123467894571';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-101231234678945711';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-10123123467,894571';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-1012312346,7894571';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '0,3';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));
    var testCislo = '-0,3';
    console.log('validateDouble('+testCislo+')', validateDouble(testCislo));


    var testSt = '19.st';
    validateDate(testSt, function (e){console.log('validateDate('+testSt+')', e)});
    var testStTecka = '19.st.';
    validateDate(testStTecka, function (e){console.log('validateDate('+testStTecka+')', e)});
    var testRok = 1958;
    validateDate(testRok, function (e){console.log('validateDate('+testRok+')', e)});
    var testRokMesic = '1958/5';
    validateDate(testRokMesic, function (e){console.log('validateDate('+testRokMesic+')', e)});
    var testDatum = '25.3.1958';
    validateDate(testDatum, function (e){console.log('validateDate('+testDatum+')', e)});
    var testDatumCas = '25.3.1958 18:23';
    validateDate(testDatumCas, function (e){console.log('validateDate('+testDatumCas+')', e)});
    var testDatumCasMezery = '25. 3. 1958 18:23';
    validateDate(testDatumCasMezery, function (e){console.log('validateDate('+testDatumCasMezery+')', e)});
    var testDatumSlovo = '13. červen 1953';
    validateDate(testDatumSlovo, function (e){console.log('validateDate('+testDatumSlovo+')', e)});


    var testCislo = 12;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = 12.4;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = -12;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = -12.4;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = '12,4';
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = '-12,4';
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = -121212312312;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = 121212312312;
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = '12,4a5';
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));
    var testCislo = '12.4a5';
    console.log('normalizeInt('+testCislo+')', normalizeInt(testCislo));


    var testCislo = 12;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = 12.4;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = -12;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = -12.4;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '12,4';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '-12,4';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = -121212312312;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = 121212312312;
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '12,4a5';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '12.4a5';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '-12,4a5';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '-12.4a5';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '0,3';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));
    var testCislo = '-0,3';
    console.log('normalizeDouble('+testCislo+')', normalizeDouble(testCislo));


    var testCislo = 'fij iofwhe urh wuioeh wofhsdjk nsdjkvnifgnweuif nwuif nweuif';
    console.log('normalizeString('+testCislo+')', normalizeString(testCislo));
    var testCislo = 'fij iofwhe urh wuioeh wofhsdjk nsdjkvnifgnweuif nwuif nweuifgh fherjklfh wejklf jklv bnsdjklfn sjkfhnwekjfwefh weuirfh weuif sjkn sdjkfnjk hrjkl hsdjklfbh sdjkfb weflh bweuilfh eklvnsdjkln bweuifh weuifbh eruigberhilgb fbsdb weuiwerilb fbslkf wehfweih weoů fhweůh fweůohfůqwogl hfelwfe';
    console.log('normalizeString('+testCislo+')', normalizeString(testCislo));
}
 //testValidace();