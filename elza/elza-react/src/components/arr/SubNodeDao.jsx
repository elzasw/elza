import './SubNodeDao.scss';

import PropTypes from 'prop-types';

import React from 'react';
import NodeDaosForm from './NodeDaosForm';
import {AbstractReactComponent, HorizontalLoader, i18n, Icon} from 'components/shared';
import {Button} from '../ui';
import {connect} from 'react-redux';
import {modalDialogShow} from 'actions/global/modalDialog.jsx';

class SubNodeDao extends AbstractReactComponent {
    static propTypes = {
        daos: PropTypes.object.isRequired,
        versionId: PropTypes.number.isRequired,
        nodeId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        selectedSubNodeId: PropTypes.number.isRequired,
        routingKey: PropTypes.string.isRequired,
        readMode: PropTypes.bool.isRequired,
    };

    renderDao = (dao, index) => {
        let filesLabel = 'subNodeDao.dao.files.more';

        if (dao.fileCount == 1) {
            filesLabel = 'subNodeDao.dao.files.one';
        } else if (dao.fileCount < 5 && dao.fileCount > 1) {
            filesLabel = 'subNodeDao.dao.files.few';
        }

        let daoResults = [];

        if (dao.url) {
            daoResults.push(
                <div className="link">
                    <a target="_blank" rel="noopener noreferrer" href={dao.url}>
                        {dao.label} - {dao.fileCount} {i18n(filesLabel)}
                    </a>
                </div>,
            );
        } else {
            daoResults.push(
                <div className="link">
                    {dao.label} - {dao.fileCount} {i18n(filesLabel)}
                </div>,
            );
        }

        let actions = [];
        actions.push(
            <Button
                onClick={() => {
                    this.handleShowDetailOne(dao);
                }}
                title={i18n('subNodeDao.dao.action.showDetailOne')}
            >
                <Icon glyph="fa-eye" />
            </Button>,
        );
        daoResults.push(<div className="actions">{actions}</div>);

        return <div className="links">{daoResults}</div>;
    };

    renderForm = () => {
        const {daos} = this.props;

        return <div className="dao-form">{daos.data.map(this.renderDao)}</div>;
    };

    handleShowDetailAll = () => {
        const {fund, versionId, selectedSubNodeId, readMode} = this.props;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('subNodeDao.dao.title.node'),
                <NodeDaosForm readMode={readMode} nodeId={selectedSubNodeId} />,
                'dialog-lg node-dao-dialog',
            ),
        );
    };

    handleShowDetailOne = dao => {
        const {fund, selectedSubNodeId, readMode} = this.props;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('subNodeDao.dao.title.node'),
                <NodeDaosForm readMode={readMode} nodeId={selectedSubNodeId} daoId={dao.id} />,
                'dialog-lg node-dao-dialog',
            ),
        );
    };

    render() {
        const {daos} = this.props;

        return (
            daos.data.length > 0 && (
                <div className="node-dao">
                    <div className="node-dao-title">{i18n('subNodeDao.title')}</div>
                    <div className="actions">
                        <Button onClick={this.handleShowDetailAll} title={i18n('subNodeDao.dao.action.showDetailAll')}>
                            <Icon glyph="fa-eye" />
                        </Button>
                    </div>
                    {daos.isFetching ? <HorizontalLoader /> : this.renderForm()}
                </div>
            )
        );
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        fund,
    };
}

export default connect(mapStateToProps)(SubNodeDao);
