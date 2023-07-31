import { Api } from 'api';
import { i18n } from 'components';
import { AdminInfo, LoggedUser } from 'elza-api';
import React, { useEffect, useState } from 'react';
import './Statistics.scss';

const getHorizontalListItems = <T extends keyof AdminInfo>(
    selectedStats: T[],
    data: AdminInfo,
): HorizontalListItem<T>[] => {
    const listItems: HorizontalListItem<T>[] = selectedStats
        .map(stat => {
            const value = data[stat];
            if (value != undefined) {
                return {
                    title: i18n(`stats.${stat}.title`),
                    value,
                };
            }
            return null;
        })
        .filter(item => item !== null) as HorizontalListItem<T>[];
    return listItems;
};

export const StatsHome = () => {
    const [stats, setStats] = useState<HorizontalListItem<keyof AdminInfo>[]>([]);
    const selectedStats: (keyof AdminInfo)[] = ['funds', 'levels', 'accessPoints'];

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItems: HorizontalListItem<keyof AdminInfo>[] = getHorizontalListItems(selectedStats, data);
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
    const [statsGeneral, setStatsGeneral] = useState<HorizontalListItem<keyof AdminInfo>[]>([]);
    const [statsUsers, setStatsUsers] = useState<HorizontalListItem<keyof AdminInfo>[]>([]);

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItemsGeneral: HorizontalListItem<keyof AdminInfo>[] = getHorizontalListItems(
                ['funds', 'levels', 'accessPoints'],
                data,
            );
            const arrItemsUsers: HorizontalListItem<keyof AdminInfo>[] = getHorizontalListItems(
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
            <div>
                <HorizontalList items={statsGeneral} />
            </div>
            <div>
                <div className="stats-user-box">
                    <HorizontalList items={statsUsers} />
                    <LoggedUsersList users={loggedUsers} />
                </div>
            </div>
        </>
    );
};

interface HorizontalListItem<T extends keyof AdminInfo> {
    title: string;
    value: AdminInfo[T];
}

interface HorizontalListProps<T extends keyof AdminInfo> {
    items: HorizontalListItem<T>[];
}

export const HorizontalList = <T extends keyof AdminInfo>({ items }: HorizontalListProps<T>) => {
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
        <div className="stats-user-list">
            <h5>{i18n('loggedUsers.title')}</h5>
            <div className="users">
                {users.map((user, index) => (
                    <div key={index}>
                        <span>{user.user}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};
