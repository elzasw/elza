import { UserDetail } from 'typings/store/UserDetail.types';

export interface UserContextValue {
    userDetail: UserDetail;
    fetched: boolean;
    fetching: boolean;
}
