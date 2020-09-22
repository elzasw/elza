import React, {Component} from 'react';
import {propsEquals} from './Utils.jsx';

/**
 * Abstraktní předek pro všechny komponenty.
 *
 */
class AbstractReactComponent<P = {}, S = {}, SS = any> extends React.Component<P, S, SS> {
    UNSAFE_componentWillUpdate() {
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
        methods.forEach(method => {
            if (!this[method]) {
                console.warn('Cannot bind method ' + method + '.');
            } else {
                this[method] = this[method].bind(this);
            }
        });
    }
}

export default AbstractReactComponent;
