package cz.bodnor.serviceslicer

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@Suppress("ktlint:standard:property-naming")
@AnalyzeClasses(
    packages = ["cz.bodnor.serviceslicer"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTests {

    @ArchTest
    val `r1 - Domain classes should not depend on application and adapter`: ArchRule = noClasses()
        .that()
        .resideInAPackage(domain)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(application, adapters)

    @ArchTest
    val `r2 - Application classes should not depend on adapters`: ArchRule = noClasses()
        .that()
        .resideInAPackage(application)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(adapters)

    @ArchTest
    val `r3 - Infrastructure classes should be independent`: ArchRule = noClasses()
        .that()
        .resideInAPackage(infrastructure)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(application, adapters)

    @ArchTest
    val `r4 - Port out should only be implemented by out adapters`: ArchRule = classes().that()
        .areNotInterfaces()
        .and().implement(resideInAPackage(outPorts)) // any class implementing a port.out interface
        .should().resideInAPackage(outAdapters) // must live in adapters
        .because("Implementations of port.out interfaces should be provided by adapters.")

    @ArchTest
    val `r5 - In adapters should only depend on CQRS abstractions`: ArchRule = classes().that()
        .resideInAPackage(inAdapters)
        .should()
        .onlyAccessClassesThat(
            resideOutsideOfPackage(basePackage)
                .or(resideInAnyPackage(cqrs, inAdapters, outAdapters, command, query, event)),
        )
        .because(
            "In adapters should talk to application only through CQRS abstractions, and not depend on concrete types outside adapters.",
        )

    companion object {
        private val basePackage = "cz.bodnor.serviceslicer"
        private val domain = "..domain.."
        private val application = "..application.."
        private val infrastructure = "..infrastructure.."
        private val adapters = "..adapter.."
        private val inAdapters = "..adapter..in.."
        private val outAdapters = "..adapter..out.."
        private val outPorts = "..application..port..out"
        private val cqrs = "..infrastructure..cqrs.."
        private val command = "..command"
        private val query = "..query"
        private val event = "..event"

        val isExternal = resideOutsideOfPackage(basePackage)
        val isCqrs = resideInAPackage(cqrs)
        val isInAdapter = resideInAPackage(inAdapters)
        val isOutAdapter = resideInAPackage(outAdapters)
        val isCommand = resideInAPackage(command)
        val isQuery = resideInAPackage(query)
        val isEvent = resideInAPackage(event)
    }
}
