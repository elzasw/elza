/**
 * Komponenta záložek otevřených AP.
 */

require ('./FaTreeTabs.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Tabs} from 'components';
import {AppActions} from 'stores';

import {selectFa, closeFa} from 'actions/fa/fa'


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
                    onSelect={item=>this.dispatch(selectFa(item))}
                    onClose={item=>this.dispatch(closeFa(item))}
                />
                <Tabs.Content>
                    FA {this.props.activeFa && this.props.activeFa.id}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = connect()(FaTreeTabs);
