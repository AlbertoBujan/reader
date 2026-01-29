package com.boaxente.riffle.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.boaxente.riffle.data.remote.AuthManager
import com.boaxente.riffle.util.RiffleLogger
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by authManager.currentUser.collectAsState()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Welcome to Riffle",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Sync your feeds across devices with Google Sign-In",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                RiffleLogger.log("Iniciando login con Credential Manager")
                                val googleIdOption = authManager.getGoogleIdOption()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context
                                )

                                val credential = result.credential
                                if (credential is CustomCredential && 
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    
                                    try {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        val tokenPreview = idToken.take(10)
                                        android.util.Log.d("AuthDebug", "Token prefix: $tokenPreview")
                                        RiffleLogger.log("Recibido token que empieza por: $tokenPreview")
                                        
                                        authManager.signInWithGoogle(idToken)
                                    } catch (e: Exception) {
                                        RiffleLogger.log("Error en Firebase signIn: ${e.message}")
                                        if (e.message?.contains("stale", ignoreCase = true) == true) {
                                            RiffleLogger.recordException(Exception("Error Stale Token tras limpieza de caché", e))
                                        } else {
                                            RiffleLogger.recordException(e)
                                        }
                                        snackbarHostState.showSnackbar("Login Failed: ${e.message}")
                                    }
                                } else {
                                    RiffleLogger.recordException(Exception("Unexpected credential type: ${credential.type}"))
                                    snackbarHostState.showSnackbar("Login Failed: Unexpected error")
                                }
                            } catch (e: NoCredentialException) {
                                android.util.Log.d("AuthDebug", "No se encontraron credenciales para auto-login")
                            } catch (e: GetCredentialException) {
                                // User cancelled or no credentials available usually doesn't need to be logged as error for user
                                RiffleLogger.recordException(e)
                                if (e.message?.contains("User cancelled") != true) { // Rough check, depends on exception type
                                     snackbarHostState.showSnackbar("Sign in error: ${e.message}")
                                }
                            } catch (e: Exception) {
                                RiffleLogger.recordException(e)
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkip
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}
