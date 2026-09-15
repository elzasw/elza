import { createFund } from 'actions/arr/fund';
import { modalDialogShow } from 'actions/global/modalDialog';
import * as perms from 'actions/user/Permission';
import { Api } from 'api';
import { FundForm } from 'components';
import { WebApi } from "actions/WebApi";
import PageLayout from 'pages/shared/layout/PageLayout';
import { StatsHome } from "components/shared/stats";
import './HomePage.scss';
import { HomePageRibbon, EntityItem, FundItem } from 'components/home';
import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { FundDetail, TasksEntityType, TasksStatus, TasksViewDetail } from 'elza-api';
import { urlEntity, urlEntityRevision } from '../../constants';
import { Link } from 'react-router-dom';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import type { ReactNode } from 'react';

// Id jsou převzatá z legacy katalogu beze změny. Texty o "rejstříkových heslech"
// jsou sjednocené na "archivní entity" - tak se ta věc jmenuje v celé aplikaci
// včetně nadpisu hned nad tímto seznamem.
const messages = defineMessages({
    addFundTitle: {
        id: 'arr.fund.title.add',
        defaultMessage: 'Vytvoření nového AS',
    },
    recentFundsTitle: {
        id: 'home.recent.fund.title',
        defaultMessage: 'Naposledy otevřené archivní soubory',
    },
    recentFundsEmptyTitle: {
        id: 'home.recent.fund.emptyList.title',
        defaultMessage: 'Zatím nebyly otevřeny žádné archivní soubory.',
    },
    recentFundsEmptyMessage: {
        id: 'home.recent.fund.emptyList.message',
        defaultMessage: 'V této sekci se nachází historie otevřených archivních souborů.',
    },
    recentEntitiesTitle: {
        id: 'home.recent.registry.title',
        defaultMessage: 'Naposledy zobrazené archivní entity',
    },
    recentEntitiesEmptyTitle: {
        id: 'home.recent.registry.emptyList.title',
        defaultMessage: 'Zatím nebyly otevřeny žádné archivní entity.',
    },
    recentEntitiesEmptyMessage: {
        id: 'home.recent.registry.emptyList.message',
        defaultMessage: 'V této sekci se nachází historie otevřených archivních entit.',
    },
    tasksTitle: {
        id: 'home.tasks.title',
        defaultMessage: 'Úkoly',
    },
});

/**
 * Názvy typů úkolů.
 *
 * Server posílá `taskTypeName` z `wf_task_type`, což je jazyk nastavený při
 * importu balíčku pravidel - přeložit se nedá. Vedle toho ale posílá i
 * `taskTypeCode`, a ten je stabilní identita (konstanty na `WfTaskType`),
 * takže popisek si umíme složit sami. Kód mimo tuhle množinu (typ z jiného
 * balíčku) spadne zpátky na serverové jméno.
 */
const taskTypeMessages = defineMessages({
    AP_UPDATE: { id: 'home.tasks.type.AP_UPDATE', defaultMessage: 'Úprava entity' },
    AP_CONFIRM: { id: 'home.tasks.type.AP_CONFIRM', defaultMessage: 'Schválení entity' },
    AP_REV_UPDATE: { id: 'home.tasks.type.AP_REV_UPDATE', defaultMessage: 'Úprava revize' },
    AP_REV_CONFIRM: { id: 'home.tasks.type.AP_REV_CONFIRM', defaultMessage: 'Schválení revize entity' },
});

export default function HomePage() {
    const [fundDetails, setFundDetails] = useState<FundDetail[]>([]);
    const [tasks, setTasks] = useState<TasksViewDetail[]>([]);

    const { arrRegionFront, registryRegionFront } = useSelector(({ stateRegion }: AppState) => stateRegion);
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const dispatch = useThunkDispatch();
    const intl = useIntl();

    useEffect(() => {
        const funds = arrRegionFront;
        if (funds?.length > 0) {
            Promise.all(funds.map((fund) => Api.funds.fundGetFund(fund.id.toString(), { overrideErrorHandler: true })
                .catch(() => { return undefined; })))
                .then((responses) => {
                    const fundDetails: FundDetail[] = responses.filter((response) => response != undefined).map((response) => response.data);
                    setFundDetails(fundDetails);
                });
        }
    }, [])

    useEffect(() => {
      (async function(){
        const { data } = await Api.tasks.tasksGetMyTasks();

        setTasks(data);
      })()
    }, [userDetail.id])

    const activeTasks = tasks.filter(({status}) => status === TasksStatus.New)

    function handleAddFund() {
        const initData = {};
        if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
            initData['fundAdmins'] = [{ id: 'default', user: userDetail }];
        }
        WebApi.getAllScopes().then((scopes) => {
            dispatch(
                modalDialogShow(
                    this,
                    intl.formatMessage(messages.addFundTitle),
                    <FundForm
                        create={true}
                        initialValues={initData}
                        scopeList={scopes}
                        onSubmitForm={(data) => {
                            return dispatch(createFund(data));
                        }}
                    />,
                ),
            );
        });
    }

    function buildRibbon() {
        return <HomePageRibbon onAddFund={handleAddFund} />
    }

    function renderHistory() {
        const registryItems = registryRegionFront
            .filter(({ data }) => data)
            .map(({ data }) => {
                return <EntityItem entity={data} />;
            });

        const arrItems = [];
        arrRegionFront.forEach(({ activeVersion, id }) => {
            const item = fundDetails.find((fund) => fund.id === id);
            if (item) {
                arrItems.push(<FundItem fundDetail={item} version={activeVersion} />);
            }
        })

        if (arrItems.length === 0) {
            arrItems.push(
                renderMessage(
                    <FormattedMessage {...messages.recentFundsEmptyTitle} />,
                    <FormattedMessage {...messages.recentFundsEmptyMessage} />,
                ),
            );
        }

        if (registryItems.length === 0) {
            registryItems.push(
                renderMessage(
                    <FormattedMessage {...messages.recentEntitiesEmptyTitle} />,
                    <FormattedMessage {...messages.recentEntitiesEmptyMessage} />,
                ),
            );
        }

        return (
            <div className="history-list-container">
                <div className="button-container">
                    {
                        userDetail.hasOne(perms.FUND_RD, perms.FUND_RD_ALL)
                        && <>
                            <h4><FormattedMessage {...messages.recentFundsTitle} /></h4>
                            <div className="section">{arrItems}</div>
                        </>
                    }
                    <h4><FormattedMessage {...messages.recentEntitiesTitle} /></h4>
                    <div className="section">{registryItems}</div>
                </div>
            </div>
        );
    }

    /**
     * Vykreslení informace o prázné historii
     */
    function renderMessage(title: ReactNode, message: ReactNode) {
        return <div key="blank" className="unselected-msg history-list-item no-history">
            <div className="title">{title}</div>
            <div className="message">{message}</div>
        </div>
    }

    function getEntityLink(entityId: number, entityType: TasksEntityType) {
        switch (entityType) {
            case TasksEntityType.Ap:
                return urlEntity(entityId);
            case TasksEntityType.ApRev:
                return urlEntityRevision(entityId);
            default:
                return undefined;
        }
    }

    const centerPanel = (
        <div className="splitter-home">
            {renderHistory()}
        {activeTasks.length > 0 && <div className="history-list-container" style={{ height: "auto", flexShrink: 0, maxHeight: "40%" }}>
          <div className="button-container">
            <h4><FormattedMessage {...messages.tasksTitle} /></h4>
            <div style={{padding: "10px"}}>
              {activeTasks.map((task) => {
                const link = getEntityLink(task.primaryEntityId, task.primaryEntityType);
                const taskType = taskTypeMessages[task.taskTypeCode as keyof typeof taskTypeMessages];
                return <div key={task.taskId}>
                  {taskType ? <FormattedMessage {...taskType} /> : task.taskTypeName} -
                  &nbsp;
                  {link ? <Link to={link}>{task.primaryEntityName}</Link> : <>{task.primaryEntityName}</>}
                  &nbsp;
                  <i>{task.description}({task.creatorName})</i>
                </div>
              })}
            </div>
          </div>
        </div>}
            <div className='stats-container'>
                <StatsHome />
            </div>
        </div>
    )

    return <PageLayout
        ribbon={buildRibbon()}
        centerPanel={centerPanel}
    />;
}
