package cz.bodnor.serviceslicer.infrastructure.cqrs.query

import jakarta.validation.Valid
import kotlin.reflect.KClass

interface Query<out R>

interface QueryHandler<out R, T : Query<R>> {
    val query: KClass<T>

    fun handle(query: T): R
}

interface QueryBus {
    operator fun <T> invoke(@Valid query: Query<T>): T
}
