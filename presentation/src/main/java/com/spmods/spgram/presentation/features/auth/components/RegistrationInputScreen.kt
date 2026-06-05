package com.spmods.spgram.presentation.features.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun RegistrationInputScreen(
    termsText: String? = null,
    isLoading: Boolean = false,
    onRegister: (firstName: String, lastName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf("") }
    var lastName  by remember { mutableStateOf("") }
    val lastNameFocus = remember { FocusRequester() }

    Column(
        modifier            = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "Your Name",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text  = "Enter your name and add a profile photo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value         = firstName,
            onValueChange = { firstName = it },
            label         = { Text("First Name (required)") },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { lastNameFocus.requestFocus() }
            )
        )

        OutlinedTextField(
            value         = lastName,
            onValueChange = { lastName = it },
            label         = { Text("Last Name (optional)") },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .focusRequester(lastNameFocus),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction      = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (firstName.isNotBlank()) onRegister(firstName.trim(), lastName.trim())
                }
            )
        )

        if (!termsText.isNullOrBlank()) {
            Text(
                text  = termsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick  = { onRegister(firstName.trim(), lastName.trim()) },
            enabled  = firstName.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Please wait…" else "Done")
        }
    }
}
