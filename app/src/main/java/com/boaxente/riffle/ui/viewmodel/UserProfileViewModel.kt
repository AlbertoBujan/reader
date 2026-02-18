package com.boaxente.riffle.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boaxente.riffle.data.model.UserInteraction
import com.boaxente.riffle.data.model.UserProfile
import com.boaxente.riffle.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val firestoreHelper: com.boaxente.riffle.data.remote.FirestoreHelper
) : ViewModel() {
    
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()
    
    private val _interactions = MutableStateFlow<List<UserInteraction>>(emptyList())
    val interactions: StateFlow<List<UserInteraction>> = _interactions.asStateFlow()
    
    val readStats = firestoreHelper.readCount
    val savedStats = firestoreHelper.savedCount
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()
    
    init {
        loadProfile()
        loadInteractions()
    }
    
    private fun loadProfile() {
        viewModelScope.launch {
            userProfileRepository.getCurrentUserProfile().collect { profile ->
                _profile.value = profile
            }
        }
    }
    
    private fun loadInteractions() {
        viewModelScope.launch {
            userProfileRepository.getUserInteractions().collect { interactions ->
                _interactions.value = interactions
            }
        }
    }
    
    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userProfileRepository.updateDisplayName(name)
            
            result.onSuccess {
                _updateSuccess.value = true
            }.onFailure { e ->
                _error.value = e.message
            }
            
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }
}
