package dev.kergos.unfinal

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyAnnotationSelectorTest {
    @Test
    fun `selector value matches a qualified enum entry`() {
        val result = compile(
            propertyAnnotations = listOf("Flagged#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class Flagged(val mode: Mode = Mode.EAGER)

                class User(val plainField: String = "", @Flagged(mode = Mode.LAZY) val flaggedField: String = "")
            """,
        )

        assertTrue(result.fieldIsFinal("plainField"))
        assertFalse(result.fieldIsFinal("flaggedField"))
    }

    @Test
    fun `selector applies to a getter facing annotation`() {
        val result = compile(
            propertyAnnotations = listOf("Flagged#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class Flagged(val mode: Mode = Mode.EAGER)

                class User(@get:Flagged(mode = Mode.LAZY) val getterAnnotatedField: String = "")
            """,
        )

        assertFalse(result.fieldIsFinal("getterAnnotatedField"))
    }

    @Test
    fun `selector does not match another enum entry`() {
        val result = compile(
            propertyAnnotations = listOf("Flagged#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class Flagged(val mode: Mode = Mode.EAGER)

                class User(@Flagged(mode = Mode.EAGER) val eagerField: String = "")
            """,
        )

        assertTrue(result.fieldIsFinal("eagerField"))
    }

    @Test
    fun `declaration default counts when the source omits the argument`() {
        val result = compile(
            propertyAnnotations = listOf("LazyByDefault#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class LazyByDefault(val mode: Mode = Mode.LAZY)

                class User(@LazyByDefault val defaultedField: MutableList<String> = mutableListOf())
            """,
        )

        assertFalse(result.fieldIsFinal("defaultedField"), "the declaration default is LAZY")
    }

    @Test
    fun `a Java annotation default counts too`() {
        val result = compile(
            propertyAnnotations = listOf("javadefault.JavaLazy#value=LAZY"),
            source = """
                class Bag(@javadefault.JavaLazy val javaDefaultedField: MutableList<String> = mutableListOf())
            """,
        )

        assertFalse(result.fieldIsFinal("javaDefaultedField", className = "Bag"))
    }

    @Test
    fun `an eager default is not matched by LAZY`() {
        val result = compile(
            propertyAnnotations = listOf("EagerByDefault#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class EagerByDefault(val mode: Mode = Mode.EAGER)

                class User(@EagerByDefault val eagerDefaultedField: String? = null)
            """,
        )

        assertTrue(result.fieldIsFinal("eagerDefaultedField"))
    }

    @Test
    fun `property selector works without a class annotation`() {
        val result = compile(
            propertyAnnotations = listOf("Flagged#mode=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class Flagged(val mode: Mode = Mode.EAGER)

                class Dto(@Flagged(mode = Mode.LAZY) val dtoField: String = "")
            """,
        )

        assertFalse(result.fieldIsFinal("dtoField", className = "Dto"))
    }

    @Test
    fun `bare annotation name matches whatever its arguments`() {
        val result = compile(
            propertyAnnotations = listOf("Tagged"),
            source = """
                annotation class Tagged(val value: String)

                class User(@Tagged("anything") val taggedField: String = "")
            """,
        )

        assertFalse(result.fieldIsFinal("taggedField"))
    }

    @Test
    fun `unknown member never matches`() {
        val result = compile(
            propertyAnnotations = listOf("Flagged#nosuch=LAZY"),
            source = """
                enum class Mode { EAGER, LAZY }
                annotation class Flagged(val mode: Mode = Mode.EAGER)

                class User(@Flagged(mode = Mode.LAZY) val flaggedField: String = "")
            """,
        )

        assertTrue(result.fieldIsFinal("flaggedField"))
    }

    @Test
    fun `an empty value is rejected`() {
        val result = compileRawOptions(
            options = listOf(PluginOption(PLUGIN_ID, "propertyAnnotation", "Tagged#value=")),
            source = """
                annotation class Tagged(val value: String)
                class User(val plainField: String = "")
            """,
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("has an empty value"), result.messages)
    }

    @Test
    fun `property selectors leave unselected properties alone`() {
        val result = compile(
            propertyAnnotations = listOf("Renamed#name=label"),
            source = """
                annotation class Renamed(val name: String = "")

                class User(@Renamed(name = "label") val renamedField: String = "", val plainField: String = "")
            """,
        )

        assertFalse(result.fieldIsFinal("renamedField"))
        assertTrue(result.fieldIsFinal("plainField"))
    }
}
