package data.models

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: String,
    val name: String,
    var port: Int?,
    val minPort: Int = 8000,
    val maxPort: Int = 9000,
    val state: StateEnum = StateEnum.STOPPED,
    val profile: String = "default"
) {
    fun isStarted(): Boolean {
        return state == StateEnum.STARTED
    }

    fun isLoading(): Boolean {
        return state == StateEnum.STARTING || state == StateEnum.STOPPING
    }
}