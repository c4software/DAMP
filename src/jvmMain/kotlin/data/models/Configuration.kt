package data.models

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val home: String = System.getProperty("user.home"),
    var dockerAvailable: Boolean = true,
    val services: MutableMap<String, Service> = hashMapOf(),
)