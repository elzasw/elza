import {fundSubNodeDaoChangeScenario} from "actions/arr/subNodeDaos";
import {i18n, Icon} from 'components/shared';
import React, {FC} from 'react';
import {Dropdown} from 'react-bootstrap';
import {useDispatch} from "react-redux";
import {ArrDaoVO} from "typings/dao";
import {Button} from '../../ui';
import './SubNodeDao.scss';

const getPlurality = (count: number) => {
    return count === 1 ? "one" : count > 1 && count < 5? "few" : "more";
}

export const SubNodeDaoItem:FC<{
    dao: ArrDaoVO;
    index: number;
    onShowDetail: (daoId: number) => void;
    versionId: number;
    nodeId: number;
    readMode?: boolean;
}> = ({
    dao, 
    index, 
    onShowDetail,
    versionId,
    nodeId,
    readMode = true,
}) => {
    const dispatch = useDispatch();
    const handleShowDetail = () => onShowDetail(dao.id);

    return (
        <div className="links" key={'dao-item-' + index}>
            <div className="link" key={'link'}>
                {dao.url ?
                        <a target="_blank" rel="noopener noreferrer" href={dao.url}>
                            {dao.label} - {dao.fileCount} {i18n(`subNodeDao.dao.files.${getPlurality(dao.fileCount)}`)}
                        </a>
                    :
                        <span>
                            {dao.label} - {dao.fileCount} {i18n(`subNodeDao.dao.files.${getPlurality(dao.fileCount)}`)}
                        </span>
                }
            </div>
            {dao.daoLink.scenario &&
                <div style={{marginRight:"5px"}}>
                    {dao.daoLink.scenario}
                </div>
            }
            <div style={{display: "flex"}} className="actions" key={'actions'}>
                <Button
                    key={'show'}
                    onClick={handleShowDetail}
                    title={i18n('subNodeDao.dao.action.showDetailOne')}
                >
                    <Icon glyph="fa-eye" />
                </Button>
                {dao.daoLink?.scenario && !readMode &&
                    <Dropdown>
                        <Dropdown.Toggle 
                            disabled={readMode} 
                            title={i18n('subNodeDao.dao.action.changeScenario')}
                            as={Button} 
                            id="scenario"
                        >
                            <Icon glyph="fa-cogs" />
                        </Dropdown.Toggle>
                        <Dropdown.Menu alignRight={true}>
                            {dao.scenarios?.map((scenario)=>{
                                const handleSelectScenario = () => dispatch(fundSubNodeDaoChangeScenario(dao.id, scenario, versionId, nodeId));
                                return <Dropdown.Item onClick={handleSelectScenario}>
                                    {scenario}
                                </Dropdown.Item>
                            })}
                        </Dropdown.Menu>
                    </Dropdown>
                }
            </div>
        </div>
    );
}
