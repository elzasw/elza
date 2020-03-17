/* global __SHOW_DEVTOOLS__ */
import React from 'react';
import {createDevTools} from 'redux-devtools';
import DockMonitor from 'redux-devtools-dock-monitor';
import FilterableLogMonitor from 'redux-devtools-filterable-log-monitor';
import FilterMonitor from 'redux-devtools-filter-actions';
import Dispatcher from 'redux-devtools-dispatch';

export default createDevTools(
    <DockMonitor
        toggleVisibilityKey="ctrl-x"
        changePositionKey="ctrl-q"
        defaultIsVisible={false}
        changeMonitorKey="ctrl-e"
    >
        <FilterMonitor blacklist={['STORE_STATE_DATA']}>
            <FilterableLogMonitor />
        </FilterMonitor>
        <Dispatcher />
    </DockMonitor>,
);
