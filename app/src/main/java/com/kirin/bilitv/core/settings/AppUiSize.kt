package com.kirin.bilitv.core.settings

enum class AppUiSize(val key: String) {
  Small("small"), Medium("medium"), Large("large");

  fun next(): AppUiSize = entries[(ordinal + 1) % entries.size]

  companion object {
    fun fromKey(key: String?): AppUiSize = entries.firstOrNull { it.key == key } ?: Medium
  }
}
