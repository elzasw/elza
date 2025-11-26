import { useDispatch } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';
import { AppState } from 'typings/store';

export const useThunkDispatch = <State,>(): ThunkDispatch<State, void, AnyAction> => useDispatch()
export const useAppThunkDispatch = (): ThunkDispatch<AppState, void, AnyAction> => useDispatch()
