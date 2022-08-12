import { useDispatch } from 'react-redux';
import { AnyAction } from 'redux';
import { ThunkDispatch } from 'redux-thunk';

export const useThunkDispatch = <State,>():ThunkDispatch<State, void, AnyAction> => useDispatch()
