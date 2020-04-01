import React from 'react';
import {Circle} from '../glyphs';

export const inCircle = (WrappedComponent, customStyle) => {
    let IconWrapper = props => {
        const {secondaryStyle, ...otherProps} = props;
        let style = secondaryStyle ? {...secondaryStyle} : {fill: 'white'};
        return (
            <svg {...otherProps}>
                <Circle />
                <svg x="20%" y="20%">
                    <g transform="scale(0.6)">
                        <WrappedComponent style={style} />
                    </g>
                </svg>
            </svg>
        );
    };
    return IconWrapper;
};
