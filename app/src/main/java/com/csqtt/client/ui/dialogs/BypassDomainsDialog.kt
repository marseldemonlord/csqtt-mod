// SPDX-FileCopyrightText: 2026 amurcanov
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.csqtt.client.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csqtt.client.SettingsStore
import com.csqtt.client.VpnRoutingPolicy
import com.csqtt.client.showRaisedToast
import com.csqtt.client.ui.design.CsqttShapes

@Composable
fun BypassDomainsDialog(
    initialDomains: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var domainsText by remember { mutableStateOf(initialDomains) }
    val parsedCount = remember(domainsText) { VpnRoutingPolicy.parseDomains(domainsText).size }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CsqttShapes.Dialog,
        title = {
            Text(
                "Исключения доменов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Сайты и домены, которые ВСЕГДА идут напрямую через домашнего провайдера (в обход VPN):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = domainsText,
                    onValueChange = { domainsText = it },
                    placeholder = {
                        Text(
                            "vk.com\nyandex.com\nya.com\nrucaptcha.com",
                            fontSize = 13.sp,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = CsqttShapes.Control,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
                )

                Text(
                    "Активных доменов: $parsedCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            val pasted = clip?.getItemAt(0)?.text?.toString()?.trim().orEmpty()
                            if (pasted.isNotBlank()) {
                                domainsText = if (domainsText.isBlank()) {
                                    pasted
                                } else {
                                    domainsText.trim() + "\n" + pasted
                                }
                                context.showRaisedToast("Вставлено из буфера", Toast.LENGTH_SHORT)
                            } else {
                                context.showRaisedToast("Буфер обмена пуст", Toast.LENGTH_SHORT)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Вставить", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Bypass Domains", domainsText.trim())
                            clipboard.setPrimaryClip(clip)
                            context.showRaisedToast("Скопировано в буфер", Toast.LENGTH_SHORT)
                        },
                        modifier = Modifier.weight(1f),
                        shape = CsqttShapes.Control,
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Копировать", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    TextButton(
                        onClick = {
                            domainsText = SettingsStore.DEFAULT_BYPASS_DOMAINS
                            context.showRaisedToast("Сброшено на стандартные", Toast.LENGTH_SHORT)
                        },
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сброс на стандартные", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(domainsText.trim())
                    onDismiss()
                },
                shape = CsqttShapes.Control,
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CsqttShapes.Control,
            ) {
                Text("Отмена")
            }
        },
    )
}
