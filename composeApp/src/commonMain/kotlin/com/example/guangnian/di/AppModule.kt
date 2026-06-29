package com.example.guangnian.di

import com.example.guangnian.feature.console.data.api.RobotTcpApiImpl
import com.example.guangnian.feature.console.viewmodel.ActionViewModel
import com.example.guangnian.feature.console.viewmodel.MapViewModel
import com.example.guangnian.feature.main.viewmodel.RobotViewModel
import org.koin.dsl.module

val appModule = module {
    single { RobotTcpApiImpl() }
    
    factory { ActionViewModel(get()) }
    factory { MapViewModel(get()) }
    factory { RobotViewModel(get()) }
}
