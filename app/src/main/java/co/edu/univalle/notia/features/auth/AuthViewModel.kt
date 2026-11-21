package co.edu.univalle.notia.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isLoggedIn: Boolean
        get() = repository.isLoggedIn

    fun login(email: String, password: String, onSuccess: () -> Unit) =
        run(onSuccess) { repository.login(email, password) }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) =
        run(onSuccess) { repository.register(name, email, password) }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit) =
        run(onSuccess) { repository.sendPasswordReset(email) }

    fun logout() = repository.logout()

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun run(onSuccess: () -> Unit, action: suspend () -> Result<Unit>) {
        if (_state.value.loading) return
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            action()
                .onSuccess {
                    _state.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = AuthUiState(error = e.message)
                }
        }
    }
}
