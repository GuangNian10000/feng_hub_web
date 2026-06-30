package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface RouteApi {
    suspend fun getRoutes(): List<RouteBean>
    suspend fun addRoute(route: RouteBean): BaseBean // 对应 route_add (Upsert)
    suspend fun updateRoute(route: RouteBean): BaseBean // 对应 route_add (Upsert)
    suspend fun deleteRoute(route_id: String): BaseBean // 对应 route_delete_by_rid
    suspend fun startRoute(route_id: String): BaseBean
    suspend fun getRouteStatus(): RouteStatusBean
}
