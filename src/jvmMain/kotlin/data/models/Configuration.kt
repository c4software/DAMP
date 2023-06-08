package data.models

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val home: String = "",
    var dockerAvailable: Boolean = false,
    val services: MutableMap<String, Service> = hashMapOf(),
)