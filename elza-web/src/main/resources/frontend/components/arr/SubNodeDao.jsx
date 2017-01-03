require('./SubNodeDao.less');

import React from 'react';
import {Icon, i18n, AbstractReactComponent, Loading} from 'components/index.jsx';
import {Button} from "react-bootstrap";
import {connect} from 'react-redux'

import {registrySelect, registryAdd} from 'actions/registry/registryRegionList.jsx'
import {routerNavigate} from 'actions/router.jsx'

const SubNodeDao = class SubNodeDao extends AbstractReactComponent {
    static PropTypes = {
        daos: React.PropTypes.object.isRequired,
        selectedSubNodeId: React.PropTypes.number.isRequired,
        routingKey: React.PropTypes.number.isRequired,
        nodeId: React.PropTypes.oneOfType([React.PropTypes.number, React.PropTypes.string]),
    };



    handleRemove(index) {
        //this.dispatch(fundSubNodeRegisterValueDelete(this.props.versionId, this.props.selectedSubNodeId, this.props.routingKey, index));
    }

    renderDao = (dao, index) => {
        const {closed, versionId, readMode} = this.props;

        let filesLabel = 'subNodeDao.dao.files.more';

        if (dao.filesCount == 1) {
            filesLabel = 'subNodeDao.dao.files.one';
        } else if (dao.filesCount < 5 && dao.filesCount > 1) {
            filesLabel = 'subNodeDao.dao.files.few';
        }

        let result;

        let daoResults = [];

        if (dao.url) {
            daoResults.push(<div className="link"><a target="_blank" href={dao.url} >{i18n('subNodeDao.dao.label')}: {dao.label}, {i18n('subNodeDao.dao.code')}: {dao.code} - {dao.filesCount} {i18n(filesLabel)}</a></div>);
        } else {
            daoResults.push(<div className="link">{i18n('subNodeDao.dao.label')}: {dao.label}, {i18n('subNodeDao.dao.code')}: {dao.code} - {dao.filesCount} {i18n(filesLabel)}</div>);
        }

        let actions = [];
        actions.push(<Button onClick={() => { this.handleShowDetailOne(dao) }} title={i18n('subNodeDao.dao.action.showDetailOne')}><Icon glyph='fa-pencil'/></Button>)
        daoResults.push(<div className="actions">{actions}</div>);

        return <div className='links'>{daoResults}</div>
    };

    renderForm = () => {
        const {daos} = this.props;

        return <div className="dao-form">
            {daos.data.map(this.renderDao)}
        </div>
    };

    handleShowDetailAll = () => {

    };

    handleShowDetailOne = (dao) => {

    };

    render() {
        const {daos} = this.props;

        return <div className='node-dao'>
            <div className='node-dao-title'>{i18n('subNodeDao.title')}</div>
            <div className="actions"><Button onClick={this.handleShowDetailAll} title={i18n('subNodeDao.dao.action.showDetailAll')}><Icon glyph='fa-pencil'/></Button></div>
            {daos.isFetching ? <Loading value={i18n('global.data.loading.dao')} /> : this.renderForm() }
        </div>
    }
};

function mapStateToProps(state) {
    const {arrRegion} = state;
    let fund = null;
    if (arrRegion.activeIndex != null) {
        fund = arrRegion.funds[arrRegion.activeIndex];
    }

    return {
        fund
    }
}

export default connect(mapStateToProps)(SubNodeDao);
