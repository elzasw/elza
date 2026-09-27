/**
 * Name of a node of the structure tree on one line; a name longer than the tree is cut off and
 * shown whole in the tooltip.
 */
const TreeLabel = ({text}: {text: string}) => (
    <span className="explorer-tree-label" title={text}>{text}</span>
);

export default TreeLabel;
