package io.kotlintest.specs

import io.kotlintest.*
import io.kotlintest.core.TestCaseConfig
import io.kotlintest.core.TestContext
import io.kotlintest.core.specs.KotlinTestDsl
import io.kotlintest.extensions.TestCaseExtension
import kotlin.coroutines.CoroutineContext

/**
 * Example:
 *
 * "some test" should {
 *    "do something" {
 *      // test here
 *    }
 * }
 *
 */
@Suppress("FunctionName")
abstract class AbstractWordSpec(body: AbstractWordSpec.() -> Unit = {}) : AbstractSpec() {

  init {
    body()
  }

  infix fun String.should(init: suspend WordScope.() -> Unit) =
      addTestCase("$this should", { this@AbstractWordSpec.WordScope(this).init() }, defaultTestCaseConfig, TestType.Container)

  infix fun String.When(init: suspend WhenContext.() -> Unit) = addWhenContext(this, init)
  infix fun String.`when`(init: suspend WhenContext.() -> Unit) = addWhenContext(this, init)

  private fun addWhenContext(name: String, init: suspend WhenContext.() -> Unit) {
    addTestCase("$name when", { thisSpec.WhenContext(this).init() }, defaultTestCaseConfig, TestType.Container)
  }

  @KotlinTestDsl
  inner class WordScope(val context: TestContext) {

    suspend fun String.config(
        invocations: Int? = null,
        enabled: Boolean? = null,
        timeout: Long? = null,
        threads: Int? = null,
        tags: Set<Tag>? = null,
        extensions: List<TestCaseExtension>? = null,
        test: suspend FinalTestContext.() -> Unit) {
      val config = TestCaseConfig(
          enabled ?: this@AbstractWordSpec.defaultTestCaseConfig.enabled,
          invocations ?: this@AbstractWordSpec.defaultTestCaseConfig.invocations,
          timeout ?: this@AbstractWordSpec.defaultTestCaseConfig.timeout,
          threads ?: this@AbstractWordSpec.defaultTestCaseConfig.threads,
          tags ?: this@AbstractWordSpec.defaultTestCaseConfig.tags,
          extensions ?: this@AbstractWordSpec.defaultTestCaseConfig.extensions)
      context.registerTestCase(this, this@AbstractWordSpec, { FinalTestContext(this, coroutineContext).test() }, config, TestType.Test)
    }

    suspend infix operator fun String.invoke(test: suspend FinalTestContext.() -> Unit) =
        context.registerTestCase(this, this@AbstractWordSpec, { FinalTestContext(this, coroutineContext).test() }, this@AbstractWordSpec.defaultTestCaseConfig, TestType.Test)

    // we need to override the should method to stop people nesting a should inside a should
    @Deprecated("A should block can only be used at the top level", ReplaceWith("{}"), level = DeprecationLevel.ERROR)
    infix fun String.should(init: () -> Unit) = { init() }
  }

  @KotlinTestDsl
  inner class WhenContext(val context: TestContext) {

    suspend infix fun String.Should(test: suspend WordScope.() -> Unit) = addShouldContext(this, test)
    suspend infix fun String.should(test: suspend WordScope.() -> Unit) = addShouldContext(this, test)

    private suspend fun addShouldContext(name: String, test: suspend WordScope.() -> Unit) {
      context.registerTestCase("$name should", thisSpec, { thisSpec.WordScope(this).test() }, thisSpec.defaultTestCaseConfig, TestType.Container)
    }

  }

  @KotlinTestDsl
  inner class FinalTestContext(val context: TestContext, coroutineContext: CoroutineContext) : TestContext(coroutineContext) {

    override fun description(): Description = context.description()
    override suspend fun registerTestCase(testCase: TestCase) = context.registerTestCase(testCase)

    // we need to override the should method to stop people nesting a should inside a should
    @Deprecated("A should block can only be used at the top level", ReplaceWith("{}"), level = DeprecationLevel.ERROR)
    infix fun String.should(init: () -> Unit) = { init() }
  }

  private val thisSpec: AbstractWordSpec
    get() = this@AbstractWordSpec
}
