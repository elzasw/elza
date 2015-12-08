/**
 * Komponenta záložek otevřených AP.
 */

import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {Tabs} from 'components';
import {FaAppStoreActions} from 'actions';

require ('./FaTreeTabs.less');

var FaTreeTabs = class FaTreeTabs extends React.Component {
    constructor(props) {
        super(props);
    }

    handleFaSelect(item) {
        FaAppStoreActions.selectFa.asFunction(item.id);
    }

    handleFaClose(item) {
        FaAppStoreActions.closeFa.asFunction(item.id);
    }

    render() {
        var tabs = this.props.fas.map((fa) => {
            return {
                id: fa.id,
                title: "Fa " + fa.id
            }
        });

        return (
            <Tabs.Container className='fa-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={this.props.activeFa} onSelect={this.handleFaSelect} onClose={this.handleFaClose}/>
                <Tabs.Content>
                    FA {this.props.activeFa && this.props.activeFa.id}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

module.exports = FaTreeTabs;
