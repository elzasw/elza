import * as React from 'react';
import {useEffect, useRef} from 'react';
import CrossTabHelper, { CrossTabEventType, getThisLayout } from './CrossTabHelper';
import { Button } from './ui';
import { i18n, Icon, TooltipTrigger } from 'components/shared';
import { MAP_URL } from '../pages/map/MapPage';
import classNames from 'classnames';

const IFRAME_SIZE = 400;

type PolygonTooltipProps = { 
    polygon: string;
};

export const PolygonTooltip = ({
    polygon
}: PolygonTooltipProps) => {
    const iframeRef = useRef<HTMLIFrameElement>(null);

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
        }

        window.addEventListener("message", receiveMessage);
        
        return () => {
            window.removeEventListener("message", receiveMessage);
        }
    },[polygon])

    return (<><strong className={'d-block py-1'}>{i18n('global.action.showInMap')}</strong>
        <iframe title="elza-map-iframe" className={'border-0 float-left'} ref={iframeRef} style={{
            height: IFRAME_SIZE,
            width: IFRAME_SIZE,
        }} src={MAP_URL + '?iframe=1'} />
        </>);
}


interface ChildrenRenderProps {
    handleShowInMap: () => void;
}

interface Props extends React.ComponentPropsWithoutRef<"span">{ 
    className?: string; 
    polygon: string;
    children?: (props: ChildrenRenderProps) => React.ReactNode;
};

/**
* Componenta s odkazem na mapu s polygonem
*/
export const PolygonShowInMap = ({
    children,
    polygon,
    className,
    ...otherProps
}: Props) => {

    const showInMap = () => {
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

    return (
        <TooltipTrigger
            {...otherProps}
            content={<PolygonTooltip polygon={polygon} />}
            holdOnHover
            placement={'auto'}
            showDelay={200}
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
