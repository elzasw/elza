import React from 'react';

const DepthIndent = props => {
    let indentSize = props.depth * props.indentSize;
    return <div style={{width: indentSize}}></div>;
};

export default DepthIndent;
