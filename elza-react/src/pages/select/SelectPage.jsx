import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'

import classNames from 'classnames';

import {AbstractReactComponent, i18n, Loading, RibbonGroup, Icon} from 'components/shared';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button, Dropdown, MenuItem} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {logout} from 'actions/global/login.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {userPasswordChange} from 'actions/admin/user.jsx'

import './SelectPage.scss'

const OPEN_PAGE = {
    PARTY: 'party',
    REGISTRY: 'registry',
};

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class SelectPage extends AbstractReactComponent {

    handleConfirm() {
        throw "You have to override this method!!!"
    };

    handleClose = () => {
        this.props.onClose();
    };

    static renderTitles = (titles) => {
        const itemss = [];
        titles.map((i,index, self) => {
            itemss.push(<div key={index}>{i}</div>);
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
                <RibbonGroup key="ribbon-group-main" className="large big-icon">
                    <Button onClick={this.handleClose} className="cancel">
                        <Icon glyph="fa-times-circle"/>
                    </Button>
                    <Button onClick={this.handleConfirm} className="confirm">
                        <Icon glyph="fa-check-circle"/>
                    </Button>
                </RibbonGroup>
            ]
        }
    };
}

export default SelectPage;
