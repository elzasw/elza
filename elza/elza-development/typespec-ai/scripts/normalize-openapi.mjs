// Post-emit normalization of the OpenAPI the TypeSpec emitter produces.
//
// TypeSpec models the AiObject discriminated union by having each subtype
// redeclare the discriminator property with a single-value enum
// (`objectType: { type: string, enum: [elza.text] }`). openapi-generator turns
// that into a per-subtype enum type whose `getObjectType()` cannot override the
// `String` discriminator on the AiObject base — a compile error in the
// generated Java on BOTH sides (the elza-ai-provider server and the elza-core
// client). The base already carries the discriminator plus an explicit mapping,
// so the child redeclaration is redundant: strip it and let the subtypes inherit
// `objectType` via their `allOf` reference to AiObject.
//
// This runs after `tsp compile` (see package.json), rewriting the single
// committed OpenAPI document in elza-core that every consumer copies.
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const target = resolve(
  process.cwd(),
  "../../elza-core/src/main/resources/ai/elza-ai-provider.openapi.yaml"
);

const before = readFileSync(target, "utf8");
// Matches only child subtypes: the AiObject base declares objectType as
// `allOf: [$ref ObjectType]`, never `type: string` + `enum`.
const after = before.replace(
  /\n {8}objectType:\n {10}type: string\n {10}enum:\n {12}- elza\.[A-Za-z]+/g,
  ""
);

if (after !== before) {
  writeFileSync(target, after, "utf8");
  console.log(`normalize-openapi: stripped redundant child discriminator enums in ${target}`);
} else {
  console.log("normalize-openapi: nothing to strip (already normalized)");
}
