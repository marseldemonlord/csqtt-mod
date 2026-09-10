// SPDX-FileCopyrightText: 2026 amurcanov
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.csqtt.client.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csqtt.client.SettingsStore
import com.csqtt.client.TunnelManager
import com.csqtt.client.VpnRoutingPolicy
import com.csqtt.client.showRaisedToast
import com.csqtt.client.ui.components.CsqttScreen
import com.csqtt.client.ui.components.CsqttSegmentedControl
import com.csqtt.client.ui.design.CsqttShapes
import com.csqtt.client.ui.design.CsqttSizes
import com.csqtt.client.ui.design.CsqttSpacing
import com.csqtt.client.ui.dialogs.BypassDomainsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoutingTab(
    settingsStore: SettingsStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val vpnRoutingMode by settingsStore.routingMode.collectAsStateWithLifecycle(initialValue = VpnRoutingPolicy.MODE_ALL)
    val bypassDomains by settingsStore.bypassDomains.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_BYPASS_DOMAINS)
    val customRoutes by settingsStore.customRoutes.collectAsStateWithLifecycle(initialValue = "")

    var showDomainsDialog by rememberSaveable { mutableStateOf(false) }
    var singleDomainInput by rememberSaveable { mutableStateOf("") }

    val parsedDomains = remember(bypassDomains) { VpnRoutingPolicy.parseDomains(bypassDomains) }

    fun applyAndReload() {
        scope.launch {
            delay(200)
            TunnelManager.reloadVpn()
        }
    }

    CsqttScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(CsqttSpacing.Md),
        ) {
            // Card 1: Traffic Routing Mode
            AppSectionCard(
                contentPadding = PaddingValues(horizontal = CsqttSpacing.Md, vertical = CsqttSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(CsqttSpacing.Sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "Режим туннелирования трафика",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                CsqttSegmentedControl(
                    options = listOf(
                        VpnRoutingPolicy.MODE_ALL to "Весь трафик (0.0.0.0/0)",
                        VpnRoutingPolicy.MODE_SELECTIVE to "Только заблокированные",
                    ),
                    selected = vpnRoutingMode,
                    enabled = true,
                    onSelected = { mode ->
                        if (mode == vpnRoutingMode) return@CsqttSegmentedControl
                        scope.launch {
                            settingsStore.saveRoutingMode(mode)
                            applyAndReload()
                        }
                    },
                )

                Text(
                    text = if (vpnRoutingMode == VpnRoutingPolicy.MODE_ALL) {
                        "Полный туннель: весь трафик устройства направляется через VPN, за исключением сайтов из списка обхода ниже и приложений из вкладки «Исключ.»."
                    } else {
                        "Умный обход: через VPN идут только заблокированные сервисы (YouTube, Discord, Meta, зарубежные CDN и т.д.). Сайты РФ (.ru), банки и сервисы идут напрямую на максимальной скорости."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Card 2: Simultaneous operation status
            AppSectionCard(
                contentPadding = PaddingValues(horizontal = CsqttSpacing.Md, vertical = CsqttSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(CsqttSpacing.Xs),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            "Одновременная работа правил",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Исключения приложений (вкладка «Исключ.») и правила обхода сайтов/маршрутов работают одновременно и независимо.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Card 3: Bypass Domains List
            AppSectionCard(
                contentPadding = PaddingValues(horizontal = CsqttSpacing.Md, vertical = CsqttSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(CsqttSpacing.Sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                "Сайты и домены в обход VPN",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Всегда мимо VPN:  шт.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showDomainsDialog = true },
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Списком", fontSize = 12.sp)
                    }
                }

                Text(
                    "Трафик к этим доменам всегда направляется напрямую к локальному провайдеру, минуя туннель:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Action buttons: Paste, Copy, Reset, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            val pastedText = clip?.getItemAt(0)?.text?.toString().orEmpty()
                            val pastedDomains = VpnRoutingPolicy.parseDomains(pastedText)
                            if (pastedDomains.isNotEmpty()) {
                                val current = VpnRoutingPolicy.parseDomains(bypassDomains).toMutableSet()
                                current.addAll(pastedDomains)
                                val result = current.sorted().joinToString("\n")
                                scope.launch {
                                    settingsStore.saveBypassDomains(result)
                                    applyAndReload()
                                }
                                context.showRaisedToast("Добавлено  доменов из буфера", Toast.LENGTH_SHORT)
                            } else {
                                context.showRaisedToast("В буфере не найдено доменов", Toast.LENGTH_SHORT)
                            }
                        },
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Вставить", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            if (parsedDomains.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Bypass Domains", parsedDomains.joinToString("\n"))
                                clipboard.setPrimaryClip(clip)
                                context.showRaisedToast("Скопировано  доменов в буфер", Toast.LENGTH_SHORT)
                            } else {
                                context.showRaisedToast("Список доменов пуст", Toast.LENGTH_SHORT)
                            }
                        },
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Копировать", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                settingsStore.saveBypassDomains(SettingsStore.DEFAULT_BYPASS_DOMAINS)
                                applyAndReload()
                            }
                            context.showRaisedToast("Сброшено на стандартные (ВК, Яндекс, Рукапча...)", Toast.LENGTH_SHORT)
                        },
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сброс", fontSize = 12.sp)
                    }

                    if (parsedDomains.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    settingsStore.saveBypassDomains("")
                                    applyAndReload()
                                }
                                context.showRaisedToast("Список доменов очищен", Toast.LENGTH_SHORT)
                            },
                            shape = CsqttShapes.Control,
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Очистить", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Quick add single domain row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = singleDomainInput,
                        onValueChange = { singleDomainInput = it },
                        placeholder = { Text("example.com", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = CsqttShapes.Control,
                    )
                    Button(
                        onClick = {
                            val domain = singleDomainInput.trim().lowercase().removePrefix("http://").removePrefix("https://").split("/").firstOrNull() ?: ""
                            if (domain.length >= 3 && domain.contains(".")) {
                                val current = VpnRoutingPolicy.parseDomains(bypassDomains).toMutableSet()
                                current.add(domain)
                                val result = current.sorted().joinToString("\n")
                                scope.launch {
                                    settingsStore.saveBypassDomains(result)
                                    applyAndReload()
                                }
                                singleDomainInput = ""
                                context.showRaisedToast("Домен  добавлен", Toast.LENGTH_SHORT)
                            } else {
                                context.showRaisedToast("Введите корректный домен (например site.ru)", Toast.LENGTH_SHORT)
                            }
                        },
                        shape = CsqttShapes.Control,
                    ) {
                        Text("+ Добавить", fontSize = 12.sp)
                    }
                }

                // Inline preview of domains
                OutlinedTextField(
                    value = bypassDomains,
                    onValueChange = { updated ->
                        scope.launch {
                            settingsStore.saveBypassDomains(updated)
                            applyAndReload()
                        }
                    },
                    label = { Text("Список доменов (по одному на строку)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = CsqttShapes.Control,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
                )
            }

            // Card 4: Custom CIDR Routes
            AppSectionCard(
                contentPadding = PaddingValues(horizontal = CsqttSpacing.Md, vertical = CsqttSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(CsqttSpacing.Sm),
            ) {
                Text(
                    "Пользовательские подсети (CIDR)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Дополнительные IP-подсети, направляемые в VPN (например 1.1.1.1/32, 104.16.0.0/12):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = customRoutes,
                    onValueChange = { updated ->
                        scope.launch {
                            settingsStore.saveCustomRoutes(updated)
                            applyAndReload()
                        }
                    },
                    placeholder = { Text("1.1.1.1/32\n8.8.8.8/32", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = CsqttShapes.Control,
                )
            }

            Spacer(Modifier.height(CsqttSizes.ScreenBottomPadding + 20.dp))
        }

        if (showDomainsDialog) {
            BypassDomainsDialog(
                initialDomains = bypassDomains,
                onSave = { updatedDomains ->
                    scope.launch {
                        settingsStore.saveBypassDomains(updatedDomains)
                        applyAndReload()
                    }
                },
                onDismiss = { showDomainsDialog = false },
            )
        }
    }
}
