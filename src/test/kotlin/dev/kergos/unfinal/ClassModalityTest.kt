package dev.kergos.unfinal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassModalityTest {
    @Test
    fun `marked class stays final`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                class User(val name: String = "")
            """,
        )

        assertTrue(result.classIsFinal(), "the plugin does not open classes")
        assertFalse(result.fieldIsFinal("name"))
    }

    @Test
    fun `members stay final`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                class User(val name: String = "")
            """,
        )

        assertTrue(result.methodIsFinal("getName"))
    }

    @Test
    fun `base class fields are unfinalized in the base class`() {
        val result = compile(
            classAnnotations = listOf("Marked", "EmbeddedValue"),
            source = """
                annotation class Marked
                annotation class EmbeddedValue

                @Marked
                abstract class BaseClass(val baseField: Long)

                @Marked
                class User(val name: String = "") : BaseClass(1)

                @EmbeddedValue
                class ValueObject(val embeddedField: Int = 0)
            """,
        )

        assertFalse(result.fieldIsFinal("name"))
        assertFalse(result.fieldIsFinal("baseField", className = "BaseClass"))
        assertFalse(result.fieldIsFinal("embeddedField", className = "ValueObject"))
    }

    @Test
    fun `subclass of a marked class is in scope`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                abstract class BaseClass

                class ChildClass(val inheritedScopeField: String = "") : BaseClass()
            """,
        )

        assertFalse(result.fieldIsFinal("inheritedScopeField", className = "ChildClass"))
    }

    @Test
    fun `marked class is in scope through several levels of inheritance`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                abstract class RootClass

                abstract class MiddleClass : RootClass()

                class LeafClass(val deeplyInheritedField: String = "") : MiddleClass()
            """,
        )

        assertFalse(result.fieldIsFinal("deeplyInheritedField", className = "LeafClass"))
    }

    @Test
    fun `a marked interface is inherited transitively too`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                interface MarkedApi

                interface MiddleApi : MarkedApi

                class ImplementingClass(val interfaceInheritedField: String = "") : MiddleApi
            """,
        )

        assertFalse(result.fieldIsFinal("interfaceInheritedField", className = "ImplementingClass"))
    }

    @Test
    fun `no options change nothing`() {
        val result = compileWithoutOptions(source = """
            annotation class Marked

            @Marked
            class User(val name: String = "")
        """)

        assertTrue(result.fieldIsFinal("name"))
    }
}
