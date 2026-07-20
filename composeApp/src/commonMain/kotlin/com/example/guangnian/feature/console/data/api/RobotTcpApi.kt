package com.example.guangnian.feature.console.data.api

import com.example.guangnian.feature.teach.data.api.TeachApi

interface RobotTcpApi : 
    VoiceApi, 
    RouteApi, 
    AgentManagerApi, 
    KnowledgeApi, 
    MotionControlApi, 
    NavigationApi, 
    RobotStatusApi,
    TeachApi,
    com.example.guangnian.feature.face.data.api.FaceApi
