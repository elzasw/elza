import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';
import {Glyphicon} from 'react-bootstrap';
import {propsEquals} from 'components/Utils.jsx'

var Icon = class Icon extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['className', 'glyph', 'onClick']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    render() {
        var cls = 'icon fa ' + this.props.glyph;
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        var props = {};

        if (this.props.onClick != null) {
            props.onClick = this.props.onClick;
        }

        return (
            <span {...props} className={cls}/>
        )
    }
}

module.exports = Icon;

