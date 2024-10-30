import { Loading } from "components/shared";
import { FC, useEffect, useRef, useState } from "react";
import { serverContextPath } from 'api';

export const COMPONENT_URL = `/component`;
export const COMPONENT_URL_WITH_CONTEXT = `${serverContextPath}${COMPONENT_URL}`;

interface ComponentPageProps {
	componentViewRequest: {
		request: ViewRequest,
		viewUrl: string,
	}
}

type ViewRequest = {
    type: string;
    requestId: string;
    daoId: string;
    entityRef: string;
}

const ComponentPage: FC<ComponentPageProps> = ({componentViewRequest}) => {
	const [initialized, setInitialized] = useState<boolean>(false);
    const iframeRef = useRef();

    useEffect(() => {
		window.addEventListener('message', handleMessage, false);
    }, []);

	useEffect(() => {
		if(iframeRef.current && initialized) {
			sendViewRequest();
		}
	}, [componentViewRequest])

    const sendViewRequest = () => {
		if(iframeRef.current) {
			//@ts-ignore
			iframeRef.current.contentWindow.postMessage(componentViewRequest.request, componentViewRequest.viewUrl);
		}
	};

    const handleMessage = (event: any) => {
        if (!componentViewRequest?.viewUrl || event.origin !== new URL(componentViewRequest.viewUrl).origin) {
          return; 
        }
  
        const data = event.data;
  
        switch (data.type) {
          case 'ViewInit':
			setInitialized(true);
            sendViewRequest();
            break;
          case 'ActiveView':
			// Asi nic?
            break;
          case 'ViewError':
            //TODO: nějak vyhodit error
            break;
          default:
            console.warn('Unknown message type:', data.type);
        }
    };
	if(!componentViewRequest) {
		return <Loading />;
	}

    return (
		<iframe
			ref={iframeRef}
			src={`${componentViewRequest.viewUrl}?SOURCE_ORIGIN=${encodeURIComponent(window.location.origin)}`}
			title="Prohlížeč komponent"
			style={{
				flexGrow: 1,
			}}
		/>
    );
};

export default ComponentPage;