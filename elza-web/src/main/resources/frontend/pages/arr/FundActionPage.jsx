/**
 * Stránka archivních pomůcek.
 */

require('./FundActionPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {Loading, Icon, Ribbon, i18n, AbstractReactComponent, ListBox} from 'components/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {fundActionsFetchDetailIfNeeded, fundActionsFetchListIfNeeded, fundActionsActionRequest} from 'actions/arr/fundAction.jsx'


var FundActionPage = class FundActionPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(

        );

        this.state = {};
    }

    componentDidMount() {
        this.dispatch(fundActionsFetchListIfNeeded())
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fundActionsFetchDetailIfNeeded())
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        return (
            <Ribbon arr />
        )
    }

    renderCenter(fund) {
        if (!fund) {
            return <div className='center-container'>Není vybrán FA</div>;
        }
        if (!fund) {}
        return <div className='center-container'></div>
    }

    renderRowItem(item) {
        var icon = item.state === 'R' ? <Icon glyph='fa-cog' /> : '';
        const name = <span title={item.name} className="name">{item.name}</span>

        return (
            <div className='item' key={item.id}>
                {icon}
                <div>
                    <div>{name}</div>
                    <div>
                        {item.date}
                        {item.state}
                    </div>
                </div>

            </div>
        )
    }

    handleSelectAction(item) {
        this.dispatch(fundActionsActionRequest(item.id));
    }

    render() {
        const {arrRegion, splitter} = this.props;
        const fund = arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : false;

        const leftPanel = <div className='actions-list-container'>
            <ListBox
                className='actions-listbox'
                key='actions-list'
                items={[
                    {
                        name: 'Nazev',
                        state: 'R',
                        date: 'DATUM nevim'
                    }
                ]}
                renderItemContent={this.renderRowItem.bind(this)}
                onSelect={this.handleSelectAction}
            />
        </div>;
        const centerPanel = this.renderCenter(fund);

        return (
            <PageLayout
                className="arr-actions-page"
                splitter={splitter}
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        )
    }
};

FundActionPage.propTypes = {
};

function mapStateToProps(state) {
    const {arrRegion, splitter} = state;
    return {
        arrRegion,
        splitter
    }
}

module.exports = connect(mapStateToProps)(FundActionPage);
