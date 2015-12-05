/**
 * Komponenta záložek otevřených AP.
 */

import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {Tabs} from 'components';
import {FaAppStore} from 'stores';
import {FaAppStoreActions} from 'actions';

require ('./FaTreeTabs.less');

var FaTreeTabs = class FaTreeTabs extends React.Component {
    constructor(props) {
        super(props);

        this.state = this.getStateFromStore(FaAppStore);

        FaAppStore.listen(status => {
            this.setState(this.getStateFromStore(status));
        });
    }

    getStateFromStore(store) {
        return {
            activeFa: store.getActiveFa(),
            fas: store.getAllFas()
        };
    }

    handleFaSelect(item) {
        FaAppStoreActions.selectFa.asFunction(item.id);
    }

    handleFaClose(item, newActiveItem) {
        FaAppStoreActions.closeFa.asFunction(item.id, newActiveItem ? newActiveItem.id : null);
    }

    render() {
        var tabs = this.state.fas.map((fa) => {
            return {
                id: fa.id,
                title: "Fa " + fa.id
            }
        });

        return (
            <Tabs.Container className='fa-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={this.state.activeFa} onSelect={this.handleFaSelect} onClose={this.handleFaClose}/>
                <Tabs.Content>
                    FA {this.state.activeFa && this.state.activeFa.id}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = FaTreeTabs;
