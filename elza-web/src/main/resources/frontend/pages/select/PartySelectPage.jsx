import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'

import classNames from 'classnames';

import {AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
    SearchWithGoto, AddRegistryForm, ImportForm,
    ListBox, Autocomplete, ExtImportForm, RegistryDetail, RibbonMenu,
    RibbonSplit} from 'components';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button} from 'react-bootstrap';
import {RegistryPage, PartyPage} from 'pages/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {logout} from 'actions/global/login.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {userPasswordChange} from 'actions/admin/user.jsx'
import SelectPage from './SelectPage'

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class PartySelectPage extends SelectPage {

    /**
     * @override
     */
    handleConfirm = () => {
        const {partyDetail, onChange} = this.props;
        if (!partyDetail.fetched || partyDetail.isFetching || !partyDetail.id) {
            return;
        }
        onChange(partyDetail.data);
        this.handleClose();
    };

    buildRibbonParts() {
        const parts = super.buildRibbonParts();
        parts.primarySection.push(<RibbonSplit key={"ribbon-spliter-pages"} />);
        parts.primarySection.push(
            <RibbonGroup key="ribbon-group-pages" className="large">
                <Button className="active">
                    <Icon glyph="fa-users" />
                    <div><span className="btnText">{i18n('ribbon.action.party')}</span></div>
                </Button>
            </RibbonGroup>
        );
        return parts;
    };

    render() {
        const {titles} = this.props;
        const props = {
            customRibbon: this.buildRibbonParts(),
            module: true,
            titles
        };

        return <PartyPage {...props} />;
    }
}

export default connect((state) => {
    const {app:{partyDetail}} = state;
    return {
        ...SelectPage.mapStateToProps(state),
        partyDetail
    }
})(PartySelectPage);

