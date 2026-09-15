package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.database.ApprovedChannel
import com.example.data.settings.AppSettings
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.SurfaceCard
import com.example.ui.theme.Accent
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

@Composable
fun SupervisorScreen(
    settings: AppSettings,
    channels: List<ApprovedChannel>,
    keyCheck: KeyCheckState,
    isAddingChannel: Boolean,
    channelInput: String,
    nameInput: String,
    descriptionInput: String,
    newPin: String,
    confirmPin: String,
    onChannelInputChange: (String) -> Unit,
    onNameInputChange: (String) -> Unit,
    onDescriptionInputChange: (String) -> Unit,
    onNewPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onAddChannel: () -> Unit,
    onDeleteChannel: (ApprovedChannel) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onRefreshMetadata: () -> Unit,
    onChangePin: () -> Unit,
    onClearHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ApiKeySection(settings = settings, keyCheck = keyCheck, onSave = onSaveApiKey) }

        item {
            AddChannelSection(
                channelInput = channelInput,
                nameInput = nameInput,
                descriptionInput = descriptionInput,
                isAdding = isAddingChannel,
                onChannelInputChange = onChannelInputChange,
                onNameInputChange = onNameInputChange,
                onDescriptionInputChange = onDescriptionInputChange,
                onAddChannel = onAddChannel,
            )
        }

        item {
            ManageChannelsSection(
                channels = channels,
                onDelete = onDeleteChannel,
                onRefreshMetadata = onRefreshMetadata,
            )
        }

        item {
            PinSection(
                newPin = newPin,
                confirmPin = confirmPin,
                onNewPinChange = onNewPinChange,
                onConfirmPinChange = onConfirmPinChange,
                onChangePin = onChangePin,
            )
        }

        item { MaintenanceSection(onClearHistory = onClearHistory) }
    }
}

@Composable
private fun PanelCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accent: androidx.compose.ui.graphics.Color = Accent.Primary,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Ivory.Strong)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Ivory.Faint)
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun PanelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    isNumeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = {
            if (placeholder != null) {
                Text(placeholder, color = Ivory.Faint, style = MaterialTheme.typography.bodySmall)
            }
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                isNumeric -> KeyboardType.NumberPassword
                isPassword -> KeyboardType.Password
                else -> KeyboardType.Text
            }
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent.Primary,
            unfocusedBorderColor = Line.Regular,
            focusedTextColor = Ivory.Strong,
            unfocusedTextColor = Ivory.Strong,
            focusedLabelColor = Accent.PrimarySoft,
            unfocusedLabelColor = Ivory.Faint,
            cursorColor = Accent.PrimarySoft,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ApiKeySection(
    settings: AppSettings,
    keyCheck: KeyCheckState,
    onSave: (String) -> Unit,
) {
    var draft by remember(settings.youtubeApiKey) { mutableStateOf(settings.youtubeApiKey) }
    val masked = remember(settings.youtubeApiKey) {
        settings.youtubeApiKey.let { key ->
            if (key.length <= 8) key else key.take(6) + "…" + key.takeLast(4)
        }
    }

    PanelCard(
        icon = Icons.Filled.Key,
        title = "מפתח YouTube API",
        subtitle = if (settings.hasApiKey) "מוגדר: $masked" else "לא מוגדר — מצב RSS מוגבל",
    ) {
        Text(
            text = "המפתח נשמר במכשיר בלבד ואינו חלק מקוד האפליקציה. עם מפתח מוצגים יותר סרטונים, תמונות ערוץ ומשך זמן לכל שיעור.",
            style = MaterialTheme.typography.bodySmall,
            color = Ivory.Muted,
        )
        PanelTextField(
            value = draft,
            onValueChange = { draft = it.trim() },
            label = "מפתח API",
            placeholder = "AIza...",
            isPassword = true,
            modifier = Modifier.testTag("supervisor_api_key_input"),
        )
        when (keyCheck) {
            is KeyCheckState.Invalid -> InlineNote(Icons.Filled.ErrorOutline, keyCheck.message, Accent.Danger)
            KeyCheckState.Valid -> InlineNote(Icons.Filled.CheckCircle, "המפתח אומת ונשמר.", Accent.Success)
            else -> Unit
        }
        Button(
            onClick = { onSave(draft) },
            enabled = keyCheck !is KeyCheckState.Checking,
            colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("supervisor_save_api_key"),
        ) {
            if (keyCheck is KeyCheckState.Checking) {
                CircularProgressIndicator(color = Ivory.Strong, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text("שמור ואמת מפתח", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InlineNote(icon: ImageVector, text: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun AddChannelSection(
    channelInput: String,
    nameInput: String,
    descriptionInput: String,
    isAdding: Boolean,
    onChannelInputChange: (String) -> Unit,
    onNameInputChange: (String) -> Unit,
    onDescriptionInputChange: (String) -> Unit,
    onAddChannel: () -> Unit,
) {
    PanelCard(
        icon = Icons.Filled.Add,
        title = "הוספת ערוץ מאושר",
        subtitle = "קישור, @שם משתמש או מזהה UC",
    ) {
        PanelTextField(
            value = channelInput,
            onValueChange = onChannelInputChange,
            label = "ערוץ להוספה",
            placeholder = "@Hidabroot או קישור מלא לערוץ",
            modifier = Modifier.testTag("add_channel_id_input"),
        )
        PanelTextField(
            value = nameInput,
            onValueChange = onNameInputChange,
            label = "שם מותאם אישית (אופציונלי)",
            placeholder = "למשל: שיעורי הרב",
            modifier = Modifier.testTag("add_channel_name_input"),
        )
        PanelTextField(
            value = descriptionInput,
            onValueChange = onDescriptionInputChange,
            label = "תיאור קצר (אופציונלי)",
            placeholder = "למשל: הלכה יומית",
        )
        Button(
            onClick = onAddChannel,
            enabled = !isAdding,
            colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_channel_button"),
        ) {
            if (isAdding) {
                CircularProgressIndicator(color = Ivory.Strong, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("מאתר את הערוץ...", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("אישור והוספה לרשימה", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ManageChannelsSection(
    channels: List<ApprovedChannel>,
    onDelete: (ApprovedChannel) -> Unit,
    onRefreshMetadata: () -> Unit,
) {
    PanelCard(
        icon = Icons.Filled.PlaylistPlay,
        title = "ניהול ערוצים (${channels.size})",
        subtitle = "הסרה מיידית מהרשימה המאושרת",
    ) {
        if (channels.isEmpty()) {
            Text("אין ערוצים ברשימה.", style = MaterialTheme.typography.bodySmall, color = Ivory.Faint)
        } else {
            channels.forEach { channel ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Ink.Base)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelAvatar(channel, size = 38.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            channel.customName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Ivory.Strong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            channel.channelId,
                            style = MaterialTheme.typography.labelSmall,
                            color = Ivory.Faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { onDelete(channel) },
                        modifier = Modifier.testTag("delete_channel_${channel.channelId}"),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "מחק", tint = Accent.Danger)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onRefreshMetadata,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, tint = Accent.PrimarySoft, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("רענן שמות ותמונות ערוצים", color = Accent.PrimarySoft, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PinSection(
    newPin: String,
    confirmPin: String,
    onNewPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onChangePin: () -> Unit,
) {
    PanelCard(
        icon = Icons.Filled.AdminPanelSettings,
        title = "קוד מפקח",
        subtitle = "לפחות 4 ספרות",
        accent = Accent.Gold,
    ) {
        PanelTextField(
            value = newPin,
            onValueChange = onNewPinChange,
            label = "קוד חדש",
            isPassword = true,
            isNumeric = true,
            modifier = Modifier.testTag("new_pin_input"),
        )
        PanelTextField(
            value = confirmPin,
            onValueChange = onConfirmPinChange,
            label = "אימות קוד חדש",
            isPassword = true,
            isNumeric = true,
            modifier = Modifier.testTag("confirm_pin_input"),
        )
        Button(
            onClick = onChangePin,
            colors = ButtonDefaults.buttonColors(containerColor = Accent.Gold, contentColor = Ink.Base),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_new_pin_button"),
        ) {
            Text("שמור קוד חדש", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MaintenanceSection(onClearHistory: () -> Unit) {
    PanelCard(
        icon = Icons.Filled.DeleteSweep,
        title = "תחזוקה",
        subtitle = "פעולות מקומיות במכשיר",
        accent = Accent.Danger,
    ) {
        OutlinedButton(
            onClick = onClearHistory,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_history_button"),
        ) {
            Text("נקה היסטוריית צפייה", color = Accent.Danger, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Line.Soft, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text(
                text = "הנגן חוסם חיפוש, תגובות, המלצות ומעבר לאפליקציית יוטיוב. אפשר לצפות אך ורק בערוצים שברשימה.",
                style = MaterialTheme.typography.bodySmall,
                color = Ivory.Faint,
            )
        }
    }
}
