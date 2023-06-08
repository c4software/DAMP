package data.env

import data.models.Configuration
import net.mamoe.yamlkt.Yaml
import java.io.File

object RepositoryEnv {

    fun readConfigurationFile(): Configuration {
       return try {
            val configuration = File("configuration.yaml").readText()
            return Yaml.decodeFromString(Configuration.serializer(), configuration)
        } catch (e: Exception) {
            print(e)
            Configuration()
        }
    }
}