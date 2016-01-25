/**
 * Komponenta rozšířeného zobrazení AP.
 */

require ('./FaExtendedView.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FaTreeMain, FaTreeMovementsLeft, FaTreeMovementsRight} from 'components';
import * as types from 'actions/constants/actionTypes';
import {MenuItem, Button} from 'react-bootstrap';
import {selectFaTab, closeFaTab} from 'actions/arr/fa'
import {faTreeFocusNode, faTreeFetchIfNeeded, faTreeNodeExpand, faTreeNodeCollapse} from 'actions/arr/faTree'
import {faSelectSubNode} from 'actions/arr/nodes'
import {createFaRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'


var FaExtendedView = class FaExtendedView extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.tabItems = [{id:0, title: 'Strom AP'}, {id: 1, title: 'Hromadné úpravy JP'}, {id: 2, title: 'Přesuny JP'}];
        this.state = { selectedTabItem: this.tabItems[2] }
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    render() {
        const {fa} = this.props;

        var tabContent = [];
        var tabContentClassName;
        if (this.state.selectedTabItem.id == 0) {
            tabContent.push(
                <FaTreeMain
                    fa={fa}
                    versionId={fa.versionId}
                    {...fa.faTree}
                />
            )
        } else if (this.state.selectedTabItem.id == 2) {
            tabContentClassName = 'movements'
            tabContent.push(
                <div key={1} className='tree-left-container'>
                    <FaTreeMovementsLeft
                        fa={fa}
                        versionId={fa.versionId}
                        {...fa.faTreeMovementsLeft}
                    />
                </div>
            )

            var leftHasSelection = Object.keys(fa.faTreeMovementsLeft.selectedIds).length > 0;
            var rightHasSelection = fa.faTreeMovementsRight.selectedId != null;
            var active = leftHasSelection && rightHasSelection;
            tabContent.push(
                <div key={2} className='tree-actions-container'>
                    <Button disabled={!active}>Přesunout do</Button>
                    <Button disabled={!active}>Přesunout před</Button>
                    <Button disabled={!active}>Přesunout za</Button>
                </div>
            )
            tabContent.push(
                <div key={3} className='tree-right-container'>
                    <FaTreeMovementsRight
                        fa={fa}
                        versionId={fa.versionId}
                        {...fa.faTreeMovementsRight}
                    />
                </div>
            )
        }

        return (
            <Tabs.Container className='fa-extended-view-container'>
                <Tabs.Tabs items={this.tabItems} activeItem={this.state.selectedTabItem}
                    onSelect={item=>this.setState({selectedTabItem: item})}
                />
                <Tabs.Content className={tabContentClassName}>
                    {tabContent}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

FaExtendedView.propTypes = {
    fa: React.PropTypes.object,
}

module.exports = connect()(FaExtendedView);

