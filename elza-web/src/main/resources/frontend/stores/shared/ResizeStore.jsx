import airflux from 'airflux';

import React from 'react';

require ('components/shared/split-pane/split-pane.js');
require ('components/shared/split-pane/split-pane.css');

/**
 * Store informující o změně velikosti komponent - změna okna prohlížeče nebo změna v závislosti na splitteru.
 */
var store = class ResizeStore extends airflux.Store {

    constructor() {
        super();

        this.handleResize.bind(this);
    }

    handleResize() {
        this.trigger({});
    }
}

var s = new store();

var fn = function() {
    s.handleResize();
};
window.addEventListener('resize', fn);
$.fn.splitPane("addResizeListener", fn);

module.exports = s;

