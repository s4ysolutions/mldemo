package solutions.s4y.mldemo.asr.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import solutions.s4y.mldemo.asr.service.ASRService
import solutions.s4y.mldemo.asr.service.AsrModels
import javax.inject.Inject

@HiltViewModel
class ASRViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val asrService: ASRService,
) : ViewModel() {
    fun asrSwitchModel(context: Context, model: AsrModels) {
        viewModelScope.launch {
            asrService.switchModel(context, model)
        }
    }

    fun asrStart(context: Context) {
        asrService.startASR(context, viewModelScope)
    }

    fun asrStop() {
        asrService.stopASR()
    }
}