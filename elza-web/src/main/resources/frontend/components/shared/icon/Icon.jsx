import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';
import {Glyphicon} from 'react-bootstrap';
import {propsEquals} from 'components/Utils.jsx'

class Icon extends AbstractReactComponent {

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['className', 'glyph', 'onClick']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    render() {
        const {glyph, ...otherProps} = this.props;
        if(glyph.indexOf("ez-")==0){
            var cls = 'icon ez ' + glyph;
        }
        else if(glyph.indexOf("fa-")==0){
            var cls = 'icon fa ' + glyph;
        }
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        var props = {};

        if (this.props.onClick != null) {
            props.onClick = this.props.onClick;
        }

        return (
            <span {...props} className={cls} {...otherProps} />
        )
    }
}

export default Icon;
