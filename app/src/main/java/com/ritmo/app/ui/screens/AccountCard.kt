package com.ritmo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ritmo.app.data.Account
import com.ritmo.app.data.AuthFailure
import com.ritmo.app.domain.Credentials
import com.ritmo.app.ui.components.GroupCard

/** O que dá para fazer com a conta conectada. Cada ação devolve o motivo da falha, ou nulo. */
class AccountActions(
    val rename: (name: String, onResult: (AuthFailure?) -> Unit) -> Unit,
    val changePassword: (current: String, new: String, onResult: (AuthFailure?) -> Unit) -> Unit,
    val signOut: (onFailure: (AuthFailure) -> Unit) -> Unit,
    val delete: (password: String, onResult: (AuthFailure?) -> Unit) -> Unit,
)

private enum class AccountDialog { RENAME, PASSWORD, SIGN_OUT, DELETE }

@Composable
fun AccountCard(account: Account, actions: AccountActions) {
    var open by rememberSaveable { mutableStateOf<AccountDialog?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scheme = MaterialTheme.colorScheme

    GroupCard(title = "Conta") {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(account.name, style = MaterialTheme.typography.titleSmall)
            Text(account.email, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(
                "Seu progresso fica salvo na conta e volta sempre que você entrar.",
                Modifier.padding(top = 6.dp),
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            notice?.let {
                Text(it, Modifier.padding(top = 6.dp), color = scheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp), color = scheme.outlineVariant)
        ActionRow(Icons.Rounded.Edit, "Editar nome") { open = AccountDialog.RENAME }
        ActionRow(Icons.Rounded.Key, "Trocar senha") { open = AccountDialog.PASSWORD }
        ActionRow(Icons.AutoMirrored.Rounded.Logout, "Sair") { open = AccountDialog.SIGN_OUT }
        ActionRow(Icons.Rounded.DeleteForever, "Excluir conta", scheme.error) { open = AccountDialog.DELETE }
    }

    val close = { open = null }
    when (open) {
        AccountDialog.RENAME -> RenameDialog(account.name, actions, close) { notice = "Nome atualizado." }
        AccountDialog.PASSWORD -> PasswordDialog(actions, close) { notice = "Senha trocada." }
        AccountDialog.SIGN_OUT -> SignOutDialog(actions, close)
        AccountDialog.DELETE -> DeleteDialog(actions, close)
        null -> Unit
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    TextButton(onClick = onClick, Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
            Icon(icon, contentDescription = null, tint = tint)
            Text(text, Modifier.padding(start = 12.dp), color = tint, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Molde das janelas da conta: mostra o erro do servidor, troca o botão por "Aguarde..." enquanto
 * espera e só fecha quando dá certo.
 */
@Composable
private fun AccountForm(
    title: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (done: (AuthFailure?) -> Unit) -> Unit,
    valid: Boolean = true,
    destructive: Boolean = false,
    onSuccess: () -> Unit = onDismiss,
    content: @Composable () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
                failure?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = {
                    busy = true
                    failure = null
                    onConfirm { result ->
                        busy = false
                        if (result == null) onSuccess() else failure = result.message
                    }
                },
            ) {
                Text(
                    if (busy) "Aguarde..." else confirm,
                    color = if (destructive && !busy && valid) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancelar") } },
    )
}

@Composable
private fun RenameDialog(current: String, actions: AccountActions, onDismiss: () -> Unit, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(current) }
    AccountForm(
        title = "Editar nome",
        confirm = "Salvar",
        onDismiss = onDismiss,
        valid = Credentials.nameProblem(name) == null && name.trim() != current,
        onConfirm = { done -> actions.rename(name, done) },
        onSuccess = {
            onDone()
            onDismiss()
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            label = { Text("Nome") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PasswordDialog(actions: AccountActions, onDismiss: () -> Unit, onDone: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    val problem = Credentials.newPasswordProblem(new).takeIf { new.isNotEmpty() }
    AccountForm(
        title = "Trocar senha",
        confirm = "Salvar",
        onDismiss = onDismiss,
        valid = current.isNotEmpty() && new.isNotEmpty() && problem == null,
        onConfirm = { done -> actions.changePassword(current, new, done) },
        onSuccess = {
            onDone()
            onDismiss()
        },
    ) {
        SecretField(current, { current = it }, "Senha atual")
        SecretField(new, { new = it }, "Senha nova", problem)
    }
}

@Composable
private fun SignOutDialog(actions: AccountActions, onDismiss: () -> Unit) {
    AccountForm(
        title = "Sair da conta?",
        confirm = "Sair",
        onDismiss = onDismiss,
        // Ao dar certo, a tela inteira troca para o login; não há janela para fechar.
        onConfirm = { done -> actions.signOut { done(it) } },
        onSuccess = {},
    ) {
        Text(
            "Seu progresso fica salvo na conta e sai deste aparelho junto com você. " +
                "Os lembretes param até você entrar de novo.",
        )
    }
}

@Composable
private fun DeleteDialog(actions: AccountActions, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AccountForm(
        title = "Excluir a conta?",
        confirm = "Excluir para sempre",
        onDismiss = onDismiss,
        valid = password.isNotEmpty(),
        destructive = true,
        onConfirm = { done -> actions.delete(password, done) },
        onSuccess = {},
    ) {
        Text(
            "A conta e todo o progresso guardado nela (objetivos, histórico, moedas, compras e listas) " +
                "serão apagados e não dá para recuperar. Digite sua senha para confirmar.",
        )
        SecretField(password, { password = it }, "Senha")
    }
}

@Composable
private fun SecretField(value: String, onChange: (String) -> Unit, label: String, problem: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(128)) },
        label = { Text(label) },
        singleLine = true,
        isError = problem != null,
        supportingText = problem?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}
