import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'

import classNames from 'classnames';

import {AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
    SearchWithGoto, RegistryPanel, AddRegistryForm, ImportForm,
    ListBox, Autocomplete, ExtImportForm, RegistryDetail, RibbonMenu,
    RibbonSplit} from 'components';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button, Dropdown, MenuItem} from 'react-bootstrap';
import {RegistryPage, PartyPage} from 'pages/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {logout} from 'actions/global/login.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {userPasswordChange} from 'actions/admin/user.jsx'
import SelectPage from './SelectPage'

const OPEN_PAGE = {
    PARTY: 'party',
    REGISTRY: 'registry',
};

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class RegistrySelectPage extends SelectPage {
    state = {
        openPage: 'registry'
    };

    static PropTypes = {
        hasParty: React.PropTypes.bool
    };

    static defaultProps = {
        hasParty: true
    };

    /**
     * @override
     */
    handleConfirm = () => {
        const {registryDetail, partyDetail, onChange} = this.props;
        switch (this.state.openPage) {
            case OPEN_PAGE.PARTY:
                if (!partyDetail.fetched || partyDetail.isFetching || !partyDetail.id) {
                    return;
                }
                onChange(partyDetail.data.record);
                break;
            case OPEN_PAGE.REGISTRY:
                if (!registryDetail.fetched || registryDetail.isFetching || !registryDetail.id) {
                    return;
                }
                onChange(registryDetail.data);
                break;
        }
        this.handleClose();
    };

    buildRibbonParts() {
        const {openPage} = this.state;
        const {hasParty} = this.props;


        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={"ribbon-spliter-pages"} />);
        const items = [];
        items.push(<Button className={classNames({active: openPage == OPEN_PAGE.REGISTRY})}
                onClick={this.handlePageChange.bind(this, OPEN_PAGE.REGISTRY)}>
            <Icon glyph="fa-th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div>
        </Button>);

        if (hasParty) {
            items.push(<Button className={classNames({active: openPage == OPEN_PAGE.PARTY})} onClick={this.handlePageChange.bind(this, OPEN_PAGE.PARTY)}>
                <Icon glyph="fa-users" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div>
            </Button>);
        }

        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                {items}
            </RibbonGroup>
        );

        return parts;
    };


    render() {
        const {openPage} = this.state;

        const {titles} = this.props;

        const props = {
            customRibbon: this.buildRibbonParts(),
            module: true,
            titles
        };

        switch (openPage) {
            case OPEN_PAGE.REGISTRY:
                return <RegistryPage {...props} />;
            case OPEN_PAGE.PARTY:
                return <PartyPage {...props} />;
        }
    }
}


function mapStateToProps(state) {
    const {app:{registryDetail, partyDetail}} = state;
    return {
        ...SelectPage.mapStateToProps(state),
        registryDetail,
        partyDetail
    }
}

export default connect(mapStateToProps)(RegistrySelectPage);
