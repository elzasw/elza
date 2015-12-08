/**
 * Akce, které zpracují vstup toastu a vrátí zobrazení toast notifikace
 * a na jejich základě se vypíše daný typ toast notifikace
 **/
var Reflux = require('reflux');

var ToastrActions = require('./ToastrActions');

var ToastrStore = Reflux.createStore({
    listenables: [ToastrActions],
    id: 1,
    toasters : [],
    getInitialData: function() {
        return this.toasters;
    },
    onClear: function(id) {
        var fi = null;
        this.toasters.forEach((t, i) => {
            if (t.id === id) {
                t.visible = false;
                fi = i;
            }
        });

        if (fi != null) {
           this.toasters.splice(fi, 1);
        }

       this.trigger(this.toasters);
    },
    onDanger: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'danger',
            dismissAfter: null
        };
        this.add(toastr);
    },
    onSuccess: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'success',
            dismissAfter: 2000
        };
        this.add(toastr);
    },
    onWarning: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'warning',
            dismissAfter: null
        };
        this.add(toastr);
    },
    onInfo: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'info',
            dismissAfter: 2000
        };
        this.add(toastr);
    },
    add: function(toastr) {

        this.toasters.push(toastr);
        this.trigger(this.toasters);

        setTimeout(function(toastr) {
            toastr.visible = true;
            this.trigger(this.toasters);
        }.bind(this), 1, toastr);
    },
    
});

module.exports = ToastrStore;
