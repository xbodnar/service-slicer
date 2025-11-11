package cz.bodnor.serviceslicer.infrastructure.cqrs.command

import jakarta.validation.Valid
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import kotlin.reflect.KClass

@Component
@Validated
class CommandBusMiddleware(
    commandHandlers: List<CommandHandler<*, *>>,
) : CommandBus {
    private val handlers: MutableMap<KClass<*>, CommandHandler<*, Command<*>>> = mutableMapOf()

    init {
        commandHandlers.forEach {
            if (handlers.containsKey(it.command)) {
                throw Exception("Multiple handlers for single command ${it.command}")
            }
            @Suppress("UNCHECKED_CAST")
            handlers[it.command] = it as CommandHandler<*, Command<*>>
        }
    }

    override fun <R> invoke(@Valid command: Command<R>): R {
        val commandClazz = command::class
        if (!handlers.containsKey(commandClazz)) throw Exception("No handler for command $commandClazz")
        @Suppress("UNCHECKED_CAST")
        return handlers[commandClazz]!!.handle(command) as R
    }
}
