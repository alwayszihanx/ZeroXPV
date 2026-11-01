package io.alwayszihan.zeroxpv.repository.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiModelInfo(
  val id: String,
  val displayName: String,
  val isFree: Boolean = false,
)
