/**
 * Name of a node of the structure tree on one line; the tree scrolls sideways for a long one and
 * the tooltip shows it whole.
 */
const TreeLabel = ({text}: {text: string}) => (
    <span className="explorer-tree-label" title={text}>{text}</span>
);

export default TreeLabel;
