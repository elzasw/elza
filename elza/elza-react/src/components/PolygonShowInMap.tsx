import * as React from 'react';
import {useEffect, useRef} from 'react';
import CrossTabHelper, { CrossTabEventType, getThisLayout } from './CrossTabHelper';
import { Button } from './ui';
import { i18n, Icon, TooltipTrigger } from 'components/shared';
import { MAP_URL } from '../pages/map/MapPage';
import classNames from 'classnames';
import { useThunkDispatch } from 'utils/hooks';
import { useSelector } from 'react-redux';
import { AppState } from 'typings/store';
import { GisSystemType } from '../constants';
import { kmlExtSystemListFetchIfNeeded } from 'actions/admin/kmlExtSystemList';
import { editInMapEditor } from 'components/registry/part-edit/form/fields/FormCoordinates';

const IFRAME_SIZE = 400;

type PolygonTooltipProps = {
    polygon: string;
};

export const PolygonTooltip = ({
    polygon
}: PolygonTooltipProps) => {
    const iframeRef = useRef<HTMLIFrameElement>(null);
    const dispatch = useThunkDispatch();
    const geoViewExternalSystems = useSelector((state: AppState) => {
        return state.app.kmlExtSystemList.rows.filter((extSystem: any) => {
            if (extSystem.type === GisSystemType.FrameApiView) {
                return true;
            }
        })
    });

    useEffect(() => {
        dispatch(kmlExtSystemListFetchIfNeeded()); // TODO - request only gis ext systems
    }, [])

    useEffect(() => {
        const receiveMessage = (e: MessageEvent) => {
            const { current: iframe } = iframeRef;
            if(e.data?.event === "elza-map-iframe-load"){
                if(iframe && iframe.contentWindow){
                    iframe?.contentWindow?.postMessage({
                        call: 'sendPolygon',
                        polygon: polygon,
                    }, "*")
                }
            }
            else if (e.data?.type === "MapViewInit" && geoViewExternalSystems.length === 1 && geoViewExternalSystems[0].url) {
                console.log("#### init message received", e.data)
                if (iframe && iframe.contentWindow) {
                    iframe?.contentWindow?.postMessage({
                        type: 'MapGeometry',
                        geometry: polygon,
                        requestId: 'test',
                        geometryFormat: "WKT",
                        initialRegion: polygon,
                    }, geoViewExternalSystems[0].url)
                }
            }
            else if (e.data?.type === "MapError") {
                throw Error(e.data.message);
            }
        }

        window.addEventListener("message", receiveMessage);

        return () => {
            window.removeEventListener("message", receiveMessage);
        }
    }, [polygon])

    let mapViewUrl = MAP_URL + '?iframe=1'; // adresa vestavene implementace v Elza

    if (geoViewExternalSystems.length === 1) {
        const { url, apiKeyValue } = geoViewExternalSystems[0];
        const queryStartIndex = url?.indexOf("?");
        const baseUrl = queryStartIndex != undefined && queryStartIndex >= 0 ? url?.substring(0, queryStartIndex) : url;
        const queryString = queryStartIndex != undefined && queryStartIndex >= 0 ? url?.substring(queryStartIndex + 1) : undefined;
        const query = new URLSearchParams(queryString);

        query.set("SOURCE_ORIGIN", window.location.origin);
        query.set("MODE", "VIEW");

        if (apiKeyValue) {
            query.set("API_KEY", apiKeyValue);
        }

        mapViewUrl = `${baseUrl}?${query}`;
    }

    return (<><strong className={'d-block py-1'}>{i18n('global.action.showInMap')}</strong>
        <iframe title="elza-map-iframe" className={'border-0 float-left'} ref={iframeRef} style={{
            height: IFRAME_SIZE,
            width: IFRAME_SIZE,
        }}
            src={mapViewUrl}
        />
    </>);
}


interface ChildrenRenderProps {
    handleShowInMap: () => void;
}

interface Props extends React.ComponentPropsWithoutRef<"span">{
    className?: string;
    polygon: string;
    showInEditor?: boolean;
    onEditorSave?: (value: string) => void;
    children?: (props: ChildrenRenderProps) => React.ReactNode;
};

/**
* Componenta s odkazem na mapu s polygonem
*/
export const PolygonShowInMap = ({
    children,
    polygon,
    className,
    showInEditor = false,
    onEditorSave = () => {return;},
    ...otherProps
}: Props) => {
    const dispatch = useThunkDispatch();
    const geoEditExternalSystems = useSelector((state: AppState) => {
        return state.app.kmlExtSystemList.rows.filter((extSystem: any) => {
            if (extSystem.type === GisSystemType.FrameApiEdit) {
                return true;
            }
        })
    });

    useEffect(() => {
        dispatch(kmlExtSystemListFetchIfNeeded());
    }, [])

    const handleEditInMap = async () => {
        if (geoEditExternalSystems.length === 1) {
            const value = await dispatch(editInMapEditor(polygon, geoEditExternalSystems[0]));
            if(value !== polygon){
                onEditorSave(value);
            }
        } else {
            throw Error("Missing or multiple GIS editing external systems. Has to be exactly one.")
        }
    }

    const showInMap = () => {
        if(showInEditor){
            handleEditInMap();
        } else {
            const thisLayout = getThisLayout();

            if (thisLayout) {
                CrossTabHelper.sendEvent(
                    thisLayout,
                    {
                        type: CrossTabEventType.SHOW_IN_MAP,
                        data: polygon
                    }
                );
            }
        }
    }

    return (
        <TooltipTrigger
            {...otherProps}
            content={polygon ? <PolygonTooltip polygon={polygon} /> : undefined}
            holdOnHover
            placement={'vertical'}
            showDelay={300}
            hideDelay={0}
        >
            {
            children?.({handleShowInMap: () => showInMap()})
                || <Button className={classNames(className)} onClick={() => showInMap()} variant={'action' as any}>
                    <Icon glyph={'fa-map'} />
                </Button>
        }
        </TooltipTrigger>
    );
}
