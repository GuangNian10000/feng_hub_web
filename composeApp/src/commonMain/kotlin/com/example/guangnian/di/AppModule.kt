package com.example.guangnian.di

import com.example.guangnian.feature.console.data.api.*
import com.example.guangnian.feature.console.viewmodel.*
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.dsl.module

val appModule = module {
    // Data Sources
    single<RobotTcpApi> { RobotTcpApiImpl() }
    
    // Scoped API Interfaces for better isolation
    single<RobotStatusApi> { get<RobotTcpApi>() }
    single<MotionControlApi> { get<RobotTcpApi>() }
    single<NavigationApi> { get<RobotTcpApi>() }
    single<AgentManagerApi> { get<RobotTcpApi>() }
    single<KnowledgeApi> { get<RobotTcpApi>() }
    single<TeachApi> { get<RobotTcpApi>() }
    single<RouteApi> { get<RobotTcpApi>() }
    single<VoiceApi> { get<RobotTcpApi>() }

    // Core Utilities
    single { Settings() }
    single { 
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 5000
            }
        }
    }
    
    // ViewModels
    factory { RobotViewModel(get()) }
    factory { ActionViewModel(get(), get(), get()) }
    factory { MapViewModel(get(), get(), get()) }
    factory { AgentViewModel(get()) }
    factory { KnowledgeViewModel(get()) }
}
