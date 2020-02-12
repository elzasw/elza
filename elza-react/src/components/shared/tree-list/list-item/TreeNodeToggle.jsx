import React from "react";
import classNames from "classnames";
import Icon from "components/shared/icon/Icon";

const TreeNodeToggle = (props) => {
    const {onClick, expanded, hidden} = props;
    const expandedGlyph = "fa-minus-square-o";
    const collapsedGlyph = "fa-plus-square-o";
    let style = {};

    if(hidden) { style.visibility = "hidden"; }

    const cls = classNames({
        "node-expand-collapse": true,
        "expanded": expanded,
        "collapsed": !expanded
    });

    return <div
        className={cls}
        onClick={onClick}
        style={style}
    >
        <Icon glyph={expanded ? expandedGlyph : collapsedGlyph}/>
    </div>
}

export default TreeNodeToggle;
