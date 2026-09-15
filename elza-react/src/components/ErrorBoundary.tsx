import * as React from "react";
import { FormattedMessage, defineMessages } from 'react-intl';

const messages = defineMessages({
    failed: { id: 'app.error.boundary.message', defaultMessage: 'Došlo k chybě. Vymažte mezipaměť a načtěte stránku znovu.' },
    continueWith: { id: 'app.error.boundary.continue', defaultMessage: 'Pro pokračování: ' },
    clearAndReload: { id: 'app.error.boundary.reload', defaultMessage: 'Vymazat a načíst znovu' },
});

const MISSING_ERROR = "Error was swallowed during propagation.";

type HocProps = {
	children: any
};
type HocState = {
	readonly error: Error | null | undefined;
};

export class ErrorBoundary extends React.Component<HocProps, HocState> {

	readonly state: HocState = {
		error: undefined,
	};

	componentDidCatch(error: Error | null, info: object) {
		this.setState({ error: error || new Error(MISSING_ERROR) });
		this.logErrorToCloud(error, info);
	}

	logErrorToCloud = (error: Error | null, info: object) => {
		// :)
	};

	handleReset = () => {
		this.setState({ error: undefined });
	};

	render() {
		const { children } = this.props;
		const { error } = this.state;

		if (error) {
			return (
				<div>
					<p><FormattedMessage {...messages.failed} /></p>
					<FormattedMessage {...messages.continueWith} />
					<a href="/" onClick={() => window.location.reload()}>
						<FormattedMessage {...messages.clearAndReload} />
					</a>
				</div>
			);
		}

		return children;
	}
}
