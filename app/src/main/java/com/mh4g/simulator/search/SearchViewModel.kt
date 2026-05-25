package com.mh4g.simulator.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel : ViewModel() {

    private val engine = SearchEngine()

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress

    private var _condition = SearchCondition()
    val condition get() = _condition

    fun updateCondition(cond: SearchCondition) {
        _condition = cond
    }

    fun startSearch() {
        engine.cancel()
        _searchState.value = SearchState.Searching
        _progress.value = 0
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                engine.search(_condition) { prog ->
                    _progress.postValue(prog)
                }
            }
            _searchState.value = SearchState.Done(result)
        }
    }

    fun cancelSearch() {
        engine.cancel()
        _searchState.value = SearchState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        engine.cancel()
    }
}

sealed class SearchState {
    object Idle : SearchState()
    object Searching : SearchState()
    data class Done(val results: SearchResults) : SearchState()
    data class Error(val message: String) : SearchState()
}
