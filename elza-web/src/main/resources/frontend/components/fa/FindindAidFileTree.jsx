/**
 * Strom archivních souborů.
 */

import React from 'react';
import {i18n} from 'components';

require ('./FindindAidFileTree.less');

var FindindAidFileTree = class FindindAidFileTree extends React.Component {
    constructor(props) {
        super(props);
    }

    renderOpened() {
        return (
            <div className='finding-aid-file-tree-conteiner opened'>
                STROM všechny AP a jejich verze, zobrazené graficky
            </div>
        );
    }

    renderClosed() {
        return (
            <div className='finding-aid-file-tree-conteiner closed'>
                ...
            </div>
        );
    }

    render() {
        var opened = this.context.opened;

        return opened ? this.renderOpened() : this.renderClosed();
    }
}

FindindAidFileTree.contextTypes = {
    opened: React.PropTypes.bool
}

module.exports = FindindAidFileTree;