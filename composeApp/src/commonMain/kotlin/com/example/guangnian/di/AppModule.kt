package com.example.guangnian.di

import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.viewmodel.ActionViewModel
import com.example.guangnian.feature.console.viewmodel.MapViewModel
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.dsl.module

val appModule = module {
    single { RobotTcpApiImpl() }
    single { Settings() }
    single { 
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 5000
            }
        }
    }
    
    factory { ActionViewModel(get()) }
    factory { MapViewModel(get(), get(), get()) }
    factory { RobotViewModel(get()) }
}
