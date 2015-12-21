/**
 * Komponenta záložek otevřených AP.
 */

require ('./FaTreeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Tabs, FaTree, FaTreeLazy} from 'components';
import {AppActions} from 'stores';

import {selectFaTab, closeFaTab} from 'actions/arr/fa'


var FaTreeTabs = class FaTreeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        var tabs = this.props.fas.map((fa) => {
            return {
                id: fa.id,
                title: fa.name
            }
        });

        return (
            <Tabs.Container className='fa-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={this.props.activeFa}
                    onSelect={item=>this.dispatch(selectFaTab(item))}
                    onClose={item=>this.dispatch(closeFaTab(item))}
                />
                <Tabs.Content>
                    {this.props.activeFa && 
                        <FaTreeLazy 
                            {...this.props.activeFa.faTree}
                            faId={this.props.activeFa.id}
                            versionId={this.props.activeFa.versionId}
                        /> 
                    }
                        {false && <FaTree 
                            {...this.props.activeFa.faTree}
                            {...this.props.faTreeData}
                            faId={this.props.activeFa.id}
                            versionId={this.props.activeFa.versionId}
                        /> }
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(FaTreeTabs);
