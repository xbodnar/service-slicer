package cz.bodnor.serviceslicer.application.module.analysis.graph.ast

data class WeightedReference(
    var methodCalls: Int = 0,
    var fieldAccesses: Int = 0,
    var objectCreations: Int = 0,
    var typeReferences: Int = 0,
) {
    val totalWeight: Int
        get() = methodCalls + fieldAccesses + objectCreations + typeReferences
}
