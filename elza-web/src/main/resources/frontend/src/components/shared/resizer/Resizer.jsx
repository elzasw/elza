import './Resizer.less'
import React from 'react';

export default const Resizer = (
    {
        horizontal,
        onMouseDown,
    },
) => {
    return <span className={'Resizer ' + (horizontal ? "horizontal" : "vertical")} onMouseDown={onMouseDown} />;
};

