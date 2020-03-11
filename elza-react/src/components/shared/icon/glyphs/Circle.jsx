import React from 'react';

const Circle = (props) => {
    return(
        <g {...props}>
            <circle r="10" cx="10" cy="10" id="circle"/>
        </g>
    )
}
export default Circle;
