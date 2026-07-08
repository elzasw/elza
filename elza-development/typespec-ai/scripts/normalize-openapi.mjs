// Post-emit normalization of the OpenAPI the TypeSpec emitter produces.
//
// TypeSpec emits each AiObject subtype as `type: object` with top-level
// `properties`/`required` AND an `allOf: [$ref AiObject]`, redeclaring the
// discriminator as an inline single-value enum. Two generators choke on that:
//   - spring (elza-ai-provider): the inline enum becomes a per-subtype enum
//     whose getObjectType() cannot override the String discriminator on the
//     base — a compile error.
//   - java/okhttp-gson (elza-core client): a `$ref` property declared alongside
//     `allOf` is dropped from the subtype's `openapiFields`, so its strict
//     validateJsonElement rejects `data` when deserializing responses.
//
// Rewrite each subtype into the pure-allOf shape the CAM client already uses
// successfully with the same generators: own properties live in a SECOND allOf
// member and the discriminator stays only on the base (AiObject):
//
//     MarkdownObject:
//       type: object
//       allOf:
//         - $ref: '#/components/schemas/AiObject'
//         - type: object
//           required: [data]
//           properties:
//             data: { $ref: '#/components/schemas/MarkdownPayload' }
//
// Runs after `tsp compile` (see package.json), rewriting the single committed
// OpenAPI document in elza-core that every consumer copies.
import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const target = resolve(
  process.cwd(),
  "../../elza-core/src/main/resources/ai/elza-ai-provider.openapi.yaml"
);

const before = readFileSync(target, "utf8");

// Each subtype emits as: required[objectType,data] + properties{objectType(enum),
// data($ref)} + allOf[AiObject]. Collapse to pure allOf, dropping the redundant
// discriminator redeclaration and moving `data` into the allOf member.
const after = before.replace(
  /\n {6}required:\n {8}- objectType\n {8}- data\n {6}properties:\n {8}objectType:\n {10}type: string\n {10}enum:\n {12}- elza\.[A-Za-z]+\n {8}data:\n {10}(\$ref: '[^']+')\n {6}allOf:\n {8}- \$ref: '#\/components\/schemas\/AiObject'/g,
  "\n      allOf:\n        - $ref: '#/components/schemas/AiObject'\n        - type: object\n          required:\n            - data\n          properties:\n            data:\n              $1"
);

if (after !== before) {
  writeFileSync(target, after, "utf8");
  console.log(`normalize-openapi: rewrote AiObject subtypes to pure-allOf in ${target}`);
} else {
  console.log("normalize-openapi: nothing to rewrite (already normalized)");
}
