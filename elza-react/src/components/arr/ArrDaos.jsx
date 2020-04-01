import './ArrDaos.scss';
/**
 * Seznam balíčků se zobrazením detailu po kliknutí na balíček.
 */
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, HorizontalLoader, Icon, Splitter} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx';
import * as daoActions from 'actions/arr/daoActions.jsx';
import {WebApi} from 'actions/index.jsx';
import ArrDao from './ArrDao';

import flattenItems from 'components/shared/utils/itemFilter.jsx';
import List from 'components/shared/tree-list/TreeList.jsx';
import ListItem from '../shared/tree-list/list-item/ListItem';

class ArrDaos extends AbstractReactComponent {
    state = {
        leftSize: 240,
    };

    static propTypes = {
        type: PropTypes.oneOf(['PACKAGE', 'NODE', 'NODE_ASSIGN']).isRequired,
        unassigned: PropTypes.bool, // jen v případě packages
        fund: PropTypes.object.isRequired,
        selectedDaoId: PropTypes.object,
        nodeId: PropTypes.number,
        daoPackageId: PropTypes.number,
        onSelect: PropTypes.func,
        readMode: PropTypes.bool.isRequired,
    };

    static defaultProps = {
        unassigned: false,
    };

    componentDidMount() {
        this.handleFetch({}, this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.handleFetch(this.props, nextProps);
    }

    handleFetch = (prevProps, nextProps) => {
        const {type, unassigned, fund, nodeId, daoPackageId} = nextProps;

        if (type === 'NODE') {
            if (nodeId != null) {
                this.props.dispatch(daoActions.fetchNodeDaoListIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === 'NODE_ASSIGN') {
            if (nodeId != null) {
                this.props.dispatch(daoActions.fetchNodeDaoListAssignIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === 'PACKAGE') {
            if (daoPackageId != null) {
                this.props.dispatch(
                    daoActions.fetchDaoPackageDaoListIfNeeded(fund.versionId, daoPackageId, unassigned),
                );
            }
        }
    };

    renderItem = props => {
        return <ListItem renderName={props.item.renderName} {...props} />;
    };

    handleSelect = item => {
        const {onSelect, type, fund} = this.props;

        let daoList = {};
        if (type === 'NODE') {
            daoList = fund.nodeDaoList;
        } else if (type === 'NODE_ASSIGN') {
            daoList = fund.nodeDaoListAssign;
        } else if (type === 'PACKAGE') {
            daoList = fund.packageDaoList;
        }

        const index = indexById(daoList.rows, item.daoId);
        const daoItem = daoList.rows[index];
        const daoFileId = item.id.startsWith('f_') ? parseInt(item.id.replace('f_', '')) : null;

        onSelect && onSelect(daoItem, daoFileId);
    };

    handlePrevDaoFile = daoItem => {
        const {selectedDaoFileId, onSelect} = this.props;
        const index = indexById(daoItem.fileList, selectedDaoFileId);
        if (index != null) {
            const file = daoItem.fileList[index - 1];
            onSelect && onSelect(daoItem, file.id);
        }
    };

    handleNextDaoFile = daoItem => {
        const {selectedDaoFileId, onSelect} = this.props;
        const index = indexById(daoItem.fileList, selectedDaoFileId);
        if (index != null) {
            const file = daoItem.fileList[index + 1];
            onSelect && onSelect(daoItem, file.id);
        }
    };

    handleUnlink = dao => {
        const {fund} = this.props;
        WebApi.deleteDaoLink(fund.versionId, dao.daoLink.id);
    };

    renderDao = item => {
        const name = item.label || item.code + ' (' + item.daoId + ')';
        return (
            <div className="item-name" title={name}>
                {name}
            </div>
        );
    };

    renderFile = file => {
        const name = file.fileName || file.code + ' (' + file.id + ')';
        return (
            <div className="item-file" title={name}>
                <Icon glyph="fa-file-o" /> {name}
            </div>
        );
    };

    render() {
        const {type, fund, nodeId, daoPackageId, readMode, selectedDaoId, selectedDaoFileId, splitter} = this.props;

        let daoList = {};
        if (type === 'NODE') {
            daoList = fund.nodeDaoList;
        } else if (type === 'NODE_ASSIGN') {
            daoList = fund.nodeDaoListAssign;
        } else if (type === 'PACKAGE') {
            daoList = fund.packageDaoList;
        }

        const showPart = !(!daoList.fetched && daoPackageId);

        let items = {ids: []};

        let selectedDao = null;
        let selectedDaoFile = null;
        let selectedItemId = null;

        if (showPart) {
            const preItems = [];
            daoList.rows.forEach(item => {
                const children = [];
                item.fileList.forEach(file => {
                    const id = 'f_' + file.id;
                    if (item.id === selectedDaoId && selectedDaoFileId == file.id) {
                        selectedItemId = id;
                        selectedDao = item;
                        selectedDaoFile = file;
                    }
                    children.push({
                        ...file,
                        id: id,
                        daoId: item.id,
                        renderName: this.renderFile,
                    });
                });
                const id = 'd_' + item.id;
                if (item.id === selectedDaoId && selectedDaoFileId == null) {
                    selectedItemId = id;
                    selectedDao = item;
                    selectedDaoFile = null;
                }
                preItems.push({
                    ...item,
                    id: id,
                    daoId: item.id,
                    renderName: this.renderDao,
                    children: children,
                });
            });

            items = flattenItems(preItems, {getItemId: i => i.id});
        }

        return (
            <div className="daos-container">
                <Splitter
                    leftSize={this.state.leftSize}
                    onChange={({leftSize, rightSize}) => {
                        this.setState({leftSize: leftSize});
                    }}
                    left={
                        <div className="daos-list">
                            <div className="title">Digitální entity</div>
                            <div className="daos-list-items">
                                <List
                                    items={items}
                                    onChange={this.handleSelect}
                                    expandAll={true}
                                    selectedItemId={selectedItemId}
                                    ref={list => {
                                        this.list = list;
                                    }}
                                    renderItem={this.renderItem}
                                />
                                {!daoList.fetched && daoPackageId && <HorizontalLoader />}
                            </div>
                        </div>
                    }
                    center={
                        <div className="daos-detail">
                            {selectedDao && (
                                <ArrDao
                                    fund={fund}
                                    readMode={readMode}
                                    dao={selectedDao}
                                    prevDaoFile={() => this.handlePrevDaoFile(selectedDao)}
                                    nextDaoFile={() => this.handleNextDaoFile(selectedDao)}
                                    daoFile={selectedDaoFile}
                                    onUnlink={() => this.handleUnlink(selectedDao)}
                                />
                            )}
                        </div>
                    }
                />
            </div>
        );
    }
}

export default connect()(ArrDaos);
