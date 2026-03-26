import { createContext, ReactNode } from 'react';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { UserContextValue } from './types';

export const UserContext = createContext<UserContextValue | null>(null);

interface Props {
    children: ReactNode;
}

export function UserProvider({ children }: Props) {
    const userDetail = useAppSelector(({ userDetail }) => userDetail);

    const value: UserContextValue = {
        userDetail,
        fetched: userDetail.fetched,
        fetching: userDetail.fetching,
    };

    return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}
