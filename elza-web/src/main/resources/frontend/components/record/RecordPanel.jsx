import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components';
import {i18n} from 'components';


var RecordPanel = class RecordPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div>
                testovaci data
            </div>
        )
    }
}

module.exports = RecordPanel;


