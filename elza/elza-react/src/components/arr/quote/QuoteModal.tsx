import { Icon, NoFocusButton, TooltipTrigger, i18n } from 'components/shared';
import { Button } from 'components/ui';
import { useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { FormattedMessage, useIntl } from 'react-intl';
import { Api } from 'api';
import { NodePlainTextRepresentation } from 'elza-api';
import { addToastrDanger, addToastrInfo } from 'components/shared/toastr/ToastrActions';
import { useThunkDispatch } from 'utils/hooks';
import { globalMessages } from 'components/shared/lang';

interface Props {
    nodeId: number;
    versionId: number;
    onClose?: () => void;
}

export function QuoteModal({ nodeId, versionId, onClose }: Props) {
    const [quotes, setQuotes] = useState<NodePlainTextRepresentation[]>([]);
    const dispatch = useThunkDispatch();
    const {formatMessage} = useIntl();

    useEffect(() => {
        (async () => {
            const { data } = await Api.node.nodeGetPlainText(versionId, nodeId);
            setQuotes(data);
        })()
    }, [nodeId, versionId])

    const copyToClipboard = (string: string) => {
        if(navigator.clipboard){
            navigator.clipboard.writeText(string);
            dispatch(addToastrInfo(formatMessage({...globalMessages.copyToClipboardFinished})));
        } else {
            dispatch(addToastrDanger(formatMessage({...globalMessages.copyToClipboardUnavailable})));
        }
    };

    return (
        <>
            <Modal.Body>
                {quotes.map(({ name, code, value }) => {
                    return <div style={{ marginBottom: "16px" }}>
                        <div style={{display: "flex", alignItems: "center"}}>
                            <b><FormattedMessage id={`arr_quote_title_${code}`} defaultMessage={name} /></b>
                            &nbsp;
                            <TooltipTrigger placement="top" content={formatMessage({...globalMessages.copyToClipboard})}>
                                <NoFocusButton onClick={() => copyToClipboard(value)}>
                                    <Icon glyph="fa-clone"/>
                                </NoFocusButton>
                            </TooltipTrigger>
                        </div>
                        <p style={{wordWrap: "break-word"}}>{value}</p>
                    </div>
                })}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={onClose}>
                    {i18n('global.action.close')}
                </Button>
            </Modal.Footer>
        </>
    );
}
