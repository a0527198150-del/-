package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.KosherBadge
import com.example.ui.theme.Accent
import com.example.ui.theme.Gradients
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

/**
 * First launch. The YouTube Data API key is requested here instead of being
 * baked into the build, and is verified against Google before it is stored.
 */
@Composable
fun OnboardingScreen(
    keyCheck: KeyCheckState,
    onSubmit: (key: String, verify: Boolean) -> Unit,
    onSkip: () -> Unit,
    onKeyEdited: () -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    val isChecking = keyCheck is KeyCheckState.Checking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gradients.Hero)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Gradients.GoldSheen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = Ink.Base,
                modifier = Modifier.size(42.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "ערוצים כשרים",
            style = MaterialTheme.typography.displaySmall,
            color = Ivory.Strong,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "סביבת צפייה מוגנת: רק ערוצים שאושרו על ידי המפקח, ללא חיפוש, ללא תגובות וללא המלצות.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ivory.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        KosherBadge("מצב מפקח מוגן בקוד")

        Spacer(Modifier.height(30.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Ink.Surface.copy(alpha = 0.92f))
                .border(1.dp, Line.Regular, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Accent.Primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = Accent.PrimarySoft, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("מפתח YouTube Data API", style = MaterialTheme.typography.titleMedium, color = Ivory.Strong)
                    Text("נשמר במכשיר בלבד", style = MaterialTheme.typography.labelSmall, color = Ivory.Faint)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "המפתח משמש לשליפת רשימת הסרטונים מהערוצים המאושרים. הוא נשמר מקומית במכשיר, ואפשר להחליף אותו בכל רגע דרך מצב מפקח.",
                style = MaterialTheme.typography.bodySmall,
                color = Ivory.Muted,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it.trim()
                    onKeyEdited()
                },
                label = { Text("הדבק כאן את המפתח") },
                placeholder = { Text("AIza...", color = Ivory.Faint) },
                singleLine = true,
                isError = keyCheck is KeyCheckState.Invalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_api_key_input"),
            )

            AnimatedVisibility(visible = keyCheck is KeyCheckState.Invalid) {
                val message = (keyCheck as? KeyCheckState.Invalid)?.message.orEmpty()
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Accent.Danger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = Accent.Danger)
                }
            }

            AnimatedVisibility(visible = keyCheck is KeyCheckState.Valid) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Accent.Success, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("המפתח אומת בהצלחה", style = MaterialTheme.typography.bodySmall, color = Accent.Success)
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onSubmit(apiKey, true) },
                enabled = apiKey.isNotBlank() && !isChecking,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent.Primary,
                    contentColor = Ivory.Strong,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_save_key"),
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = Ivory.Strong,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("בודק מול Google...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("אימות ושמירה", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onSkip,
                enabled = !isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_skip"),
            ) {
                Text("המשך ללא מפתח (מצב RSS מוגבל)", color = Ivory.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { showHelp = !showHelp }) {
            Text(
                text = if (showHelp) "הסתר הוראות" else "איך משיגים מפתח? (2 דקות)",
                color = Accent.Gold,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        AnimatedVisibility(visible = showHelp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Ink.Elevated)
                    .border(1.dp, Line.Soft, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HelpStep(1, "היכנס אל console.cloud.google.com והתחבר עם חשבון Google.")
                HelpStep(2, "צור פרויקט חדש (או בחר פרויקט קיים).")
                HelpStep(3, "בתפריט \"APIs & Services\" בחר \"Library\", חפש YouTube Data API v3 ולחץ Enable.")
                HelpStep(4, "עבור ל-\"Credentials\", לחץ \"Create credentials\" ובחר \"API key\".")
                HelpStep(5, "העתק את המפתח שנוצר והדבק אותו בשדה שלמעלה.")
                Text(
                    text = "השימוש ב-API חינמי במכסה יומית נדיבה. ללא מפתח האפליקציה עדיין עובדת, אך מציגה פחות סרטונים ובלי משך זמן.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ivory.Faint,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HelpStep(index: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Accent.Primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$index", style = MaterialTheme.typography.labelSmall, color = Accent.PrimarySoft)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ivory.Muted)
    }
}
