import { fundSubNodeDaoChangeScenario } from "actions/arr/subNodeDaos";
import {} from 'components/shared';
import { useIntl } from 'react-intl';
import { daoMessages } from 'components/arr/daoMessages';
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
    const intl = useIntl();
    const dispatch = useThunkDispatch()

    return <Dropdown>
        <Dropdown.Toggle
            disabled={readMode}
            title={intl.formatMessage(daoMessages.subNodeDaoDaoActionChangeScenario)}
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
