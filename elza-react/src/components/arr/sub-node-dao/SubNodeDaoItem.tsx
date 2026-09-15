import { Icon } from 'components/shared';
import { FormattedMessage, useIntl } from 'react-intl';
import { daoMessages } from 'components/arr/daoMessages';
import React, { FC } from 'react';
import { ArrDaoVO } from "typings/dao";
import { Button } from '../../ui';
import { ScenarioDropdown } from './ScenarioDropdown';
import './SubNodeDao.scss';

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
    const intl = useIntl();
    const handleShowDetail = () => onShowDetail(dao.id);

    return (
        <div className="links" key={'dao-item-' + index}>
            <div className="link" key={'link'}>
                {dao.url ?
                        <a target="_blank" rel="noopener noreferrer" href={dao.url}
                           title={dao.truncated ? intl.formatMessage(daoMessages.subNodeDaoDaoFilesTruncated) : undefined}>
                            {dao.label} - <FormattedMessage
                                {...(dao.truncated ? daoMessages.daoFileCountTruncated : daoMessages.daoFileCount)}
                                values={{ count: dao.fileCount }}
                            />
                        </a>
                    :
                        <span title={dao.truncated ? intl.formatMessage(daoMessages.subNodeDaoDaoFilesTruncated) : undefined}>
                            {dao.label} - <FormattedMessage
                                {...(dao.truncated ? daoMessages.daoFileCountTruncated : daoMessages.daoFileCount)}
                                values={{ count: dao.fileCount }}
                            />
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
                    title={<FormattedMessage {...daoMessages.subNodeDaoDaoActionShowDetailOne} />}
                >
                    <Icon glyph="fa-eye" />
                </Button>
            </div>
        </div>
    );
}
