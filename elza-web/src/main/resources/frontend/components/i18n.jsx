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
  **/

String.format = function(format) {
  var args = Array.prototype.slice.call(arguments, 1);
  return format.replace(/{(\d+)}/g, function(match, number) { 
    return typeof args[number] != 'undefined'
      ? args[number] 
      : match
    ;
  });
};

var GetText = function(key, ...params) {
    if (key) {
        if (key[0] == '^') {
            key = key.substring(1, key.length);
            var text = messages[key];
            return text != null ? String.format(text, ...params) : null;
        } else {
            var text = messages[key];
            return text != null ? String.format(text, ...params) : "[" + key + "]";
        }
    } else {
        return "<null>";
    }
}

module.exports = GetText