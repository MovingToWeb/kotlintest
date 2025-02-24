package io.kotlintest.extensions.system

sealed class OverrideMode {

  @PublishedApi
  internal abstract fun override(originalValues: Map<String, String>, newValues: Map<String, String?>): MutableMap<String, String>

  /**
   * Sets specific values and overrides pre-existent ones, if any
   *
   * Any values that are not present in the overrides will be left untouched.
   */
  object SetOrOverride : OverrideMode() {
    override fun override(originalValues: Map<String, String>, newValues: Map<String, String?>) =
            originalValues.toMutableMap().apply { putReplacingNulls(newValues) }
  }

  /**
   * Sets specific values, ignoring pre-existent ones, if any
   *
   * Any values that are not present in the overrides will be left untouched.
   */
  object SetOrIgnore : OverrideMode() {
    override fun override(originalValues: Map<String, String>, newValues: Map<String, String?>) =
            originalValues.toMutableMap().apply { putWithoutReplacements(newValues) }

    private fun MutableMap<String, String>.putWithoutReplacements(map: Map<String, String?>) {
      map.forEach { (key, value) ->
        value?.let { this.putIfAbsent(key, it) }
      }
    }
  }

  /**
   * Sets specific values and throws an exception if the chosen key already exists
   *
   * Any values that are not present in the overrides will be left untouched.
   */
  object SetOrError : OverrideMode() {
    override fun override(originalValues: Map<String, String>, newValues: Map<String, String?>): MutableMap<String, String> {
      val keysToForbidOverride = originalValues.keys

      return if (newValues.keys.any { it in keysToForbidOverride }) {
        throw IllegalOverrideException()
      } else {
        SetOrOverride.override(originalValues, newValues)
      }

    }

    class IllegalOverrideException : IllegalArgumentException("Overriding a variable when mode is set to SetOrError. Use another OverrideMode to allow this.")
  }
}

@PublishedApi
internal fun MutableMap<String, String>.putReplacingNulls(map: Map<String, String?>) {
  map.forEach { (key, value) ->
    if (value == null) remove(key) else put(key, value)
  }
}
