import { LinkContainer } from 'react-router-bootstrap';
import { urlFundTree } from "../../constants"
import { truncateStringWithTooltip } from "./utils"
import { Button } from "components/ui"
import { Icon } from "components/shared"
import { FundDetail } from 'elza-api';
import { ActiveVersion } from 'typings/store';
import { FormattedDate, FormattedMessage, FormattedTime, defineMessages } from 'react-intl';

const messages = defineMessages({
    lockedVersion: { id: 'home.fund.lockedVersion', defaultMessage: 'Verze {date}' },
});

export interface Props {
  fundDetail: FundDetail;
  version: ActiveVersion;
}

export function FundItem({ fundDetail, version }) {
  const name = truncateStringWithTooltip(fundDetail.name, 90)

  return <LinkContainer to={urlFundTree(fundDetail.id, version.lockDate === null ? undefined : version.id)} className="history-list-item history-button">
    <Button>
      <div className="background-text-container">
        {/* <Icon glyph='fa-database' /> */}
        <div className="background-text">{fundDetail.name}</div>
      </div>
      <div className="fund-content">
        <div className="history-name">
          {name}
        </div>
        <div className="desc-container">
          <>
            <div className="fund-desc-container">
              {fundDetail.mark && <div className="fund-desc-item" >
                {fundDetail.mark}
              </div>}
              <div className="fund-desc-item version" >
                {version.lockDate && <>
                  <Icon glyph={'fa-lock'} />{' '}
                  <FormattedMessage
                    {...messages.lockedVersion}
                    values={{
                      date: <>
                        <FormattedDate value={version.lockDate} />{' '}
                        <FormattedTime value={version.lockDate} />
                      </>,
                    }}
                  />
                </>}
              </div>
            </div>
          </>
        </div>
        <div className="fund-label">
          {fundDetail.internalCode || fundDetail.fundNumber}
          {/* {[fundDetail.fundNumber, fundDetail.internalCode].filter((item) => item).join(", ")} */}
          &nbsp;
        </div>
      </div>
    </Button>
  </LinkContainer>
}

