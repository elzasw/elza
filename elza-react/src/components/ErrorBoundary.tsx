import * as React from "react";

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
					<p>Something went wrong! Clear cache and refresh.</p>
					{`To continue: `}
					<a href="/" onClick={() => window.location.reload()}>
						Clear & reload
					</a>
				</div>
			);
		}

		return children;
	}
}
