import { PropsWithChildren } from "react";
import { useUserSettings } from "contexts/user";

export function ExperimentalFeature({ children }: PropsWithChildren) {
    const { settings } = useUserSettings();
    if (!settings.showExperimentalFeatures) return null;
    return <>{children}</>;
}
