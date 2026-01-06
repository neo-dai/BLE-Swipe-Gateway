package com.mbbridge.controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbbridge.controller.ui.theme.MBBridgeControllerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MBBridgeControllerTheme {
                MBBridgeApp()
            }
        }
    }
}

@Composable
fun MBBridgeApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 服务器控制
            ServerControlCard(
                isRunning = uiState.isServerRunning,
                onStart = { viewModel.startServer() },
                onStop = { viewModel.stopServer() }
            )

            // 统计信息
            StatsCard(stats = uiState.stats)

            // 最近命令
            LastCommandCard(command = uiState.lastCommand)

            // 日志
            LogCard(
                logs = uiState.logs,
                onClear = { viewModel.clearLogs() }
            )

            // 模拟测试
            SimulateCard(
                onSimulatePrev = { viewModel.simulateCommand(CommandType.PREV) },
                onSimulateNext = { viewModel.simulateCommand(CommandType.NEXT) }
            )

            // 设置
            SettingsCard(
                token = uiState.token,
                onSaveToken = { viewModel.saveToken(it) },
                onOpenAccessibility = { viewModel.openAccessibilitySettings() }
            )
        }
    }
}

@Composable
fun ServerControlCard(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = context.getString(R.string.server_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRunning) context.getString(R.string.server_running)
                    else context.getString(R.string.server_stopped),
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = if (isRunning) onStop else onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isRunning) context.getString(R.string.stop_server)
                        else context.getString(R.string.start_server)
                )
            }
        }
    }
}

@Composable
fun StatsCard(stats: CommandStats) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.stats),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "PREV",
                    value = stats.prevCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "NEXT",
                    value = stats.nextCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    label = "Total",
                    value = stats.totalCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun LastCommandCard(command: Command?) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.last_command),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (command != null) {
                val timestamp = android.text.format.DateFormat.format(
                    "yyyy-MM-dd HH:mm:ss",
                    command.ts
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "类型: ${command.getCommandType()}")
                    Text(text = "值 (v): ${command.v}")
                    Text(text = "时间: $timestamp")
                    Text(text = "来源: ${command.source}")
                }
            } else {
                Text(
                    text = context.getString(R.string.no_command),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogCard(logs: List<String>, onClear: () -> Unit) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text(context.getString(R.string.clear_logs))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (logs.isEmpty()) {
                Text(
                    text = "暂无日志",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulateCard(onSimulatePrev: () -> Unit, onSimulateNext: () -> Unit) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = context.getString(R.string.simulate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSimulatePrev,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.simulate_prev))
                }
                OutlinedButton(
                    onClick = onSimulateNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(context.getString(R.string.simulate_next))
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    token: String,
    onSaveToken: (String) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf(token) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Token 设置
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text(context.getString(R.string.token_settings)) },
                placeholder = { Text(context.getString(R.string.token_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSaveToken(tokenInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.save_token))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 无障碍设置
            OutlinedButton(
                onClick = onOpenAccessibility,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.open_accessibility))
            }
        }
    }
}
