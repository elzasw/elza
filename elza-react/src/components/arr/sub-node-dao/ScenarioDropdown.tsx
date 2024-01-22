import { fundSubNodeDaoChangeScenario } from "actions/arr/subNodeDaos";
import { i18n } from 'components/shared';
import { PropsWithChildren } from 'react';
import { Dropdown } from 'react-bootstrap';
import { ArrDaoVO } from "typings/dao";
import { Button } from '../../ui';
import './SubNodeDao.scss';
import { useThunkDispatch } from "utils/hooks";

export interface ScenarioDropdownProps  extends PropsWithChildren {
    dao: ArrDaoVO;
    readMode?: boolean;
    versionId: number;
    nodeId: number;
}

export const ScenarioDropdown = ({
    dao,
    readMode = false,
    versionId,
    nodeId,
    children,
}: ScenarioDropdownProps) => {
    const dispatch = useThunkDispatch()

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
