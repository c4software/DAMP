import data.env.RepositoryEnv
import data.models.Configuration
import data.models.ProcessResult
import data.models.Service
import data.models.StateEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class MainState(
    var configuration: Configuration,
    var loading: Boolean,
) {
    companion object {
        fun initial() = MainState(
            loading = true,
            configuration = Configuration(),
        )
    }
}

object MainViewModel {

    private val _mainState: MutableStateFlow<MainState> = MutableStateFlow(MainState.initial())
    val mainState = _mainState.asStateFlow()

    suspend fun init() {
        withContext(Dispatchers.IO) {
            // Lecture du fichier de configuration
            _mainState.update { it.copy(configuration = RepositoryEnv.readConfigurationFile(), loading = true) }

            // Initialisation des services en fonction de leur état (running ou stopped) via Docker
            getAllServicesState()

            // On indique que le chargement est terminé
            _mainState.update { it.copy(loading = false) }
        }
    }

    fun updateService(service: Service) {
        val newList = (_mainState.value.configuration.services + hashMapOf(service.id to service)).toMutableMap()
        _mainState.update { it.copy(configuration = _mainState.value.configuration.copy(services = newList), loading = false) }
    }


    /**
     * Lance une commande et retourne le résultat (code de retour + sortie)
     */
    private suspend fun execCommands(commands: List<String>): ProcessResult {
        return withContext(Dispatchers.IO) {
            suspendCoroutine {

                val env = mutableMapOf<String, String>().apply {
                    put("DAMP_HOME_DIRECTORY", _mainState.value.configuration.home)

                    _mainState.value.configuration.services.values.forEach { service ->
                        put("DAMP_${service.id}_PORT".uppercase(), service.port.toString())
                    }
                }

                val process = ProcessBuilder(commands).apply {
                    directory(File(_mainState.value.configuration.home))
                    redirectErrorStream(true)
                    // inheritIO() // Enable this to see the output in the console (for debugging, because its consume the output stream
                    environment().putAll(env)
                }.start()

                process.waitFor()
                it.resume(ProcessResult(process.exitValue(), process.inputStream.bufferedReader().readText()))

                process.destroy()
            }
        }
    }

    /**
     * Initialise les services et leur état
     */
    private suspend fun getAllServicesState() {
        val results = execCommands(listOf("docker", "compose", "ps", "--services", "--filter", "status=running"))

        _mainState.update { it.copy(configuration = _mainState.value.configuration.copy(dockerAvailable = results.resultCode == 0)) }

        val servicesWithState = _mainState.value.configuration.services.values.map {
            it.copy(state = if (results.output.contains(it.profile, true)) StateEnum.STARTED else StateEnum.STOPPED)
        }

        // On met à jour la liste des services
        val newList = servicesWithState.associateBy { it.id }.toMutableMap()
        _mainState.update { it.copy(configuration = _mainState.value.configuration.copy(services = newList)) }

        if (results.output.contains("Cannot connect to the Docker", true) || results.resultCode != 0) {
            _mainState.update { it.copy(configuration = _mainState.value.configuration.copy(dockerAvailable = false)) }
        }
    }

    /**
     * Démarre ou arrête un service
     */
    suspend fun changeDockerServiceState(service: Service, state: StateEnum) {
        execCommands(
            mutableListOf("docker", "compose", "--profile", service.profile, if (state == StateEnum.STARTED) "up" else "down")
                .apply {
                    if (state == StateEnum.STARTED) {
                        add("-d")
                    }
                },
        )

        updateService(service.copy(state = state))
    }

    suspend fun changeStateAll(state: StateEnum = StateEnum.STARTED) {
        // Calcul du port pour chaque service
        val services = _mainState.value.configuration.services.values.map {
            if (!it.isStarted()) {
                it.copy(port = getAvailablePort(it.minPort, it.maxPort), state = StateEnum.STARTING)
            } else {
                it
            }
        }

        // On met à jour la liste des services
        val newList = services.associateBy { it.id }.toMutableMap()
        _mainState.update { it.copy(configuration = _mainState.value.configuration.copy(services = newList)) }

        delay(500)

//        // On lance ou stoppe les services
        execCommands(
            mutableListOf("docker", "compose", "--profile", "default").apply {
                add(if (state == StateEnum.STARTED) "up" else "down")
                if (state == StateEnum.STARTED) add("-d")
            })

        getAllServicesState()
    }

    /**
     * Retourne le port disponible entre deux bornes de ports
     */
    fun getAvailablePort(start: Int, end: Int): Int? {
        for (port in start..end) {
            try {
                java.net.ServerSocket(port).close()
                return port
            } catch (ex: java.lang.Exception) {
                // port not available
            }
        }
        return null
    }

}