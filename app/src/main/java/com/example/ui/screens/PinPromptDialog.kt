package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Accent
import com.example.ui.theme.Gradients
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

@Composable
fun PinPromptDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .clip(RoundedCornerShape(26.dp))
                .background(Ink.Surface)
                .border(1.dp, Line.Regular, RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Gradients.GoldSheen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Ink.Base, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text("אימות מפקח", style = MaterialTheme.typography.titleLarge, color = Ivory.Strong)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "הזן את קוד המפקח כדי לנהל ערוצים והגדרות. ברירת המחדל היא 1234 — מומלץ להחליף.",
                style = MaterialTheme.typography.bodySmall,
                color = Ivory.Muted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { character -> character.isDigit() }.take(12) },
                placeholder = { Text("• • • •", color = Ivory.Faint, textAlign = TextAlign.Center) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                isError = errorMessage != null,
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent.Gold,
                    unfocusedBorderColor = Line.Regular,
                    focusedTextColor = Ivory.Strong,
                    unfocusedTextColor = Ivory.Strong,
                    cursorColor = Accent.Gold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_dialog_input"),
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = Accent.Danger)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("ביטול", color = Ivory.Muted, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { onSubmit(pin) },
                    enabled = pin.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent.Gold, contentColor = Ink.Base),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pin_dialog_submit"),
                ) {
                    Text("אישור", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(0.dp))
        }
    }
}
