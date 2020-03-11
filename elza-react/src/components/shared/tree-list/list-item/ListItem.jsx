import React from "react";
import PropTypes from "prop-types";
import TreeNodeToggle from "./TreeNodeToggle.jsx";
import DepthIndent from "./DepthIndent.jsx";
import classNames from "classnames";
import {propsEquals} from "components/Utils.jsx";

class ListItem extends React.PureComponent {

    static defaultProps = {
        highlighted: false,
        indent: 10,
        selected: false,
        selectable: true,
        focusable: true,
        depth: 0,
        expanded: false,
        className: "",
        ignoreDepth: false,
        onExpandCollapse: (e) => {throw "callback 'onExpandCollapse' for 'ListItem' is not defined"}
    };

    static propTypes = {
        highlighted: PropTypes.bool,
        selected: PropTypes.bool,
        selectable: PropTypes.bool,
        focusable: PropTypes.bool,
        depth: PropTypes.number,
        expanded: PropTypes.bool,
        className: PropTypes.string,
        ignoreDepth: PropTypes.bool,
        onExpandCollapse: PropTypes.func,
        renderName: PropTypes.func
    };

    constructor(props){
        super(props);
    }

    shouldComponentUpdate(nextProps){
        // re-renders only when one of these props changes
        return !propsEquals(this.props, nextProps,[
            "name",
            "highlighted",
            "selected",
            "selectable",
            "focusable",
            "depth",
            "expanded",
            "className",
            "ignoreDepth",
            "hasChildren",
            "indent"
        ]);
    }

    render() {
        const {
            item,
            name,
            highlighted,
            selected,
            selectable,
            focusable,
            depth,
            expanded,
            className,
            onExpandCollapse,
            ignoreDepth,
            hasChildren,
            indent,
            renderName,
            ...otherProps
        } = this.props;

        const cls = classNames({
            "item": true,
            "focus": highlighted,
            "active": selected,
            "not-selectable": !selectable,
            "not-focusable": !focusable,
            [className]: className
        });

        return (
            <div
                {...otherProps}
                className={cls}
            >
                {!ignoreDepth && depth >= 0 && <DepthIndent depth={depth} indentSize={indent}/>}
                {!ignoreDepth && <TreeNodeToggle expanded={expanded} hidden={!hasChildren} onClick={onExpandCollapse}/>}
                <div className="item-text">{renderName ? renderName(item) : name}</div>
            </div>
        )
    }
}

export default ListItem;
