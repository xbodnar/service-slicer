package cz.bodnor.serviceslicer.infrastructure.cqrs.query

import jakarta.validation.Valid
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import kotlin.reflect.KClass

@Component
@Validated
class QueryBusMiddleware(
    queryHandlers: List<QueryHandler<*, *>>,
) : QueryBus {
    private val handlers: MutableMap<KClass<*>, QueryHandler<*, Query<*>>> = mutableMapOf()

    init {
        queryHandlers.forEach {
            if (handlers.containsKey(it.query)) {
                throw Exception("Multiple handlers for single query ${it.query}")
            }
            @Suppress("UNCHECKED_CAST")
            handlers[it.query] = it as QueryHandler<*, Query<*>>
        }
    }

    override fun <R> invoke(@Valid query: Query<R>): R {
        val queryClazz = query::class
        if (!handlers.containsKey(queryClazz)) throw Exception("No handler for query $queryClazz")
        @Suppress("UNCHECKED_CAST")
        return handlers[queryClazz]!!.handle(query) as R
    }
}
