import { LinkContainer } from 'react-router-bootstrap';
import { TooltipTrigger } from "components/shared";
import { Button } from "components/ui";
import { truncateStringWithTooltip } from './utils';
import { ApAccessPointVO } from 'api';

export interface Props {
  entity: ApAccessPointVO;
}

export function EntityItem({ entity }: Props) {
  return (
    <TooltipTrigger
      style={{ zIndex: 2, display: "inline-block" }}
      content={entity.description
        && <div style={{ maxWidth: "13em" }} >
          {entity.description}
        </div>}
      placement="vertical"
    >
      <LinkContainer
        to={`/entity/${entity.id}`}
        className="history-list-item history-button"
      >
        <Button>
          <div className="background-text-container">
            {/* <Icon glyph='fa-th-list' /> */}
            <div className="background-text">{entity.name}</div>
          </div>
          <div style={{ zIndex: 2 }} className="history-name">
            {entity.name && truncateStringWithTooltip(entity.name, 120)}
          </div>
        </Button>
      </LinkContainer>
    </TooltipTrigger>
  )
}

