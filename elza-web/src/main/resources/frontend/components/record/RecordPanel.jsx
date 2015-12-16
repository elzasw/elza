import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent} from 'components';
import {i18n} from 'components';


var RecordPanel = class RecordPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);
console.log('test');
console.log(props);
    }

    render() {
console.log('tady');
console.log(this.props.selectedId);
        return (
            <div>
                testovaci data {this.props.selectedId}
            </div>
        )
    }
}

module.exports = RecordPanel;


