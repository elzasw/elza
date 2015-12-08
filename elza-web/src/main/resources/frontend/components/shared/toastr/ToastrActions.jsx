/**
 *
 * Vytvoření akcí pro toast notifikace.
 * @param object {String title, String message}
 *
**/
var Reflux = require('reflux');

var ToastrActions = Reflux.createActions({
    "danger" : {},
    "info" : {},
    "success" : {},
    "warning" : {},
    "clear" : {},
});

module.exports = ToastrActions;