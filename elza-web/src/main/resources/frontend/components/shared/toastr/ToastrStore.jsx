/**
 * Akce, které zpracují vstup toastu a vrátí zobrazení toast notifikace
 * a na jejich základě se vypíše daný typ toast notifikace
 **/
var Reflux = require('reflux');

var ToastrActions = require('./ToastrActions');

var ToastrStore = Reflux.createStore({
    listenables: [ToastrActions],
    id: 1,
    lastKey: 1,
    toasters : [],
    getInitialData: function() {
        return this.toasters;
    },
    onClear: function(id) {
        var fi = null;
        this.removeOldToasts();
        this.toasters.forEach((t, i) => {
            if (t.id === id) {
                t.visible = false;
            }
        });

        this.trigger(this.toasters);
    },
    removeOldToasts: function(id){
        this.toasters.forEach((t, i) => {
            if (t.visible === false) {
                this.toasters.splice(i, 1);
            }
        });
    },

    getLastKey: function(){
        this.lastKey++
        return this.lastKey;
    },
    onDanger: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'danger',
            dismissAfter: null,
            key: this.getLastKey()
        };
        this.add(toastr);
    },
    onSuccess: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'success',
            dismissAfter: 2000,
            key: this.getLastKey()
        };
        this.add(toastr);
    },
    onWarning: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'warning',
            dismissAfter: null,
            key: this.getLastKey()
        };
        this.add(toastr);
    },
    onInfo: function(data) {
        var toastr = {
            id: this.id++,
            title: data.title,
            message: data.message,
            type: 'info',
            dismissAfter: 2000,
            key: this.getLastKey()
        };
        this.add(toastr);
    },
    add: function(toastr) {
        this.removeOldToasts();
        this.toasters.push(toastr);
        this.trigger(this.toasters);
        setTimeout(function(toastr) {
            toastr.visible = true;
            this.trigger(this.toasters);
        }.bind(this), 1, toastr);
    },
    
});

module.exports = ToastrStore;
