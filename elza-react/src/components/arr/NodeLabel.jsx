/**
 * Komponenta zobrazuje název node včetně ikonky a reference mark.
 */
import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import {createReferenceMarkString, getGlyph} from 'components/arr/ArrUtils.jsx';

import './NodeLabel.scss';

// Na kolik znaků se má název položky oříznout
const NODE_NAME_MAX_CHARS = 60;

class NodeLabel extends AbstractReactComponent {
    static propTypes = {
        node: PropTypes.object.isRequired, // struktura tree node client
        nameMaxChars: PropTypes.number,
        inline: PropTypes.bool,
    };

    static defaultProps = {
        nameMaxChars: NODE_NAME_MAX_CHARS,
        inline: false,
    };

    render() {
        const {inline, node, nameMaxChars} = this.props;

        if (node == null) {
            return (
                <div className={'node-label' + (inline ? ' inline' : '')}>
                    <div className={'node-label-container'}>?</div>
                </div>
            );
        }

        const refMark = <div className="reference-mark">{createReferenceMarkString(node)}</div>;

        var name = node.name ? node.name : <i>{i18n('fundTree.node.name.undefined', node.id)}</i>;
        if (name.length > nameMaxChars) {
            name = name.substring(0, nameMaxChars - 3) + '...';
        }
        name = (
            <div title={name} className="name">
                {name}
            </div>
        );

        var icon = <Icon className="node-icon" glyph={getGlyph(node.icon)} />;

        return (
            <div className={'node-label' + (inline ? ' inline' : '')}>
                <div className={'node-label-container'}>
                    {refMark}
                    {icon}
                    {name}
                </div>
            </div>
        );
    }
}

export default NodeLabel;
