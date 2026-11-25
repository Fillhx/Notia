package co.edu.univalle.notia.features.organizations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrganizationsUiState(
    val loading: Boolean = false,
    val organizations: List<OrganizationMembership> = emptyList(),
    val listError: String? = null,
    val joinLoading: Boolean = false,
    val joinError: String? = null,
    val createLoading: Boolean = false,
    val createError: String? = null,
    val created: OrganizationMembership? = null
)

class OrganizationsViewModel : ViewModel() {

    private val repository = OrganizationRepository()

    private val _state = MutableStateFlow(OrganizationsUiState())
    val state: StateFlow<OrganizationsUiState> = _state.asStateFlow()

    fun load() {
        _state.value = _state.value.copy(loading = true, listError = null)
        viewModelScope.launch {
            repository.myOrganizations()
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        loading = false,
                        organizations = list,
                        listError = null
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        loading = false,
                        listError = e.message
                    )
                }
        }
    }

    fun join(code: String, onSuccess: () -> Unit) {
        if (_state.value.joinLoading) return
        _state.value = _state.value.copy(joinLoading = true, joinError = null)
        viewModelScope.launch {
            repository.joinWithCode(code)
                .onSuccess {
                    _state.value = _state.value.copy(joinLoading = false, joinError = null)
                    load()
                    onSuccess()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        joinLoading = false,
                        joinError = e.message
                    )
                }
        }
    }

    fun create(name: String) {
        if (_state.value.createLoading) return
        _state.value = _state.value.copy(createLoading = true, createError = null)
        viewModelScope.launch {
            repository.createOrganization(name)
                .onSuccess { newOrg ->
                    _state.value = _state.value.copy(
                        createLoading = false,
                        createError = null,
                        created = newOrg
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        createLoading = false,
                        createError = e.message
                    )
                }
        }
    }

    fun dismissCreated() {
        _state.value = _state.value.copy(created = null)
        load()
    }

    fun clearJoinError() {
        _state.value = _state.value.copy(joinError = null)
    }

    fun clearCreateError() {
        _state.value = _state.value.copy(createError = null)
    }
}
