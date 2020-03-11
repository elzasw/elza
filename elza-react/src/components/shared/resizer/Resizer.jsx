import './Resizer.scss';
import React from 'react';

const Resizer = (
    {
        horizontal,
        onMouseDown,
    },
) => {
    return <span className={'Resizer ' + (horizontal ? "horizontal" : "vertical")} onMouseDown={onMouseDown} />;
};

export default Resizer;

