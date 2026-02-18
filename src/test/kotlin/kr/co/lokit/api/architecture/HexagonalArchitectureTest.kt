package kr.co.lokit.api.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HexagonalArchitectureTest {
    companion object {
        private lateinit var classes: JavaClasses

        @JvmStatic
        @BeforeAll
        fun setUp() {
            classes =
                ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("kr.co.lokit.api")
        }
    }

    @Test
    fun `domain model must not depend on application or infrastructure`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..domain..application..",
                "..domain..infrastructure..",
                "..domain..presentation..",
            ).check(classes)
    }

    @Test
    fun `application layer must not depend on infrastructure implementation`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..domain..infrastructure..")
            .check(classes)
    }

    @Test
    fun `application layer must not depend on dto`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..domain..dto..")
            .check(classes)
    }

    @Test
    fun `application layer must not depend on jpa entities`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..application..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Entity")
            .check(classes)
    }
}
