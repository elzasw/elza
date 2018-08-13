import React from 'react';
import {propsEquals} from 'components/Utils.jsx'

/**
 * Abstraktní předek pro všechny komponenty.
 */
class AbstractReactComponent extends React.Component {
    constructor(props) {
        super(props);

        if (props && props.dispatch) {
            this.dispatch = props.dispatch;
        }
    }

    componentWillUpdate() {
    //console.log(this);
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        return !propsEquals(this.props, nextProps);
    }

    /**
     * Bind metod pro předné názvy metod v parametru.
     * @deprecated nově používáme syntaxi () => {}
     */
    bindMethods(...methods) {
        methods.forEach( (method) => {
            if (!this[method]) {
                console.warn("Cannot bind method " + method + ".");
            } else {
                this[method] = this[method].bind(this)
            }
        });
    }
}

export default AbstractReactComponent;
