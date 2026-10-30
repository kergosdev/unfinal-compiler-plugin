package dev.kergos.unfinal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldFinalityTest {
    @Test
    fun `val backing field is not final`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked
                annotation class Key

                @Marked
                class User(@Key val id: Long = 0, val name: String = "")
            """,
        )

        assertFalse(result.fieldIsFinal("name"))
        assertFalse(result.fieldIsFinal("id"))
    }

    @Test
    fun `var and delegated properties keep their shape`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                @Marked
                class User(val valProperty: String = "") {
                    var varProperty: String = ""
                    private val privateProperty: String = ""
                    val delegatedProperty: String by lazy { "" }
                }
            """,
        )

        assertFalse(result.fieldIsFinal("valProperty"))
        assertFalse(result.fieldIsFinal("varProperty"))
        assertFalse(result.fieldIsFinal("privateProperty"))
        assertTrue(result.fieldIsFinal("delegatedProperty\$delegate"), "a delegated property has no field to unfinalize")
    }

    @Test
    fun `unmarked classes are untouched`() {
        val result = compile(
            classAnnotations = listOf("Marked"),
            source = """
                annotation class Marked

                class User(val unmarkedProperty: String = "")
            """,
        )

        assertTrue(result.fieldIsFinal("unmarkedProperty"))
        assertTrue(result.classIsFinal())
    }

    @Test
    fun `an annotation from another package is supported`() {
        val otherPackage = SourceFileOf(
            """
            package other

            @Target(AnnotationTarget.CLASS)
            annotation class Elsewhere
            """,
        )

        val result = compile(
            classAnnotations = listOf("other.Elsewhere"),
            source = """
                @other.Elsewhere
                class User(val name: String = "")
            """,
            extraSources = listOf(otherPackage),
        )

        assertFalse(result.fieldIsFinal("name"))
    }

    private fun SourceFileOf(content: String) = com.tschuchort.compiletesting.SourceFile.kotlin("other.kt", content)
}
