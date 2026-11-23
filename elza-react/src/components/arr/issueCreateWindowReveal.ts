import type { IssueTarget } from './IssueCreateWindow';

type RevealListener = (target: IssueTarget) => void;

const revealListeners = new Set<RevealListener>();

/**
 * Asks the open comment window, if there is one, to switch to the target and come into view. Only one
 * window is open at a time, so a repeated "add comment" goes to the open window instead of a new one.
 */
export function requestIssueCreateWindowReveal(target: IssueTarget) {
    revealListeners.forEach((listener) => listener(target));
}

export function subscribeIssueCreateWindowReveal(listener: RevealListener) {
    revealListeners.add(listener);
    return () => {
        revealListeners.delete(listener);
    };
}
