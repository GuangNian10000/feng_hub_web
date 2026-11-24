package com.example.guangnian.di

import com.example.guangnian.core.event.RobotEventBus
import com.example.guangnian.core.network.createPlatformHttpClient
import com.example.guangnian.core.network.ws.FrameworkManager
import com.example.guangnian.core.network.ws.TabletWsClient
import com.example.guangnian.core.network.ws.TabletWsDispatcher
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val appModule = module {
    // Core Utilities & Event Bus
    single { RobotEventBus() }
    single { Settings() }

    // Core Network Clients
    single { createPlatformHttpClient() }

    // Framework & WS Management
    single { FrameworkManager(get()) }
    single { TabletWsClient(get()) }
    single(createdAtStart = true) { TabletWsDispatcher(get()) }
}
