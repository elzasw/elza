import { Api } from 'api';
import { FormattedMessage, defineMessages } from 'react-intl';
import { AdminInfo, LoggedUser } from 'elza-api';
import React, { ReactNode, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { urlAdminUser } from '../../../../src/constants';
import './Statistics.scss';
import { AppState } from 'typings/store';
import { useSelector } from 'react-redux';
import * as perms from 'actions/user/Permission.jsx';

type LoggedUserWithCount = LoggedUser & { count: number };

/**
 * Popisky dlaždic. Id jsou převzatá z legacy katalogu beze změny.
 *
 * Klíč se dřív skládal za běhu (`stats.${stat}.title`), což statický extraktor
 * nevidí - klíče by se do katalogu vůbec nedostaly. Množina je uzavřená, takže
 * stačí ji vypsat a indexovat přímo.
 */
const statMessages = defineMessages({
    funds: {
        id: 'stats.funds.title',
        defaultMessage: 'Počet AS',
    },
    levels: {
        id: 'stats.levels.title',
        defaultMessage: 'Počet JP',
    },
    accessPoints: {
        id: 'stats.accessPoints.title',
        defaultMessage: 'Počet arch. entit',
    },
    users: {
        id: 'stats.users.title',
        defaultMessage: 'Počet uživatelů',
    },
    loggedUsers: {
        id: 'stats.loggedUsers.title',
        defaultMessage: 'Počet aktivních relací',
    },
});

const messages = defineMessages({
    loggedUsersTitle: {
        id: 'loggedUsers.title',
        defaultMessage: 'Přihlášení uživatelé',
    },
});

/** Statistika, pro kterou existuje popisek. */
type StatKey = keyof typeof statMessages;

/**
 * `keyof T & StatKey` dělá z chybějícího popisku chybu překladu: přibude-li do
 * AdminInfo další údaj, nejde ho sem předat, dokud nemá deskriptor.
 */
const getHorizontalListItems = <T extends object>(
    selectedStats: (keyof T & StatKey)[],
    data: T,
    getValue: (value: T[keyof T]) => string | null = (value) => {
        if (typeof value !== "string" && typeof value !== "number") {
            console.warn("Value is not string or number.")
            return null;
        }
        return value.toString();
    },
): HorizontalListItem[] =>
    selectedStats
        .map((stat): HorizontalListItem | null => {
            const value = getValue(data[stat]);
            return value == undefined
                ? null
                : { title: <FormattedMessage {...statMessages[stat]} />, value };
        })
        .filter((item): item is HorizontalListItem => item !== null);

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

    const userDetail = useSelector(({userDetail}:AppState) => userDetail);

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
        if(userDetail.hasOne(perms.USER_CONTROL_ENTITY, perms.GROUP_CONTROL_ENTITY)){
            Api.admin.adminLoggedUsers({ overrideErrorHandler: true }).then(({ data: _loggedUsers }) => {
                setLoggedUsers(_loggedUsers?.users || []);
            });
        }
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
    /** Uzel, ne řetězec: popisek je <FormattedMessage>, aby se přepnul s jazykem. */
    title: ReactNode;
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
                <h6><FormattedMessage {...messages.loggedUsersTitle} /></h6>
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
