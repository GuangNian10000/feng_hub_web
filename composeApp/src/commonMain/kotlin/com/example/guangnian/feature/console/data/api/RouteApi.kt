package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface RouteApi {
    suspend fun getRoutes(): List<RouteBean>
    suspend fun addRoute(route: RouteBean): BaseBean
    suspend fun updateRoute(route: RouteBean): BaseBean
    suspend fun deleteRoute(routeId: String): BaseBean
    suspend fun startRoute(routeId: String): BaseBean
    suspend fun getRouteStatus(): RouteStatusBean
}
