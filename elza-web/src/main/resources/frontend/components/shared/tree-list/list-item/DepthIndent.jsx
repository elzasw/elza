import React from "react";

const DepthIndent = (props) => {
    const depth = props.depth || 0;
    let indentSize = props.depth * props.indentSize;
    return <div style={{width: indentSize}}></div>
}

export default DepthIndent;
