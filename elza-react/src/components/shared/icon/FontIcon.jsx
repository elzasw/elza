import React from 'react';
import {propsEquals} from 'components/Utils.jsx';
import classNames from 'classnames';
import AbstractReactComponent from '../../AbstractReactComponent';

class Icon extends AbstractReactComponent {
    static eqProps = ['className', 'glyph', 'onClick'];

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }

        return !propsEquals(this.props, nextProps, Icon.eqProps);
    }

    render() {
        const {glyph, className, ...otherProps} = this.props;
        const cls = {
            icon: true,
            ez: glyph.indexOf('ez-') === 0,
            fa: glyph.indexOf('fa-') === 0,
        };

        return <span className={classNames(cls, glyph, className)} {...otherProps} />;
    }
}

export default Icon;
