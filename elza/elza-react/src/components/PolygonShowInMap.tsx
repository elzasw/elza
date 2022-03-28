import * as React from 'react';
import CrossTabHelper, { CrossTabEventType, getThisLayout } from './CrossTabHelper';
import { Button } from './ui';
import { i18n, Icon, TooltipTrigger } from 'components/shared';
import { MAP_URL } from '../pages/map/MapPage';
import classNames from 'classnames';

const IFRAME_SIZE = 400;

type PolygonTooltipProps = { polygon: string };

class PolygonTooltip extends React.Component<PolygonTooltipProps> {
    iframeRef;

    constructor(props) {
        super(props);

        this.iframeRef = React.createRef();
    }

    componentDidMount() {
        this.iframeRef.current.addEventListener('load', () => setTimeout(() => {
            if (this.iframeRef.current.contentWindow) {
                this.iframeRef.current.contentWindow.postMessage({
                    call: 'sendPolygon',
                    polygon: this.props.polygon,
                });
            }
        }), 750);
    }

    render() {
        return (<><strong className={'d-block py-1'}>{i18n('global.action.showInMap')}</strong>
            <iframe className={'border-0 float-left'} ref={this.iframeRef} style={{
                height: IFRAME_SIZE,
                width: IFRAME_SIZE,
            }} src={MAP_URL + '?iframe=1'} />
        </>);
    }
}

type Props = { className?: string; polygon: string };

/**
 * Componenta s odkazem na mapu s polygonem
 */
export class PolygonShowInMap extends React.Component<Props> {
    showInMap() {
        const thisLayout = getThisLayout();

        if (thisLayout) {
            CrossTabHelper.sendEvent(thisLayout, {type: CrossTabEventType.SHOW_IN_MAP, data: this.props.polygon});
        }
    }

    render() {

        return (
            <Button className={classNames('px-0', this.props.className)} onClick={() => this.showInMap()} variant={'action' as any}>
                <TooltipTrigger
                    className={'float-left h-100 px-1'}
                    content={<PolygonTooltip polygon={this.props.polygon} />}
                    holdOnHover
                    placement={'auto'}
                    showDelay={50}
                    hideDelay={0}
                >
                    <Icon glyph={'fa-map'} />
                </TooltipTrigger>
            </Button>
        );
    }
}
