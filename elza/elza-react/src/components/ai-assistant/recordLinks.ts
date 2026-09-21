import { urlEntity, urlFundTree, urlNode } from "../../constants";

/**
 * A record link target as the AI provider protocol writes it — `ap:<accessPointId>`
 * (an entity / access point), `node:<nodeId>` (a description level), `fund:<fundId>`
 * (a fund). The provider knows no routes of this installation, so an answer's
 * markdown refers to the archive's own records symbolically and the client maps
 * each reference onto its own route.
 */
const RECORD_REF = /^(ap|node|fund):(\d+)$/;

/** The in-app path of a record reference, or null when `href` is not one. */
export function recordLinkPath(href: string): string | null {
    const match = RECORD_REF.exec(href.trim());
    if (!match) return null;
    const id = Number(match[2]);
    switch (match[1]) {
        case "ap":
            return urlEntity(id);
        case "node":
            return urlNode(id);
        case "fund":
            return urlFundTree(id);
        default:
            return null;
    }
}

interface HastNodeLike {
    type: string;
    tagName?: string;
    properties?: Record<string, unknown>;
    children?: HastNodeLike[];
}

/**
 * Rehype plugin: rewrites record-reference hrefs (`ap:12` …) into in-app paths.
 * Must run BEFORE the sanitizer, whose default schema keeps relative URLs but
 * would strip an unknown `ap:` protocol — so a record link survives sanitizing
 * as an ordinary relative link, recognizable by its leading `/`.
 */
export function rehypeRecordLinks() {
    const visit = (node: HastNodeLike) => {
        if (node.type === "element" && node.tagName === "a" && typeof node.properties?.href === "string") {
            const path = recordLinkPath(node.properties.href);
            if (path) node.properties.href = path;
        }
        node.children?.forEach(visit);
    };
    return (tree: unknown) => visit(tree as HastNodeLike);
}
