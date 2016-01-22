/**
 * Úvodní stránka administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import ReactDOM from 'react-dom';

require ('./AdminPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {Ribbon, ModalDialog, NodeTabs, PartySearch} from 'components';
import {ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';

var AdminPage = class AdminPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }

    render() {

        var centerPanel = (
                <div>
                    Administrace - HOME
                </div>
        )

        return (
                <PageLayout
                        className='admin-packages-page'
                        ribbon={this.buildRibbon()}
                        centerPanel={centerPanel}
                        />
        )
    }
}

module.exports = AdminPage;

