package com.bernardo.feedvault.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bernardo.feedvault.ui.AppUiState
import com.bernardo.feedvault.ui.GalleryViewModel
import javax.crypto.Cipher

/**
 * Full-screen gate shown when the Cofre is selected but locked. Handles first-run
 * password setup and unlock (password or biometric). Once unlocked the gallery
 * underneath shows the encrypted mirror, so this composable is dismissed.
 */
@Composable
fun VaultGate(
    state: AppUiState,
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!state.vaultInitialized) VaultSetup(viewModel) else VaultUnlock(state, viewModel, context)

        if (state.vaultBusy) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    state.vaultBusyMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultSetup(viewModel: GalleryViewModel) {
    var pw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Shield, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Criar o Cofre", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Defina uma senha para criptografar a mídia. Ela não pode ser recuperada — se esquecer a senha, os arquivos do cofre são perdidos para sempre.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pw, onValueChange = { pw = it }, label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar senha") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (pw != confirm) viewModel.setError("As senhas não coincidem")
                else viewModel.vaultSetupPassword(pw)
            },
            enabled = pw.isNotBlank() && confirm.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Criar cofre") }
    }
}

@Composable
private fun VaultUnlock(state: AppUiState, viewModel: GalleryViewModel, context: Context) {
    var pw by remember { mutableStateOf("") }
    val canBiometric = state.vaultBiometricEnabled && biometricAvailable(context)

    LaunchedEffect(canBiometric) {
        if (canBiometric) tryBiometricUnlock(context, viewModel)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(16.dp))
        Text("Cofre bloqueado", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pw, onValueChange = { pw = it }, label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.vaultUnlockPassword(pw) },
            enabled = pw.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Desbloquear") }
        if (canBiometric) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { tryBiometricUnlock(context, viewModel) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Usar biometria")
            }
        }
    }
}

// ── Biometric helpers ────────────────────────────────────────────────────────

fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

private fun tryBiometricUnlock(context: Context, viewModel: GalleryViewModel) {
    val activity = context as? FragmentActivity ?: return
    val cipher = viewModel.vaultBiometricDecryptCipher() ?: run {
        viewModel.setError("Biometria indisponível — use a senha"); return
    }
    authenticate(activity, "Desbloquear o cofre", cipher,
        onSuccess = { viewModel.vaultCompleteBiometricUnlock(it) }, onError = {})
}

fun enableVaultBiometric(context: Context, viewModel: GalleryViewModel) {
    val activity = context as? FragmentActivity ?: return
    val cipher = viewModel.vaultBiometricEncryptCipher() ?: run {
        viewModel.setError("Não foi possível preparar a biometria"); return
    }
    authenticate(activity, "Ativar desbloqueio por biometria", cipher,
        onSuccess = { viewModel.vaultCompleteEnableBiometric(it) },
        onError = { msg -> viewModel.setError(msg) })
}

private fun authenticate(
    activity: FragmentActivity,
    title: String,
    cipher: Cipher,
    onSuccess: (Cipher) -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            result.cryptoObject?.cipher?.let(onSuccess)
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) onError(errString.toString())
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setNegativeButtonText("Cancelar")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
}
