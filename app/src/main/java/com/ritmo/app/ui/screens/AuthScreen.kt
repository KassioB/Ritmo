package com.ritmo.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.AuthFailure
import com.ritmo.app.domain.Credentials
import com.ritmo.app.domain.Mood
import com.ritmo.app.ui.UiState
import com.ritmo.app.ui.components.ChunkyButton
import com.ritmo.app.ui.components.Mascot
import com.ritmo.app.ui.theme.Ritmo

private const val MAX_NAME = 60
private const val MAX_EMAIL = 120
private const val MAX_PASSWORD = 128

private val FieldShape = RoundedCornerShape(16.dp)
private val HeroShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

private enum class AuthMode(
    val title: String,
    val subtitle: String,
    val action: String,
    val switchQuestion: String,
    val switchAction: String,
) {
    SIGN_IN("Que bom te ver", "Entre para continuar seu ritmo.", "Entrar", "Não tem conta?", "Criar conta"),
    SIGN_UP("Crie sua conta", "Seu progresso fica vinculado a ela.", "Criar conta", "Já tem conta?", "Entrar"),
    RESET("Esqueci minha senha", "Digite o e-mail da conta para receber um link de senha nova.", "Enviar link", "Lembrou a senha?", "Entrar"),
}

/**
 * Abre direto no login, como a maioria dos apps: "Criar conta" e o Google ficam na mesma tela, logo
 * abaixo. Os motivos de recusa (senha errada, e-mail já usado) só aparecem depois de enviar.
 */
@Composable
fun AuthScreen(
    state: UiState,
    onSignUp: (name: String, email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onSignIn: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onReset: (email: String, onResult: (AuthFailure?) -> Unit) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    BackHandler(enabled = mode != AuthMode.SIGN_IN) { mode = AuthMode.SIGN_IN }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Hero(state)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AuthForm(
                mode = mode,
                onSignUp = onSignUp,
                onSignIn = onSignIn,
                onReset = onReset,
                onModeChange = { mode = it },
            )
        }
    }
}

/**
 * O topo usa a cor do cabeçalho do tema, que é o que decide a cor dos ícones da barra de status;
 * assim eles continuam legíveis em qualquer tema, claro ou escuro.
 */
@Composable
private fun Hero(state: UiState) {
    val brand = Ritmo.brand
    Column(
        Modifier
            .fillMaxWidth()
            .background(brand.header, HeroShape)
            .statusBarsPadding()
            .padding(top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Quem está começando ainda não tem streak, e a fase 0 é o pet apagado.
        Mascot(state.petId, maxOf(state.stage.index, 1), Mood.EXCITED, sizeDp = 96.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Ritmo",
            color = brand.onHeader,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            "Construa seu ritmo, um dia de cada vez",
            color = brand.onHeader.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ColumnScope.AuthForm(
    mode: AuthMode,
    onSignUp: (name: String, email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onSignIn: (email: String, password: String, onFailure: (AuthFailure) -> Unit) -> Unit,
    onReset: (email: String, onResult: (AuthFailure?) -> Unit) -> Unit,
    onModeChange: (AuthMode) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    // Fica de fora do rememberSaveable de propósito: a senha não deve ir para o estado salvo do sistema.
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var attempted by rememberSaveable(mode) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember(mode) { mutableStateOf<String?>(null) }
    var googleNotice by remember(mode) { mutableStateOf(false) }
    var sentTo by remember(mode) { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    LaunchedEffect(mode) { password = "" }

    val nameProblem = Credentials.nameProblem(name).takeIf { mode == AuthMode.SIGN_UP }
    val emailProblem = Credentials.emailProblem(email)
    val passwordProblem = when (mode) {
        AuthMode.SIGN_IN -> "Digite sua senha.".takeIf { password.isEmpty() }
        AuthMode.SIGN_UP -> Credentials.newPasswordProblem(password)
        AuthMode.RESET -> null
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
            AuthMode.RESET -> onReset(email) { result ->
                busy = false
                if (result == null) sentTo = Credentials.normalize(email) else failure = result.message
            }
            AuthMode.SIGN_IN -> onSignIn(email, password, onFailure)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(mode.title, color = scheme.onSurface, style = MaterialTheme.typography.headlineSmall)
        Text(mode.subtitle, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }

    if (mode == AuthMode.SIGN_UP) {
        AuthField(
            value = name,
            onValueChange = { name = it.take(MAX_NAME) },
            label = "Nome",
            icon = Icons.Rounded.Person,
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
        icon = Icons.Rounded.Mail,
        problem = emailProblem.takeIf { attempted },
        autofill = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = if (mode == AuthMode.RESET) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
    )
    // O link de senha esquecida fica colado ao campo, fora do espaçamento da coluna.
    if (mode != AuthMode.RESET) Column {
        AuthField(
            value = password,
            onValueChange = { password = it.take(MAX_PASSWORD) },
            label = "Senha",
            icon = Icons.Rounded.Lock,
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
        if (mode == AuthMode.SIGN_IN) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { onModeChange(AuthMode.RESET) }) { Text("Esqueci minha senha") }
            }
        }
    }

    failure?.let { Message(it, scheme.error) }
    sentTo?.let {
        Message(
            "Se existir uma conta com $it, o link já está a caminho. Confira também a caixa de spam.",
            scheme.onSurface,
        )
    }
    ChunkyButton(if (busy) "Aguarde..." else mode.action, ::submit, Modifier.fillMaxWidth(), enabled = !busy)

    if (mode != AuthMode.RESET) {
        OrDivider(if (mode == AuthMode.SIGN_UP) "ou cadastre com" else "ou entre com")
        GoogleButton { googleNotice = true }
        if (googleNotice) {
            Text(
                "O login com Google chega em breve. Por enquanto, use e-mail e senha.",
                Modifier.fillMaxWidth(),
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mode.switchQuestion, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = {
            onModeChange(if (mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
        }) { Text(mode.switchAction) }
    }
    Text(
        "Sua conta e seu progresso ficam salvos na nuvem.",
        Modifier.fillMaxWidth(),
        color = scheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
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
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        shape = FieldShape,
        isError = problem != null,
        supportingText = problem?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = autofill },
    )
}

@Composable
private fun OrDivider(text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
        Text(
            text,
            Modifier.padding(horizontal = 12.dp),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider(Modifier.weight(1f), color = scheme.outlineVariant)
    }
}

/** Fica visível desde já para a tela não mudar quando o login com Google chegar. */
@Composable
private fun GoogleButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        shape = FieldShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        GoogleMark(20.dp)
        Spacer(Modifier.width(10.dp))
        Text("Google", color = scheme.onSurface, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text("em breve", color = scheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

/** O "G" de quatro cores, desenhado em arcos para não depender de imagem. */
@Composable
private fun GoogleMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.22f
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(inset, inset)
        fun arc(color: Color, start: Float, sweep: Float) = drawArc(
            color, start, sweep, useCenter = false, topLeft = topLeft, size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Butt),
        )
        arc(Color(0xFFEA4335), 200f, 115f)
        arc(Color(0xFFFBBC05), 145f, 55f)
        arc(Color(0xFF34A853), 45f, 100f)
        arc(Color(0xFF4285F4), 0f, 45f)
        drawRect(
            Color(0xFF4285F4),
            topLeft = Offset(this.size.width / 2, this.size.height / 2 - stroke / 2),
            size = Size(this.size.width / 2 - inset + stroke / 2, stroke),
        )
    }
}

@Composable
private fun Message(text: String, color: Color) {
    Text(
        text,
        Modifier.fillMaxWidth(),
        color = color,
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
}
