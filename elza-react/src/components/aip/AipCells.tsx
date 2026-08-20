import { Icon } from 'components/shared';
import { LinkedNodeVO } from 'elza-api';
import { Button } from "react-bootstrap";
import { serverContextPath } from "../../api";

export const getBoolIcon = (value?: boolean) => {
    return value ? <Icon glyph="fa-check"/> : <Icon glyph="fa-close"/>;
}

export const getConnectedToJP = (
    linkedNodes: Array<LinkedNodeVO> | null,
    fundId: number,
    handleDeleteLink: (linkId: number) => void,
) => {
    let iconString = "fa fa-close";
    let nodes;

    if (linkedNodes) {
        iconString = "fa fa-check";

        nodes = linkedNodes.map(item =>
            <div key={item.id}>
                <a href={`${serverContextPath}/fund/${fundId}/node/${item.nodeId}`}>{item.name}</a>
                <Button key="deleteLink" variant="action" onClick={() => handleDeleteLink(item.id)}>
                    <Icon glyph="fa fa-close" />
                </Button>
            </div>)
    }

    return <div><Icon glyph={iconString}/> {nodes}</div>;
}
