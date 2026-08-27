package com.example.guangnian.feature.console.viewmodel

import com.example.guangnian.feature.console.data.bean.Point2DBean
import com.example.guangnian.feature.console.data.bean.RobotPositionBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapUiStateTest {

    @Test
    fun recordsEverySuccessfulPositionWhileMapping() {
        val state = MapUiState()
            .withMappingState(true)
            .withRobotPosition(RobotPositionBean(x = 1.0, y = 2.0))
            .withRobotPosition(RobotPositionBean(x = 1.5, y = 2.5))

        assertEquals(
            listOf(Point2DBean(1.0, 2.0), Point2DBean(1.5, 2.5)),
            state.mappingTrajectory
        )
    }

    @Test
    fun doesNotRecordPositionsOutsideMapping() {
        val position = RobotPositionBean(x = 1.0, y = 2.0)
        val state = MapUiState().withRobotPosition(position)

        assertEquals(position, state.robotPosition)
        assertTrue(state.mappingTrajectory.isEmpty())
    }

    @Test
    fun clearsTrajectoryWhenMappingEnds() {
        val state = MapUiState()
            .withMappingState(true)
            .withRobotPosition(RobotPositionBean(x = 1.0, y = 2.0))
            .withMappingState(false)

        assertFalse(state.isMapping)
        assertTrue(state.mappingTrajectory.isEmpty())
    }

    @Test
    fun startsEachMappingSessionWithAnEmptyTrajectory() {
        val staleState = MapUiState(
            isMapping = false,
            mappingTrajectory = listOf(Point2DBean(9.0, 9.0))
        )

        val state = staleState.withMappingState(true)

        assertTrue(state.isMapping)
        assertTrue(state.mappingTrajectory.isEmpty())
    }

    @Test
    fun repeatedMappingStatusDoesNotClearCurrentTrajectory() {
        val state = MapUiState()
            .withMappingState(true)
            .withRobotPosition(RobotPositionBean(x = 1.0, y = 2.0))
            .withMappingState(true)

        assertEquals(listOf(Point2DBean(1.0, 2.0)), state.mappingTrajectory)
    }
}
