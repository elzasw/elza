import { useSelector } from "react-redux";
import { TypedUseSelectorHook } from "react-redux";
import { AppState } from "typings/store";

export const useAppSelector: TypedUseSelectorHook<AppState> = useSelector;
