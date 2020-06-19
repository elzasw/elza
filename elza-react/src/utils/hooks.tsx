import {DependencyList, EffectCallback, useEffect, useRef} from "react";

export const useDebouncedEffect = (
    effect: EffectCallback,
    delay: number,
    deps: DependencyList = [],
    force?: boolean
) => {
    const lastRan = useRef(Date.now());

    useEffect(
        () => {
            if (!force) {
                const handler = setTimeout(function () {
                    if (Date.now() - lastRan.current >= delay) {
                        effect();
                        lastRan.current = Date.now();
                    }
                }, delay - (Date.now() - lastRan.current));

                return () => {
                    clearTimeout(handler);
                };
            } else {
                effect();
            }
        },
        [delay, ...deps],
    );
};
