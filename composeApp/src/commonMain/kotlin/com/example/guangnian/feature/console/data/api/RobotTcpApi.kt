package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.motion.data.api.ActionEditingApi
import com.example.guangnian.feature.teach.data.api.TeachApi

interface RobotTcpApi : 
    VoiceApi, 
    RouteApi, 
    AgentManagerApi, 
    AgentControlApi,
    KnowledgeApi, 
    MotionControlApi, 
    NavigationApi, 
    RobotStatusApi,
    TeachApi,
    ActionEditingApi,
    SystemSettingsApi,
    com.example.guangnian.feature.face.data.api.FaceApi
