import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import data.models.StateEnum
import kotlinx.coroutines.*
import java.awt.Dimension

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
@Preview
fun App() {
    val composableScope = rememberCoroutineScope()
    val mainState = MainViewModel.mainState.collectAsState(initial = MainState.initial())

    composableScope.launch {
        MainViewModel.init()
    }

    MaterialTheme(colors = darkColors()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "DAMP", modifier = Modifier.padding(8.dp))
                    },
                    actions = {
                        AnimatedContent(mainState.value.configuration.dockerAvailable) {
                            Row {
                                IconButton(onClick = { composableScope.launch { MainViewModel.init() } }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }

                                if (it) {
                                    // Si au moins un service est démarré, on affiche le bouton "Stop all"
                                    if (mainState.value.configuration.services.values.any { it.isStarted() }) {
                                        IconButton(onClick = { composableScope.launch { MainViewModel.changeStateAll(StateEnum.STOPPED) } }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Stop all")
                                        }
                                    } else {
                                        IconButton(onClick = { composableScope.launch { MainViewModel.changeStateAll(StateEnum.STARTED) } }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start all")
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        ) {
            if (!mainState.value.configuration.dockerAvailable) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "Docker est actuellement non disponible.", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        composableScope.launch {
                            MainViewModel.init()
                        }
                    }) {
                        Text(text = "Rafraichir")
                    }
                }

                return@Scaffold
            }

            if (mainState.value.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).wrapContentHeight(),
                ) {
                    // Liste des services
                    mainState.value.configuration.services.values.map { it ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                modifier = Modifier
                                    .pointerHoverIcon(if (it.isStarted()) PointerIcon.Hand else PointerIcon.Default)
                                    .onClick {
                                        if (it.isStarted()) {
                                            java.awt.Desktop.getDesktop().browse(java.net.URI("http://localhost:${it.port}"))
                                        }
                                    },
                            ) {
                                Text(
                                    text = it.name,
                                    fontSize = MaterialTheme.typography.body1.fontSize,
                                )
                                AnimatedVisibility(it.isStarted()) {
                                    Text(
                                        fontSize = MaterialTheme.typography.body2.fontSize,
                                        text = "Port d'écoute : ${it.port ?: "N/A"}"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                modifier = Modifier.width(110.dp),
                                enabled = mainState.value.configuration.services.filterValues { it.isLoading() }.isEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = when (it.state) {
                                        StateEnum.STARTED -> MaterialTheme.colors.error
                                        StateEnum.STARTING -> MaterialTheme.colors.error
                                        StateEnum.STOPPED -> MaterialTheme.colors.primary
                                        StateEnum.STOPPING -> MaterialTheme.colors.primary
                                    }
                                ),
                                onClick = {
                                    composableScope.launch {
                                        withContext(Dispatchers.IO) {
                                            if (it.state == StateEnum.STOPPING || it.state == StateEnum.STARTING) {
                                                return@withContext
                                            }

                                            if (it.state == StateEnum.STOPPED) {
                                                // Si le service est actuellement arrêté, on le démarre
                                                MainViewModel.updateService(it.copy(state = StateEnum.STARTING, port = MainViewModel.getAvailablePort(it.minPort, it.maxPort)))
                                                MainViewModel.changeDockerServiceState(it, StateEnum.STARTED)
                                            } else if (it.isStarted()) {
                                                // Si le service est actuellement démarré, on l'arrête
                                                MainViewModel.updateService(it.copy(state = StateEnum.STOPPING))
                                                MainViewModel.changeDockerServiceState(it, StateEnum.STOPPED)
                                            }
                                        }
                                    }
                                }
                            ) {
                                when (it.state) {
                                    StateEnum.STARTED, StateEnum.STARTING -> Text(text = "Arrêter")
                                    StateEnum.STOPPED, StateEnum.STOPPING -> Text(text = "Démarrer")
                                }
                            }
                        }


                        AnimatedContent(it.state == StateEnum.STARTING || it.state == StateEnum.STOPPING) {
                            if (it) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(1.dp), strokeCap = StrokeCap.Round)
                            } else {
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                        }
                    }

                    AnimatedVisibility(!mainState.value.configuration.dockerAvailable) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.CenterHorizontally),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Red, text = "Docker n'est pas disponible sur votre système")
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = {
            exitApplication()
        },
        state = rememberWindowState(width = 400.dp, height = 358.dp),
        resizable = true,
        title = "DAMP",
    ) {
        window.minimumSize = Dimension(400, 358)

        App()
    }
}
