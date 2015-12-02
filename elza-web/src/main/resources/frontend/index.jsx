/**
 * Vstupní soubor pro UI - inicializace a zobrazení VIEW.
 */

'use strict'

// Import css Bootstrapu
require ('bootstrap/less/bootstrap.less');

import React from 'react';
import ReactDOM from 'react-dom';

import { createHistory, useBasename } from 'history'
import { Route, Link, History, Lifecycle } from 'react-router'

// Globální init
//Utils.init();
//var es5Shim = require('es5-shim');


// Aplikace
var Router = require('./router');
Router.start();
