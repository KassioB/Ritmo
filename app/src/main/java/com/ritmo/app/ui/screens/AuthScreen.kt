package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Account
import com.ritmo.app.data.AuthFailure
import com.ritmo.app.domain.Credentials
import com.ritmo.app.domain.Mood
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyButton
import com.ritmo.app.ui.components.GroupCard
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.components.PageTopBar
import com.ritmo.app.ui.components.SectionBanner
import com.ritmo.app.ui.theme.Ritmo

private const val MAX_NAME = 60
private const val MAX_EMAIL = 120
private const val MAX_PASSWORD = 128

private enum class AuthMode(val overline: String, val title: String, val action: String = "") {
    WELCOME("Bem-vindo ao Ritmo", "Construa seu ritmo, um dia de cada vez"),
    SIGN_IN("Que bom te ver", "Entre na sua conta", "Entrar"),
    SIGN_UP("Vamos começar", "Crie sua conta para vincular seu progresso", "Criar conta"),
    RESET("Esqueci minha senha", "Escolha uma senha nova", "Salvar senha"),
}

/** Começa nas opções de entrada; de lá, cada uma abre o próprio formulário. */
@Composable
fun AuthScreen(
    state: UiState,
    onSignUp: (name: String, email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onSignIn: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onReset: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.WELCOME) }
    val back = when (mode) {
        AuthMode.WELCOME -> null
        AuthMode.RESET -> AuthMode.SIGN_IN
        else -> AuthMode.WELCOME
    }
    BackHandler(enabled = back != null) { back?.let { mode = it } }

    Column(Modifier.fillMaxSize()) {
        if (back != null) {
            PageTopBar("Ritmo", Icons.AutoMirrored.Rounded.ArrowBack, "Voltar", { mode = back })
        } else {
            // A faixa escura mantém os ícones da barra de status legíveis, como no resto do app.
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Ritmo.brand.header)
                    .statusBarsPadding()
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Ritmo", color = Ritmo.brand.onHeader, style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionBanner(
                overline = mode.overline,
                title = mode.title,
                // Quem está começando ainda não tem streak, e a fase 0 é o pet apagado.
                side = { Mascot(state.petId, maxOf(state.stage.index, 1), Mood.EXCITED, sizeDp = 84.dp) },
            )
            if (mode == AuthMode.WELCOME) {
                WelcomeOptions(state.account) { mode = it }
            } else {
                AuthForm(mode, state.account, onSignUp, onSignIn, onReset) { mode = AuthMode.RESET }
            }
        }
    }
}

/**
 * Enquanto a conta for só do aparelho, "Entrar" precisa de uma conta aqui e "Criar conta" precisa
 * que não exista nenhuma. As duas opções ficam sempre visíveis, e o motivo aparece antes de a pessoa
 * preencher um formulário que não iria funcionar.
 */
@Composable
private fun ColumnScope.WelcomeOptions(account: Account?, onOpen: (AuthMode) -> Unit) {
    var failure by remember { mutableStateOf<String?>(null) }
    OptionButton("Entrar", highlighted = account != null) {
        if (account == null) failure = AuthFailure.NO_ACCOUNT.message else onOpen(AuthMode.SIGN_IN)
    }
    OptionButton("Criar conta", highlighted = account == null) {
        if (account != null) failure = AuthFailure.ALREADY_HAS_ACCOUNT.message else onOpen(AuthMode.SIGN_UP)
    }
    ChunkyButton("Continuar com Google", {}, Modifier.fillMaxWidth(), enabled = false)
    Note("Login com Google em breve.")
    failure?.let { FailureText(it) }
    Note("Sua conta fica guardada neste aparelho.")
}

/** O destaque fica na opção que serve para este aparelho agora. */
@Composable
private fun OptionButton(text: String, highlighted: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    if (highlighted) {
        ChunkyButton(text, onClick, Modifier.fillMaxWidth())
    } else {
        ChunkyButton(
            text,
            onClick,
            Modifier.fillMaxWidth(),
            color = scheme.surfaceContainerHigh,
            lip = scheme.outline,
            contentColor = scheme.onSurface,
        )
    }
}

@Composable
private fun ColumnScope.AuthForm(
    mode: AuthMode,
    account: Account?,
    onSignUp: (name: String, email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onSignIn: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onReset: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onForgotPassword: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Fica de fora do rememberSaveable de propósito: a senha não deve ir para o estado salvo do sistema.
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var attempted by rememberSaveable(mode) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember(mode) { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    LaunchedEffect(mode) { password = "" }

    val nameProblem = Credentials.nameProblem(name).takeIf { mode == AuthMode.SIGN_UP }
    val emailProblem = Credentials.emailProblem(email)
    val passwordProblem = when (mode) {
        AuthMode.SIGN_IN -> "Digite sua senha.".takeIf { password.isEmpty() }
        else -> Credentials.newPasswordProblem(password)
    }

    fun submit() {
        attempted = true
        failure = null
        if (busy || nameProblem != null || emailProblem != null || passwordProblem != null) return
        focus.clearFocus()
        busy = true
        val onFailure: (AuthFailure) -> Unit = {
            busy = false
            failure = it.message
        }
        when (mode) {
            AuthMode.SIGN_UP -> onSignUp(name, email, password, onFailure)
            AuthMode.RESET -> onReset(email, password, onFailure)
            else -> onSignIn(email, password, onFailure)
        }
    }

    if (mode == AuthMode.RESET && account != null) {
        Note(
            "A conta fica só neste aparelho, então não dá para enviar um e-mail de recuperação. " +
                "Confirme o e-mail da conta (${Credentials.masked(account.email)}) e escolha uma " +
                "senha nova. Seus dados continuam aqui.",
        )
    }
    GroupCard {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (mode == AuthMode.SIGN_UP) {
                AuthField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME) },
                    label = "Nome",
                    problem = nameProblem.takeIf { attempted },
                    autofill = ContentType.PersonFullName,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            AuthField(
                value = email,
                onValueChange = { email = it.take(MAX_EMAIL) },
                label = "E-mail",
                problem = emailProblem.takeIf { attempted },
                autofill = ContentType.EmailAddress,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            AuthField(
                value = password,
                onValueChange = { password = it.take(MAX_PASSWORD) },
                label = if (mode == AuthMode.RESET) "Senha nova" else "Senha",
                problem = passwordProblem.takeIf { attempted },
                autofill = if (mode == AuthMode.SIGN_IN) ContentType.Password else ContentType.NewPassword,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (showPassword) "Esconder senha" else "Mostrar senha",
                        )
                    }
                },
            )
        }
    }
    failure?.let { FailureText(it) }
    ChunkyButton(if (busy) "Aguarde..." else mode.action, ::submit, Modifier.fillMaxWidth(), enabled = !busy)
    when (mode) {
        AuthMode.SIGN_UP -> Note("Sua conta fica guardada neste aparelho. Se você já usa o Ritmo aqui, seu progresso continua igual.")
        AuthMode.SIGN_IN -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onForgotPassword) { Text("Esqueci minha senha") }
        }
        else -> Unit
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    problem: String?,
    autofill: ContentType,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = problem != null,
        supportingText = problem?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = Modifier.fillMaxWidth().semantics { contentType = autofill },
    )
}

@Composable
private fun FailureText(message: String) {
    Text(
        message,
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}
