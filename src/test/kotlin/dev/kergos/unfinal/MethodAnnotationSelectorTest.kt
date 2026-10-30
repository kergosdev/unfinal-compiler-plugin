package dev.kergos.unfinal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MethodAnnotationSelectorTest {
    @Test
    fun `annotated method is opened`() {
        val result = compile(
            methodAnnotations = listOf("Opens"),
            source = """
                annotation class Opens

                class User {
                    @Opens
                    fun openedMethod() {}
                }
            """,
        )

        assertFalse(result.methodIsFinal("openedMethod"))
    }

    @Test
    fun `unannotated method stays final`() {
        val result = compile(
            methodAnnotations = listOf("Opens"),
            source = """
                annotation class Opens

                class User {
                    @Opens
                    fun openedMethod() {}

                    fun unannotatedMethod() {}
                }
            """,
        )

        assertFalse(result.methodIsFinal("openedMethod"))
        assertTrue(result.methodIsFinal("unannotatedMethod"))
    }

    @Test
    fun `method selector needs no class marker`() {
        val result = compile(
            methodAnnotations = listOf("Opens"),
            source = """
                annotation class Opens

                class Listener {
                    @Opens
                    fun listenerMethod() {}
                }
            """,
        )

        assertFalse(result.methodIsFinal("listenerMethod", className = "Listener"))
    }

    @Test
    fun `methods are untouched without method selectors`() {
        val result = compile(
            source = """
                annotation class Opens

                class User {
                    @Opens
                    fun openedMethod() {}
                }
            """,
        )

        assertTrue(result.methodIsFinal("openedMethod"))
    }

    @Test
    fun `a selector from another package is honoured`() {
        val otherPackage = com.tschuchort.compiletesting.SourceFile.kotlin(
            "other.kt",
            """
            package other

            @Target(AnnotationTarget.FUNCTION)
            annotation class ElsewhereOpens
            """,
        )

        val result = compile(
            methodAnnotations = listOf("other.ElsewhereOpens"),
            source = """
                class User {
                    @other.ElsewhereOpens
                    fun openedMethod() {}
                }
            """,
            extraSources = listOf(otherPackage),
        )

        assertFalse(result.methodIsFinal("openedMethod"))
    }
}
