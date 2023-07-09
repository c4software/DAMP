package data.env

import data.models.Configuration
import net.mamoe.yamlkt.Yaml
import java.io.File

object RepositoryEnv {

    fun readConfigurationFile(): Configuration {
        return try {
            val path = File(System.getProperty("user.home") + "/.damp/configuration.yaml")
            val localPath = File("configuration.yaml")
            if (localPath.exists()) {
                val configuration = localPath.readText()
                return Yaml.decodeFromString(Configuration.serializer(), configuration)
            } else if (path.exists()) {
                val configuration = path.readText()
                return Yaml.decodeFromString(Configuration.serializer(), configuration)
            } else {
                Configuration()
            }
        } catch (e: Exception) {
            print(e)
            Configuration()
        }
    }
}