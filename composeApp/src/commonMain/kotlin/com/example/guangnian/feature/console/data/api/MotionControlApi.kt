package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.console.data.bean.*

interface MotionControlApi {
    suspend fun getPostureList(): List<PostureBean>
    suspend fun getArmList(): List<ArmActionBean>
    suspend fun getMotionList(): List<MotionBean>
    suspend fun executePosture(msg: Map<String, String>)
    suspend fun executePosture(postureName: String)
    suspend fun executeMotion(motionId: String)
    suspend fun executeSport(sport: String)
    suspend fun executeArm(armId: Int)
    suspend fun moveTranslate(angle: Double, level: Int)
    suspend fun moveRotate(level: Int)
    suspend fun moveMixed(angle: Double, translateLevel: Int, rotateLevel: Int)
    suspend fun stopTranslate()
    suspend fun stopRotate()
    suspend fun stopAll()
    suspend fun recover()
    suspend fun getEmergency()
}
