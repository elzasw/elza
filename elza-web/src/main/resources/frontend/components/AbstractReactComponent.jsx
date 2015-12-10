import React from 'react';

var AbstractReactComponent = class AbstractReactComponent extends React.Component {
    constructor(props) {
        super(props);

        if (props && props.dispatch) {
            this.dispatch = props.dispatch;
        }
    }

    bindMethods(...methods) {
        methods.forEach( (method) => {
            if (!this[method]) {
                console.error("Cannot bind method " + method + ".");
            } else {
                this[method] = this[method].bind(this)
            }
        });
    }
}

module.exports = AbstractReactComponent;