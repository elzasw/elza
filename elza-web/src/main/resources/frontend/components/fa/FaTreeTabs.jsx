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
            <div className='fa-tabs-container'>
                <Tabs items={tabs} activeItem={this.state.activeFa} onSelect={this.handleFaSelect} onClose={this.handleFaClose}/>
                <div className='tab-content'>
                    FA {this.state.activeFa && this.state.activeFa.id}
                </div>
            </div>
        );
    }
}

module.exports = FaTreeTabs;
