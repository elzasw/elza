import React from 'react';
import {AbstractReactComponent} from 'components';
import {Glyphicon} from 'react-bootstrap';
import {propsEquals} from 'components/Utils'

var Icon = class Icon extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(nextProps, nextState) {
        var eqProps = ['className', 'glyph']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    render() {
        var cls = 'icon fa ' + this.props.glyph;
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        //<Glyphicon className='icon' glyph={this.props.glyph} />
        return (
            <span className={cls}/>
        )
    }
}

module.exports = Icon;

