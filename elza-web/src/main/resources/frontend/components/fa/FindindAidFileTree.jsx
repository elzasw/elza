/**
 * Strom archivních souborů.
 */

require ('./FindindAidFileTree.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {AppActions} from 'stores';

var {faActions} = AppActions.AppActions;

var FindindAidFileTree = class FindindAidFileTree extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');

        if (props.isParentOpened) {
            this.dispatch(faActions.fetchFaFileTreeIfNeeded());
        }
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.isParentOpened) {
            this.dispatch(faActions.fetchFaFileTreeIfNeeded());
        }
    }

    handleSelect(item) {
        this.dispatch(faActions.selectFa(item));
        this.props.onSelect(item);
    }

    renderOpened() {
        var rows = this.props.items.map(item=>{
            return (
                <div key={item.id} onClick={this.handleSelect.bind(this, item)}>
                    {item.name}
                </div>
            )
        });

        return (
            <div className='finding-aid-file-tree-conteiner'>
                {this.props.isFetching && <Loading/>}
                {!this.props.isFetching && rows}
            </div>
        );
    }

    renderClosed() {
        return (
            <div className='finding-aid-file-tree-conteiner'>
                ...
            </div>
        );
    }

    render() {
        return this.props.isParentOpened ? this.renderOpened() : this.renderClosed();
    }
}

module.exports = connect()(FindindAidFileTree);