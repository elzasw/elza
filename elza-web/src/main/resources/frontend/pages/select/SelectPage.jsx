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

const OPEN_PAGE = {
    PARTY: 'party',
    REGISTRY: 'registry',
};

import './SelectPage.less'

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class SelectPage extends AbstractReactComponent {

    static mapStateToProps(state) {
        const {userDetail, status} = state;
        return {
            userDetail,
            status,
        }
    }

    handleLogout = () => {
        this.dispatch(logout());
    };

    handlePasswordChangeForm = () => {
        this.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm
            onSubmitForm={this.handlePasswordChange}/>))
    };

    handlePasswordChange = (data) => {
        this.dispatch(userPasswordChange(data.oldPassword, data.password));
    };

    handlePageChange = (page) => {
        if (this.state.openPage !== page) {
            this.setState({openPage: page});
        }
    };

    handleConfirm() {
        throw "You have to override this method!!!"
    };

    handleClose = () => {
        this.dispatch(modalDialogHide());
    };

    static renderTitles = (titles) => {
        const items = [];
        titles.map((i,index, self) => {
            items.push(<div>{i}</div>)
            (index+1) < self.length && items.push(<span>&nbsp;>&nbsp;</span>)
        });
        return <div className="titles-header">{items}</div>
    };

    buildRibbon(centerGroup) {
        const {userDetail, status: {saveCounter}} = this.props;
        return <RibbonMenu>
            <RibbonGroup key="ribbon-group-main" className="large">
                <Button onClick={this.handleClose} style={{borderRadius:'50%', background: '#CD5051', color: 'white'}}>
                    <Icon glyph="fa-times" style={{fontSize: '3em'}}/>
                </Button>
                <Button onClick={this.handleConfirm} style={{borderRadius:'50%', background: '#86BB65', color: 'white'}}>
                    <Icon glyph="fa-check" style={{fontSize: '3em'}}/>
                </Button>
            </RibbonGroup>
            <RibbonSplit key="ribbon-spliter-1"/>
            {centerGroup}
            <RibbonGroup className="small right">
                <Dropdown bsStyle='default' key='user-menu' id='user-menu'>
                    <Dropdown.Toggle noCaret>
                        {userDetail.username}<Icon glyph="fa-user"/>
                    </Dropdown.Toggle>
                    <Dropdown.Menu>
                        <MenuItem eventKey="1"
                                  onClick={this.handlePasswordChangeForm}>{i18n('ribbon.action.admin.user.passwordChange')}</MenuItem>
                        <MenuItem divider/>
                        <MenuItem eventKey="2" onClick={this.handleLogout}>{i18n('ribbon.action.logout')}</MenuItem>
                    </Dropdown.Menu>
                </Dropdown>
                {saveCounter > 0 &&
                <div className="save-msg"><Icon glyph="fa-spinner fa-spin"/>{i18n('ribbon.saving')}</div>}
            </RibbonGroup>
        </RibbonMenu>
    };
}

export default SelectPage;
