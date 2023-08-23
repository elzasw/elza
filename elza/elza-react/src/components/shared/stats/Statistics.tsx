import { Api } from 'api';
import { i18n } from 'components';
import { AdminInfo, LoggedUser } from 'elza-api';
import React, { useEffect, useState } from 'react';
import './Statistics.scss';

const getHorizontalListItems = <T extends Object>(selectedStats: (keyof T)[], data: T): HorizontalListItem<T>[] => {
    const listItems: HorizontalListItem<T>[] = selectedStats
        .map(stat => {
            const value = data[stat];
            if (value != undefined) {
                return {
                    title: i18n(`stats.${stat.toString()}.title`),
                    value,
                };
            }
            return null;
        })
        .filter(item => item !== null) as HorizontalListItem<T>[];
    return listItems;
};

export const StatsHome = () => {
    const [stats, setStats] = useState<HorizontalListItem<AdminInfo>[]>([]);
    const selectedStats: (keyof AdminInfo)[] = ['funds', 'levels', 'accessPoints'];

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItems: HorizontalListItem<AdminInfo>[] = getHorizontalListItems(selectedStats, data);
            setStats(arrItems);
        });
    }, []);

    return (
        <div className="stats-flex-container">
            <HorizontalList items={stats} />
        </div>
    );
};

export const StatsAdmin = () => {
    const [loggedUsers, setLoggedUsers] = useState<LoggedUser[]>([]);
    const [statsGeneral, setStatsGeneral] = useState<HorizontalListItem<AdminInfo>[]>([]);
    const [statsUsers, setStatsUsers] = useState<HorizontalListItem<AdminInfo>[]>([]);

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItemsGeneral: HorizontalListItem<AdminInfo>[] = getHorizontalListItems(
                ['funds', 'levels', 'accessPoints'],
                data,
            );
            const arrItemsUsers: HorizontalListItem<AdminInfo>[] = getHorizontalListItems(
                ['users', 'loggedUsers'],
                data,
            );
            setStatsGeneral(arrItemsGeneral);
            setStatsUsers(arrItemsUsers);
        });
        Api.admin.adminLoggedUsers({ overrideErrorHandler: true }).then(({ data: _loggedUsers }) => {
            setLoggedUsers(_loggedUsers?.users || []);
        });
    }, []);

    return (
        <>
            <div style={{ margin: '10px' }}>
                <HorizontalList items={statsGeneral} />
            </div>
            <HorizontalListWithUsers statsUsers={statsUsers} loggedUsers={loggedUsers} />
        </>
    );
};

interface HorizontalListItem<T extends Object> {
    title: string;
    value: T[keyof T];
}

interface HorizontalListProps<T extends Object> {
    items: HorizontalListItem<T>[];
}

export const HorizontalList = <T extends Object>({ items }: HorizontalListProps<T>) => {
    if (items.length === 0) {
        return <></>;
    }
    return (
        <div className="stats-box">
            {items.map((item, index) => (
                <div key={index}>
                    <span>{item.title}:</span> {item.value}
                </div>
            ))}
        </div>
    );
};

interface LoggedUsersListProps {
    users: LoggedUser[];
}

export const LoggedUsersList = ({ users }: LoggedUsersListProps) => {
    if (users.length === 0) {
        return <></>;
    }
    return (
        <div className="stats-box" style={{ paddingTop: 0 }}>
            <div>
                <h6>{i18n('loggedUsers.title')}</h6>
                <div className="users">
                    {users.map((user, index) => (
                        <div key={index}>
                            <span>{user.user}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

interface HorizontalListWithUsersProps {
    statsUsers: HorizontalListItem<AdminInfo>[];
    loggedUsers: LoggedUser[];
}

export const HorizontalListWithUsers = ({ statsUsers, loggedUsers }: HorizontalListWithUsersProps) => {
    return (
        <div className="stats-user-box">
            <div className="background-box">
                <HorizontalList items={statsUsers} />
                <LoggedUsersList users={loggedUsers} />
            </div>
        </div>
    );
};
