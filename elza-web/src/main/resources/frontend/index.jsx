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
import { Utils } from 'components'

// Globální init
Utils.init();
//var es5Shim = require('es5-shim');
import {SplitPane} from 'components';

// Aplikace
var Router = require('./router');
Router.start();
