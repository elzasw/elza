/**
 * Úvodní stránka administrace.
 *
 * @author Martin Šlapa
 * @since 22.12.2015
 */
import React from 'react';
import {connect} from 'react-redux';
import {i18n, Icon, RibbonGroup} from 'components/shared';
import {Button} from '../../components/ui';
import {developerSet} from 'actions/global/developer.jsx';
import {resetLocalStorage} from 'actions/store/storeEx.jsx';
import {WebApi} from 'actions/index.jsx';
import * as perms from 'actions/user/Permission.jsx';
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx';
import {getIndexStateFetchIfNeeded, reindex} from 'actions/admin/fulltext.jsx';

import './AdminPage.scss';
import AbstractReactComponent from '../../components/AbstractReactComponent';
import Ribbon from '../../components/page/Ribbon';
import PageLayout from '../shared/layout/PageLayout';
import { showConfirmDialog } from 'components/shared/dialog';
import { StatsAdmin } from 'components/shared/stats';

class AdminPage extends AbstractReactComponent {
    UNSAFE_componentWillReceiveProps = nextProps => {
        this.fetchData(nextProps);
    };

    componentDidMount = () => {
        this.fetchData(this.props);
    };

    fetchData = props => {
        const {fetched, userDetail} = props;

        if (userDetail.hasOne(perms.ADMIN)) {
            if (!fetched) {
                props.dispatch(getIndexStateFetchIfNeeded());
            }
        }
    };

    renderReindexing = () => {
        return <div>{i18n('admin.fulltext.message.reindexing')}</div>;
    };

    startReindexing = () => {
        this.props.dispatch(reindex());
    };

    handleDeveloperMode = () => {
        this.props.dispatch(developerSet(!this.props.developer.enabled));
    };

    handleResetLocalStorage = async () => {
        const {dispatch} = this.props;
        const response = await dispatch(showConfirmDialog(i18n('global.title.processAction')))
        if (response) {
            resetLocalStorage();
        }
    };

    handleResetServerCache = async () => {
        const {dispatch} = this.props;
        const response = await dispatch(showConfirmDialog(i18n('global.title.processAction')))
        if (response) {
            WebApi.resetServerCache().then(() => {
                this.props.dispatch(addToastrSuccess(i18n('admin.resetServerCache.success')));
            });
        }
    };

    buildRibbon() {
        const {
            userDetail,
            fulltext: {indexing},
        } = this.props;

        const altActions = [];

        if (userDetail.hasOne(perms.FUND_ARR_ALL, perms.FUND_ARR, perms.FUND_RD_ALL, perms.FUND_RD)) {
            altActions.push(
                <Button
                    active={this.props.developer.enabled}
                    key="developerMode"
                    onClick={this.handleDeveloperMode}
                    variant={'default'}
                >
                    <Icon glyph="fa-cogs" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.admin.developer')}</span>
                    </div>
                </Button>,
            );
        }

        altActions.push(
            <Button
                key="reindex"
                onClick={this.startReindexing}
                disabled={indexing}
                title={i18n('ribbon.action.admin.reindex.title')}
                variant={'default'}
            >
                <Icon glyph="fa-search" />
                <div>
                    <span className="btnText">
                        {indexing ? i18n('admin.fulltext.message.reindexing') : i18n('ribbon.action.admin.reindex')}
                    </span>
                </div>
            </Button>,
        );
        altActions.push(
            <Button
                key="resetLocalStorage"
                onClick={this.handleResetLocalStorage}
                title={i18n('ribbon.action.admin.resetLocalStorage.title')}
                variant={'default'}
            >
                <Icon glyph="fa-times" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.admin.resetLocalStorage')}</span>
                </div>
            </Button>,
        );
        altActions.push(
            <Button
                key="resetServerCache"
                onClick={this.handleResetServerCache}
                title={i18n('ribbon.action.admin.resetServerCache.title')}
                variant={'default'}
            >
                <Icon glyph="fa-times" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.admin.resetServerCache')}</span>
                </div>
            </Button>,
        );

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        return <Ribbon admin altSection={altSection} {...this.props} />;
    }

    render() {
        const {splitter} = this.props;

        return (
            <PageLayout
                splitter={splitter}
                className="admin-packages-page"
                ribbon={this.buildRibbon()}
                centerPanel={<StatsAdmin />}
            />
        );
    }
}

function mapStateToProps(state) {
    const {
        splitter,
        developer,
        userDetail,
        adminRegion: {fulltext},
    } = state;

    return {
        splitter,
        developer,
        userDetail,
        fulltext,
    };
}

export default connect(mapStateToProps)(AdminPage);
