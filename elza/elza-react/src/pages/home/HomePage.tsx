import { createFund } from 'actions/arr/fund';
import { modalDialogShow } from 'actions/global/modalDialog';
import * as perms from 'actions/user/Permission';
import { Api } from 'api';
import { FundForm, i18n } from 'components';
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

export default function HomePage() {
    const [fundDetails, setFundDetails] = useState<FundDetail[]>([]);
    const [tasks, setTasks] = useState<TasksViewDetail[]>([]);

    const { arrRegionFront, registryRegionFront } = useSelector(({ stateRegion }: AppState) => stateRegion);
    const userDetail = useSelector(({ userDetail }: AppState) => userDetail);
    const splitter = useSelector(({ splitter }: AppState) => splitter);
    const dispatch = useThunkDispatch();

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
                    i18n('arr.fund.title.add'),
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
                    i18n('home.recent.fund.emptyList.title'),
                    i18n('home.recent.fund.emptyList.message'),
                ),
            );
        }

        if (registryItems.length === 0) {
            registryItems.push(
                renderMessage(
                    i18n('home.recent.registry.emptyList.title'),
                    i18n('home.recent.registry.emptyList.message'),
                ),
            );
        }

        return (
            <div className="history-list-container">
                <div className="button-container">
                    {
                        userDetail.hasOne(perms.FUND_RD, perms.FUND_RD_ALL)
                        && <>
                            <h4>{i18n('home.recent.fund.title')}</h4>
                            <div className="section">{arrItems}</div>
                        </>
                    }
                    <h4>{i18n('home.recent.registry.title')}</h4>
                    <div className="section">{registryItems}</div>
                </div>
            </div>
        );
    }

    /**
     * Vykreslení informace o prázné historii
     */
    function renderMessage(title: string, message: string) {
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
            <h4>Úkoly</h4>
            <div style={{padding: "10px"}}>
              {activeTasks.map((task) => {
                const link = getEntityLink(task.primaryEntityId, task.primaryEntityType);
                return <div>
                  {task.taskTypeName} -
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
        splitter={splitter}
        ribbon={buildRibbon()}
        centerPanel={centerPanel}
    />;
}
