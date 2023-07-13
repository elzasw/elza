import React, { PropsWithChildren } from "react"
import Icon from "../icon/Icon"
import "./WaitingOverlay.scss"

export const WaitingOverlay = ({
    children,
}: PropsWithChildren<{}>) => {
    return <div className="waiting-overlay">
        <div className="waiting-icon">
            <Icon glyph="fa-spin fa-circle-o-notch" />
        </div>
        <div>
            {children}
        </div>
    </div>
}