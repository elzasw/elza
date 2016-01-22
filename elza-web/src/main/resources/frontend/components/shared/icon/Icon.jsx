import React from 'react';
import {AbstractReactComponent} from 'components';
import {Glyphicon} from 'react-bootstrap';

var Icon = class Icon extends AbstractReactComponent {
    constructor(props) {
        super(props);
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

