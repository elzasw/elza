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
        const {fas, activeFa} = this.props;

        if (fas.length == 0) {
            return <div></div>
        }

        var tabs = fas.map((fa) => {
            return {
                id: fa.id,
                key: fa.id,
                title: fa.name
            }
        });

        return (
            <Tabs.Container className='fa-tabs-container'>
                <Tabs.Tabs items={tabs} activeItem={activeFa}
                    onSelect={item=>this.dispatch(selectFaTab(item))}
                    onClose={item=>this.dispatch(closeFaTab(item))}
                />
                <Tabs.Content>
                    <FaTreeLazy 
                        fa={activeFa}
                        {...activeFa.faTree}
                        versionId={this.props.activeFa.versionId}
                    /> 
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

FaTreeTabs.propTypes = {
    fas: React.PropTypes.array.isRequired,
    activeFa: React.PropTypes.object,
}

module.exports = connect()(FaTreeTabs);
