import './Resizer.scss';
import React from 'react';

const Resizer = React.forwardRef(({horizontal, onMouseDown}, ref) => {
    return <span ref={ref} className={'Resizer ' + (horizontal ? 'horizontal' : 'vertical')} onMouseDown={onMouseDown} />;
});

export default Resizer;
