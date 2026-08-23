/** Seznam digitálních entit se zobrazením detailu vybrané položky. */
import { useEffect, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';
import { HorizontalLoader, Icon, Splitter } from 'components/shared';
import { addToastrSuccess } from 'components/shared/toastr/ToastrActions';
import flattenItems from 'components/shared/utils/itemFilter';
import List from 'components/shared/tree-list/TreeList';
import { indexById } from 'stores/app/utils';
import * as daoActions from 'actions/arr/daoActions';
import { WebApi } from 'actions/index';
import { useAppThunkDispatch } from 'utils/hooks';
import { ArrDaoFileVO, ArrDaoVO } from 'typings/dao';
import { Fund, SimpleList } from 'typings/store';
import ListItem from '../shared/tree-list/list-item/ListItem';
import { ArrDao } from './ArrDao';

import './ArrDaos.scss';

const messages = defineMessages({
    listTitle: {
        id: 'arrDaos.list.title',
        defaultMessage: 'Digitální entity',
    },
    unlinkSuccess: {
        id: 'arrDaos.unlink.success',
        defaultMessage: 'Digitální entita byla odpojena od jednotky popisu',
    },
});

type DaosType = 'PACKAGE' | 'NODE' | 'NODE_ASSIGN';

interface Props {
    type: DaosType;
    fund: Fund;
    readMode: boolean;
    /** Jen pro type PACKAGE — zobrazit pouze DAO, která nejsou nikam přiřazená. */
    unassigned?: boolean;
    nodeId?: number | null;
    daoPackageId?: number | null;
    selectedDaoId?: number | null;
    selectedDaoFileId?: number | null;
    onSelect?: (dao: ArrDaoVO, daoFileId: number | null) => void;
    onLinkChange?: () => void;
}

export type ArrDaosProps = Props;

/** Položka stromu — DAO nese id "d_<daoId>", jeho soubor "f_<fileId>". */
interface TreeItem {
    id: string;
    daoId: number;
}

const daoListOf = (fund: Fund, type: DaosType): SimpleList<ArrDaoVO> | undefined => {
    switch (type) {
        case 'NODE':
            return fund.nodeDaoList;
        case 'NODE_ASSIGN':
            return fund.nodeDaoListAssign;
        case 'PACKAGE':
            return fund.packageDaoList;
    }
};

export function ArrDaos({
    type,
    fund,
    readMode,
    unassigned = false,
    nodeId,
    daoPackageId,
    selectedDaoId,
    selectedDaoFileId,
    onSelect,
    onLinkChange,
}: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const [leftSize, setLeftSize] = useState(240);

    const daoList = daoListOf(fund, type);

    // Seznam se dotahuje i po zneplatnění store (např. po odpojení vazby) — proto
    // je v závislostech i samotný seznam; fetchIfNeeded se hlídá vlastním dataKey.
    useEffect(() => {
        if (type === 'NODE') {
            if (nodeId != null) {
                dispatch(daoActions.fetchNodeDaoListIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === 'NODE_ASSIGN') {
            if (nodeId != null) {
                dispatch(daoActions.fetchNodeDaoListAssignIfNeeded(fund.versionId, nodeId));
            }
        } else if (type === 'PACKAGE') {
            if (daoPackageId != null) {
                dispatch(daoActions.fetchDaoPackageDaoListIfNeeded(fund.versionId, daoPackageId, unassigned));
            }
        }
    }, [dispatch, type, unassigned, fund.versionId, nodeId, daoPackageId, daoList]);

    const handleSelect = (item: TreeItem) => {
        const index = indexById(daoList?.rows, item.daoId);
        if (index == null) {
            return;
        }
        const dao = (daoList as SimpleList<ArrDaoVO>).rows[index];
        const daoFileId = item.id.startsWith('f_') ? parseInt(item.id.replace('f_', ''), 10) : null;
        onSelect?.(dao, daoFileId);
    };

    const handleStepDaoFile = (dao: ArrDaoVO, step: number) => {
        const files = dao.fileList || [];
        const index = indexById(files, selectedDaoFileId);
        if (index != null) {
            onSelect?.(dao, files[index + step].id);
        }
    };

    const handleUnlink = async (dao: ArrDaoVO) => {
        await WebApi.deleteDaoLink(fund.versionId, dao.daoLink.id);
        dispatch(addToastrSuccess(intl.formatMessage(messages.unlinkSuccess)));
        onLinkChange?.();
    };

    const renderDao = (item: ArrDaoVO & TreeItem) => {
        const name = item.label || item.code + ' (' + item.daoId + ')';
        return (
            <div className="item-name" title={name}>
                {name}
            </div>
        );
    };

    const renderFile = (file: ArrDaoFileVO) => {
        const name = file.fileName || file.code + ' (' + file.id + ')';
        return (
            <div className="item-file" title={name}>
                <Icon glyph="fa-file-o" /> {name}
            </div>
        );
    };

    const showPart = !(!daoList?.fetched && daoPackageId);

    if (showPart && (!daoList?.rows || daoList.rows.length === 0)) {
        return null;
    }

    let items: { ids: string[] } = { ids: [] };
    let selectedDao: ArrDaoVO | null = null;
    let selectedDaoFile: ArrDaoFileVO | null = null;
    let selectedItemId: string | null = null;

    if (showPart) {
        const preItems = (daoList as SimpleList<ArrDaoVO>).rows.map((dao) => {
            const children = (dao.fileList || []).map((file) => {
                const id = 'f_' + file.id;
                if (dao.id === selectedDaoId && selectedDaoFileId === file.id) {
                    selectedItemId = id;
                    selectedDao = dao;
                    selectedDaoFile = file;
                }
                return { ...file, id, daoId: dao.id, renderName: renderFile };
            });
            const id = 'd_' + dao.id;
            if (dao.id === selectedDaoId && selectedDaoFileId == null) {
                selectedItemId = id;
                selectedDao = dao;
                selectedDaoFile = null;
            }
            return { ...dao, id, daoId: dao.id, renderName: renderDao, children };
        });

        items = flattenItems(preItems, { getItemId: (i: TreeItem) => i.id });
    }

    return (
        <div className="daos-container">
            <Splitter
                leftSize={leftSize}
                onChange={({ leftSize: size }: { leftSize: number; rightSize: number }) => setLeftSize(size)}
                left={
                    <div className="daos-list">
                        <div className="title">{intl.formatMessage(messages.listTitle)}</div>
                        <div className="daos-list-items">
                            <List
                                items={items}
                                onChange={handleSelect}
                                expandAll={true}
                                selectedItemId={selectedItemId}
                                renderItem={(props: { item: { renderName: unknown } }) => (
                                    <ListItem renderName={props.item.renderName} {...props} />
                                )}
                            />
                            {!daoList?.fetched && daoPackageId && <HorizontalLoader />}
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
                                prevDaoFile={() => handleStepDaoFile(selectedDao as ArrDaoVO, -1)}
                                nextDaoFile={() => handleStepDaoFile(selectedDao as ArrDaoVO, 1)}
                                daoFile={selectedDaoFile ?? undefined}
                                onUnlink={() => handleUnlink(selectedDao as ArrDaoVO)}
                            />
                        )}
                    </div>
                }
            />
        </div>
    );
}

