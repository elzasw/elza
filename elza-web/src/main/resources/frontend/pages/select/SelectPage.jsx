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

    handleConfirm() {
        throw "You have to override this method!!!"
    };

    handleClose = () => {
        this.dispatch(modalDialogHide());
    };

    static renderTitles = (titles) => {
        const itemss = [];
        titles.map((i,index, self) => {
            itemss.push(<div>{i}</div>);
            (index+1) < self.length && itemss.push(<span>&nbsp;>&nbsp;</span>)
        });
        return <div className="titles-header">{itemss}</div>
    };

    getPageProps() {
        const {titles} = this.props;

        return {
            customRibbon: this.buildRibbonParts(),
            module: true,
            status: titles ? SelectPage.renderTitles(titles) : null
        }
    }

    buildRibbonParts() {
        return {
            altActions: [],
            itemActions: [],
            primarySection: [
                <RibbonGroup key="ribbon-group-main" className="large">
                    <Button onClick={this.handleClose} style={{borderRadius:'50%', background: '#CD5051', color: 'white'}}>
                        <Icon glyph="fa-times" style={{fontSize: '3em'}}/>
                    </Button>,
                    <Button onClick={this.handleConfirm} style={{borderRadius:'50%', background: '#86BB65', color: 'white'}}>
                        <Icon glyph="fa-check" style={{fontSize: '3em'}}/>
                    </Button>
                </RibbonGroup>
            ]
        }
    };
}

export default SelectPage;
