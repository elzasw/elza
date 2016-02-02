/**
 * Vstupní soubor pro UI - inicializace a zobrazení VIEW.
 */

'use strict'

// Import css Bootstrapu
require ('./elza-styles.less');

import React from 'react';
import ReactDOM from 'react-dom';

import { createHistory, useBasename } from 'history'
import { Route, Link, History, Lifecycle } from 'react-router'
import { Utils } from 'components'

// Globální init
Utils.init();
var es6promise = require('es6-promise');
//var es5Shim = require('es5-shim');

// Web socket
var websocket = require('./websocket');

// Aplikace
var Router = require('./router');
Router.start();
