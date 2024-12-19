package ZP2015

import cz.tacr.elza.groovy.GroovyGenCtx

return generate(GENERATOR_CONTEXT)

String generate(final GroovyGenCtx genCtx) {
    return String.format("Fund: %s, nodeIds: %s", genCtx.getFund().getName(), genCtx.getNodeIds())
}