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
 * @param cislo int
**/
var MIN_INTEGER = -Math.pow(2,31);
var MAX_INTEGER = Math.pow(2,31)-1;

var validateInt = function (cislo)
{
    if (cislo < MIN_INTEGER || cislo > MAX_INTEGER)
        return 'Zadané číslo je mimo rozsah celého čísla typu INT';
    return null;
}

/**
 * Validace zda je číslo desetinným číslem kladným i záporným v intervalu desetinného čísla JAVA
 * @param desetineCislo decimal(18,6)
**/
var validateDouble = function (desetineCislo)
{
    if (desetineCislo.replace(",","").length > 18 ||  desetineCislo.substr(desetineCislo.indexOf(",")+1).length > 6)
        return 'Zadané číslo je mimo rozsah desetinného čísla typu Decimal(18,6)';
    return null;
}


/**
 * Validace zda je datace ve správném formátu
 * @param datace
 * volání funkce: validateDate('19.st.', function (message){console.log(message); });
**/
var validateDate = function (datace, callback = function (){}) {
    AjaxUtils.ajaxGet('/api/validate/unitDate', {value: datace})
            .then(json=>{
                var message = null;
                if (json['valid'] == false)
                    message = json['message'];
                callback(message);
            });
}

/**
 * Normalizace čísla - pouze povolené celé číslo
 * @param cislo int
 **/
export function normalizeInt(cislo) {
    var pole = cislo.split('');
    var output = [];
    for( var i = 0, len = pole.length; i < len; i++ ) {
        if( parseInt(pole[i])) {
            output.push(pole[i]);
        }
    }
    return output.join('');
}

/**
 * Normalizace čísla - lze pouze číslice a nahrazení '.' za ',' kdy náhodou
 * @param cislo decimal(18,6)
 **/
export function normalizeDouble(cislo) {
    var pole = cislo.split('');
    var output = [];
    for( var i = 0, len = pole.length; i < len; i++ ) {
        if (pole[i] == '.') {
            pole[i] = ',';
        }
        
        if( parseInt(pole[i]) || (pole[i] == ',' && output.join('').indexOf(',') === -1)) {
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

