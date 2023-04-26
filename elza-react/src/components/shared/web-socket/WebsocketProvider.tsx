import React, {PropsWithChildren, createContext, useContext, ReactNode} from 'react';
import { websocket } from 'websocketActions';

interface Props {
    url: string;
    eventMap: Record<string, (message: any) => void>;
}

const WebsocketContext = createContext<websocket | undefined>(undefined)

export const useWebsocket = () => {
    const websocketContext = useContext(WebsocketContext);
    if(!websocketContext){
        throw Error("Websocket context missing.")
    }
    return websocketContext;
}

export const WebsocketProvider = ({children}: PropsWithChildren<Props>) => {
    return <WebsocketContext.Provider value={(window as any).ws}>
        {children}
    </WebsocketContext.Provider>
}

export const WebsocketConsumer = ({children}:{children: (context: {websocket: websocket | undefined}) => ReactNode}) => {
    return <WebsocketContext.Consumer>{(websocket) => {
        return children({websocket});
    }}</WebsocketContext.Consumer>
}
