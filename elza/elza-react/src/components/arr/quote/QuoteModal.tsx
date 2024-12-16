import { i18n } from 'components/shared';
import { Button } from 'components/ui';
import { useEffect, useState } from 'react';
import { Modal } from 'react-bootstrap';
import { FormattedMessage } from 'react-intl';
import { Api } from 'api';
import { NodePlainTextRepresentation } from 'elza-api';

interface Props {
    nodeId: number;
    versionId: number;
    onClose: () => void;
}

export function QuoteModal({ nodeId, versionId, onClose }: Props) {
    const [quotes, setQuotes] = useState<NodePlainTextRepresentation[]>([]);

    useEffect(() => {
        (async () => {
            const { data } = await Api.node.nodeGetPlainText(versionId, nodeId);
            setQuotes(data);
        })()
    }, [nodeId, versionId])

    return (
        <>
            <Modal.Body>
                {quotes.map(({ name, code, value }) => {
                    return <div style={{ marginBottom: "16px" }}>
                        <div><b><FormattedMessage id={`arr_quote_title_${code}`} defaultMessage={name} /></b></div>
                        <p>{value}</p>
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
