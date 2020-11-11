import { modalDialogShow } from 'actions/global/modalDialog.jsx';
import { HorizontalLoader, i18n, Icon } from 'components/shared';
import React, { FC } from 'react';
import { connect } from 'react-redux';
import { ArrSubNodeDaosVO } from "typings/dao";
import { Button } from '../../ui';
import NodeDaosForm from '../NodeDaosForm';
import './SubNodeDao.scss';
import { SubNodeDaoItem } from "./SubNodeDaoItem";
import {useDispatch} from "react-redux";

const SubNodeDao:FC<{
    daos: ArrSubNodeDaosVO;
    selectedSubNodeId: number;
    readMode?: boolean;
    versionId: number;
    nodeId: number;
}> = ({
    daos, 
    selectedSubNodeId, 
    readMode = true,
    versionId,
    nodeId,
}) => {
    const dispatch = useDispatch();

    const handleShowDetail = (
        daoId?: number,
    ) => {
        dispatch(
            modalDialogShow(
                this,
                i18n('subNodeDao.dao.title.node'),
                <NodeDaosForm readMode={readMode} nodeId={selectedSubNodeId} daoId={daoId} />,
                'dialog-lg node-dao-dialog',
            ),
        );
    };

    const handleShowDetailAll = () => handleShowDetail();

    return (
        daos.data.length > 0 ? (
            <div className="node-dao">
                <div className="node-dao-title">{i18n('subNodeDao.title')}</div>
                <div className="actions">
                    <Button onClick={handleShowDetailAll} title={i18n('subNodeDao.dao.action.showDetailAll')}>
                        <Icon glyph="fa-eye" />
                    </Button>
                </div>
                { daos.isFetching || !daos.fetched ? 
                    <HorizontalLoader /> : 
                    <div className="dao-form">{
                        daos.data.map(( item, index )=>
                            <SubNodeDaoItem 
                                versionId={versionId} 
                                nodeId={nodeId} 
                                dao={item} 
                                index={index} 
                                onShowDetail={handleShowDetail}
                                readMode={readMode}
                            />
                        )
                    }</div>
                }
            </div>
        ) : null
    );
}

/*
SubNodeDao.propTypes = {
    daos: PropTypes.object.isRequired,
    selectedSubNodeId: PropTypes.number.isRequired,
    readMode: PropTypes.bool,
};
 */

function mapStateToProps(state: any) {
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
