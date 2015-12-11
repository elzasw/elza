import {Utils} from 'components'
import {WebApi} from 'actions'

import {globalActions} from './global/actions'
import {faActions} from './fa/actions'
import {partyActions} from './party/actions'
import {recordActions} from './record/actions'

export const AppActions = {
    globalActions: globalActions,
    faActions: faActions,
    partyActions: partyActions,
    recordActions: recordActions,
}