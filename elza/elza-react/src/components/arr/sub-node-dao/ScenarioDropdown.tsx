import { fundSubNodeDaoChangeScenario } from "actions/arr/subNodeDaos";
import { i18n } from 'components/shared';
import React, { FC } from 'react';
import { Dropdown } from 'react-bootstrap';
import { useDispatch } from "react-redux";
import { ArrDaoVO } from "typings/dao";
import { Button } from '../../ui';
import './SubNodeDao.scss';

export interface ScenarioDropdownProps {
    dao: ArrDaoVO;
    readMode?: boolean;
    versionId: number;
    nodeId: number;
}

export const ScenarioDropdown:FC<ScenarioDropdownProps> = ({
    dao,
    readMode = false,
    versionId,
    nodeId,
    children,
}) => {
    const dispatch = useDispatch()

    return <Dropdown>
        <Dropdown.Toggle 
            disabled={readMode} 
            title={i18n('subNodeDao.dao.action.changeScenario')}
            as={Button} 
            id="scenario"
        >
            {children}
        </Dropdown.Toggle>
        <Dropdown.Menu>
            {dao.scenarios?.map((scenario, index)=>{
                const handleSelectScenario = () => dispatch(fundSubNodeDaoChangeScenario(dao.id, scenario, versionId, nodeId));
                return <Dropdown.Item key={index} onClick={handleSelectScenario}>
                    {scenario}
                </Dropdown.Item>
            })}
        </Dropdown.Menu>
    </Dropdown>
}
