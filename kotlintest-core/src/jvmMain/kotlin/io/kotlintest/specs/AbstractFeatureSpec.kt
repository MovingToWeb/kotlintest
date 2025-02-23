package io.kotlintest.specs

import io.kotlintest.AbstractSpec
import io.kotlintest.Tag
import io.kotlintest.TestType
import io.kotlintest.core.TestCaseConfig
import io.kotlintest.core.TestContext
import io.kotlintest.core.specs.KotlinTestDsl
import io.kotlintest.extensions.TestCaseExtension

abstract class AbstractFeatureSpec(body: AbstractFeatureSpec.() -> Unit = {}) : AbstractSpec() {

  init {
    body()
  }

  inner class ScenarioBuilder(val name: String, val context: TestContext) {
    suspend fun config(
        invocations: Int? = null,
        enabled: Boolean? = null,
        timeout: Long? = null,
        threads: Int? = null,
        tags: Set<Tag>? = null,
        extensions: List<TestCaseExtension>? = null,
        test: suspend TestContext.() -> Unit) {
      val config = TestCaseConfig(
          enabled ?: defaultTestCaseConfig.enabled,
          invocations ?: defaultTestCaseConfig.invocations,
          timeout ?: defaultTestCaseConfig.timeout,
          threads ?: defaultTestCaseConfig.threads,
          tags ?: defaultTestCaseConfig.tags,
          extensions ?: defaultTestCaseConfig.extensions)
      context.registerTestCase(name, this@AbstractFeatureSpec, test, config, TestType.Test)
    }
  }

  fun feature(name: String, init: suspend FeatureScope.() -> Unit) =
      addTestCase(createTestName("Feature: ", name), { this@AbstractFeatureSpec.FeatureScope(this).init() }, defaultTestCaseConfig, TestType.Container)

  @KotlinTestDsl
  inner class FeatureScope(val context: TestContext) {

    suspend fun and(name: String, init: suspend FeatureScope.() -> Unit) =
        context.registerTestCase(createTestName("And: ", name), this@AbstractFeatureSpec, { this@AbstractFeatureSpec.FeatureScope(this).init() }, this@AbstractFeatureSpec.defaultTestCaseConfig, TestType.Container)

    suspend fun scenario(name: String, test: suspend TestContext.() -> Unit) =
        context.registerTestCase(createTestName("Scenario: ", name), this@AbstractFeatureSpec, test, this@AbstractFeatureSpec.defaultTestCaseConfig, TestType.Test)

    fun scenario(name: String) = this@AbstractFeatureSpec.ScenarioBuilder(createTestName("Scenario: ", name), context)
  }
}
