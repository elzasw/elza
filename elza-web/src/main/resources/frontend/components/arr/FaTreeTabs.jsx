/**
 * Komponenta záložek otevřených AP.
 */

require ('./FaTreeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Tabs, FaTreeLazy} from 'components';
import {AppActions} from 'stores';
import {selectFaTab, closeFaTab} from 'actions/arr/fa'

var FaTreeTabs = class FaTreeTabs extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        if (this.props.fas.length == 0) {
            return <div></div>
        }

        var tabs = this.props.fas.map((fa) => {
            return {
                id: fa.id,
                key: fa.id,
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
                    <FaTreeLazy 
                        {...this.props.activeFa.faTree}
                        versionId={this.props.activeFa.versionId}
                    /> 
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(FaTreeTabs);
