/**
 * Komponenta pro načítání textu na základě klíče z resource textů včetně formátování.
 * Komponenta využívá globální proměnnou messages, která musí být definovaná a musí již představovat aktuálně zvolený jazyk.
 * Pokud daný klíč v messages neexistuje, je vrácen řetězec '[klíč]'.
 * Pokud je vstupem klíč začínající znakem '^', je v případě neexistence klíče vráceno null.
 *
 * Příklad definice messages:
 *     var messages = {
 *         "a.b.c": "Osoba {0}"
 *     };
 *
 * Příklad formátu:
 *     i18n('a.b.c', person.displayName);
 *
 *  Rozšířený formát:
 *      - pokud je jako první parametr vložen objekt, je možné používat v textu i textové parametry, které odpovídají parametrům objektu
 *      - pokud parametr v objektu je pole, je sestaven řetězec z hodnot oddělovaný čárkou
 *
 *      Např:
 *
 *      var messages = {
 *         "a.b.c": "Jméno: {name}, věk: {age}, tituly: {educations}"
 *     };
 *
 *      i18n('a.b.c', {
 *                      name: 'Martin',
 *                      age: 27,
 *                      educations: ['Ing.', 'Ph.D.']
 *                      });
 *
 *      Výsledek: Jméno: Martin, věk: 27, tituly: Ing., Ph.D.
 *
 **/

String.format = function(format) {
    const args = Array.prototype.slice.call(arguments, 1);
    if (args && args.length === 1 && typeof args[0] === 'object') {
        const data = args[0];

        for (const param in data) {
            if (data.hasOwnProperty(param)) {
                format = format.replace(/{([a-zA-Z-.]+)}/g, function(match, index) {
                    if (match === '{' + param + '}') {
                        if (Array.isArray(data[param])) {
                            return data[param].join(', ');
                        } else {
                            return data[param];
                        }
                    } else {
                        return match;
                    }
                });
            }
        }

        return format;
    } else {
        // starý formát - kvůli zpětné kompatibilitě
        return format.replace(/{(\d+)}/g, function(match, number) {
            return typeof args[number] != 'undefined' ? args[number] : match;
        });
    }
};

if (!window.messages) {
    window.messages = window.__DEV__ ? window.devMessages : {};
}

export default function(key, ...params) {
    if (key) {
        if (key[0] === '^') {
            key = key.substring(1, key.length);
            const text = window.messages[key];
            return text != null ? String.format(text, ...params) : null;
        } else {
            const text = window.messages[key];
            return text != null ? String.format(text, ...params) : '[' + key + ']';
        }
    } else {
        return '<null>';
    }
}
