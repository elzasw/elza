import { Api } from 'api';
import { i18n } from 'components';
import { AdminInfo, LoggedUser } from 'elza-api';
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { urlAdminUser } from '../../../../src/constants';
import './Statistics.scss';

type LoggedUserWithCount = LoggedUser & { count: number };

const getHorizontalListItems = <T extends Object>(
    selectedStats: (keyof T)[],
    data: T,
    getValue: (value: T[keyof T]) => string = (value) => {
        if (typeof value !== "string" && typeof value !== "number") {
            console.warn("Value is not string or number.")
            return null;
        }
        return value.toString();
    },
): HorizontalListItem[] => {
    const listItems = selectedStats
        .map(stat => {
            const value = getValue(data[stat]);
            if (value != undefined) {
                return {
                    title: i18n(`stats.${stat.toString()}.title`),
                    value,
                } as HorizontalListItem;
            }
            return null;
        })
        .filter(item => item !== null);
    return listItems;
};

export const StatsHome = () => {
    const [stats, setStats] = useState<HorizontalListItem[]>([]);
    const selectedStats: (keyof AdminInfo)[] = ['funds', 'levels', 'accessPoints'];

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItems: HorizontalListItem[] = getHorizontalListItems(selectedStats, data);
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
    const [statsGeneral, setStatsGeneral] = useState<HorizontalListItem[]>([]);
    const [statsUsers, setStatsUsers] = useState<HorizontalListItem[]>([]);

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            const arrItemsGeneral: HorizontalListItem[] = getHorizontalListItems(
                ['funds', 'levels', 'accessPoints'],
                data,
            );
            const arrItemsUsers: HorizontalListItem[] = getHorizontalListItems(
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

interface HorizontalListItem {
    title: string;
    value: number | string;
}

interface HorizontalListProps {
    items: HorizontalListItem[];
}

export const HorizontalList = ({ items }: HorizontalListProps) => {
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

    const uniqueUsers: LoggedUserWithCount[] = users.reduce((result: LoggedUserWithCount[], user) => {
        const index = result.findIndex(_user => _user.user === user.user);

        if (index !== -1) {
            result[index].count++;
        } else {
            result.push({ ...user, count: 1 });
        }

        return result;
    }, []);

    return (
        <div className="stats-box" style={{ paddingTop: 0 }}>
            <div>
                <h6>{i18n('loggedUsers.title')}</h6>
                <div className="users">
                    {uniqueUsers.map((user, index) =>
                        user?.userId != undefined ? (
                            <Link key={index} to={urlAdminUser(user.userId)}>
                                <span>{user.user}</span>
                                {user.count > 1 ? ` (${user.count})` : ''}
                            </Link>
                        ) : (
                            <div key={index}>
                                <span>{user.user}</span>
                                {user.count > 1 ? ` (${user.count})` : ''}
                            </div>
                        ),
                    )}
                </div>
            </div>
        </div>
    );
};

interface HorizontalListWithUsersProps {
    statsUsers: HorizontalListItem[];
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
