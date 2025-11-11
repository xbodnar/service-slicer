package cz.bodnor.serviceslicer.infrastructure.cqrs.command

import jakarta.validation.Valid
import kotlin.reflect.KClass

interface Command<out R>

interface CommandHandler<out R, T : Command<R>> {
    val command: KClass<T>

    fun handle(command: T): R
}

interface CommandBus {
    operator fun <T> invoke(@Valid command: Command<T>): T
}
