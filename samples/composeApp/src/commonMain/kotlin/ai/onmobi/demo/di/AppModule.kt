package ai.onmobi.demo.di

import ai.onmobi.demo.AudioRecorder
import ai.onmobi.demo.SpeechViewModel
import org.koin.dsl.module

val appModule = module {
    single { AudioRecorder() }
    single { SpeechViewModel(get()) }
}
