import React from 'react';
import {AbstractReactComponent} from 'components';
import {Glyphicon} from 'react-bootstrap';

var Icon = class Icon extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        //<Glyphicon className='icon' glyph={this.props.glyph} />
        return (
            <span className={'icon fa ' + this.props.glyph}/>
        )
    }
}

module.exports = Icon;

