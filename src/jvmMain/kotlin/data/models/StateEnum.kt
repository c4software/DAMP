package data.models

import kotlinx.serialization.Serializable

@Serializable
enum class StateEnum {
    STARTED, STARTING, STOPPED, STOPPING
}