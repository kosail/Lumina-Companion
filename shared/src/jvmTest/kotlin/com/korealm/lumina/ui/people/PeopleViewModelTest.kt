package com.korealm.lumina.ui.people

import com.korealm.lumina.data.FakeEnrollmentRepository
import com.korealm.lumina.data.FakePeopleRepository
import com.korealm.lumina.data.PeopleSnapshot
import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollPhase
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.ui.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wiring tests for [PeopleViewModel]: telemetry collection, refresh, the enrollment lifecycle and
 * cancel — all against fakes, with virtual time.
 *
 * Lives in `jvmTest` because `Dispatchers.setMain` (needed for `viewModelScope`) is a JVM/Android
 * test utility; the pure reductions are covered in `commonTest` by `PeopleReducerTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun collectsSnapshotsIntoUiState() = runTest(dispatcher) {
        val snapshots = MutableSharedFlow<PeopleSnapshot>(extraBufferCapacity = 4)
        val viewModel = PeopleViewModel(FakePeopleRepository(snapshots), FakeEnrollmentRepository())
        runCurrent()

        assertEquals(ConnectionState.Connecting, viewModel.uiState.value.connection)

        snapshots.emit(PeopleSnapshot(online = true, names = listOf("Ana")))
        runCurrent()
        assertEquals(ConnectionState.Online, viewModel.uiState.value.connection)
        assertEquals(listOf("Ana"), viewModel.uiState.value.people)

        snapshots.emit(PeopleSnapshot(online = false, names = emptyList()))
        runCurrent()
        assertEquals(ConnectionState.Offline, viewModel.uiState.value.connection)
        // Last-known names survive a drop.
        assertEquals(listOf("Ana"), viewModel.uiState.value.people)
    }

    @Test
    fun refreshUpdatesTheListAndPostsAMessage() = runTest(dispatcher) {
        val people = FakePeopleRepository()
        people.refreshResult = ControlResult.Ok(listOf("Ana"))
        val viewModel = PeopleViewModel(people, FakeEnrollmentRepository())
        runCurrent()

        viewModel.onRefresh()
        runCurrent()

        assertEquals(listOf("Ana"), viewModel.uiState.value.people)
        assertEquals(PeopleMessage.Refreshed, viewModel.uiState.value.message?.message)
        assertFalse(viewModel.uiState.value.refreshing)
    }

    @Test
    fun cameraEnrollmentStreamsThenCompletesAndRefreshes() = runTest(dispatcher) {
        val people = FakePeopleRepository()
        val enrollment = FakeEnrollmentRepository()
        enrollment.onCamera(
            flowOf(
                EnrollEvent.Progress(EnrollPhase.StoppingRuntime, null, null, null),
                EnrollEvent.Progress(EnrollPhase.Capturing, 1, 10, null),
                EnrollEvent.Done(personCount = 2, embeddingsAdded = 10),
            ),
        )
        val viewModel = PeopleViewModel(people, enrollment)
        runCurrent()

        viewModel.onNameChange("  Ana  ")
        viewModel.onEnrollFromCamera()
        runCurrent()

        assertEquals(listOf("Ana"), enrollment.names)
        assertEquals(1, enrollment.cameraCalls)
        assertFalse(viewModel.uiState.value.enrolling)
        assertEquals(PeopleMessage.PersonEnrolled, viewModel.uiState.value.message?.message)
        // The name field is cleared so the next person can be entered.
        assertEquals("", viewModel.uiState.value.name)
        // `personCount` may lag, so the completion triggers a refresh (contract §4.6).
        assertEquals(1, people.refreshCalls)
    }

    @Test
    fun aBusyFailureIsSurfacedAndEndsEnrolling() = runTest(dispatcher) {
        val enrollment = FakeEnrollmentRepository()
        enrollment.onCamera(flowOf(EnrollEvent.Failure(EnrollmentFailure.Busy)))
        val viewModel = PeopleViewModel(FakePeopleRepository(), enrollment)
        runCurrent()

        viewModel.onNameChange("Ana")
        viewModel.onEnrollFromCamera()
        runCurrent()

        assertEquals(PeopleMessage.Busy, viewModel.uiState.value.message?.message)
        assertFalse(viewModel.uiState.value.enrolling)
    }

    @Test
    fun anEmptyNameBlocksEnrollment() = runTest(dispatcher) {
        val enrollment = FakeEnrollmentRepository()
        val viewModel = PeopleViewModel(FakePeopleRepository(), enrollment)
        runCurrent()

        viewModel.onEnrollFromCamera()
        runCurrent()

        assertTrue(viewModel.uiState.value.nameError)
        assertEquals(PeopleMessage.NeedName, viewModel.uiState.value.message?.message)
        assertEquals(0, enrollment.cameraCalls)
    }

    @Test
    fun anEmptyPickIsIgnored() = runTest(dispatcher) {
        val enrollment = FakeEnrollmentRepository()
        val viewModel = PeopleViewModel(FakePeopleRepository(), enrollment)
        runCurrent()

        viewModel.onNameChange("Ana")
        viewModel.onImagesPicked(emptyList())
        runCurrent()

        assertEquals(0, enrollment.imagesCalls)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun photosArePreparedByTheRepositoryAndSent() = runTest(dispatcher) {
        val enrollment = FakeEnrollmentRepository()
        enrollment.onImages(flowOf(EnrollEvent.Done(personCount = 1, embeddingsAdded = 3)))
        val viewModel = PeopleViewModel(FakePeopleRepository(), enrollment)
        runCurrent()

        viewModel.onNameChange("Ana")
        viewModel.onImagesPicked(listOf(byteArrayOf(1, 2), byteArrayOf(3, 4)))
        runCurrent()

        assertEquals(1, enrollment.imagesCalls)
        assertEquals(2, enrollment.imageBatches.single().size)
        assertEquals(PeopleMessage.PersonEnrolled, viewModel.uiState.value.message?.message)
    }

    @Test
    fun cancelIsSentOnASecondConnectionWhileEnrolling() = runTest(dispatcher) {
        val enrollment = FakeEnrollmentRepository()
        // A stream that never completes keeps the enrollment active.
        enrollment.onCamera(MutableSharedFlow())
        val viewModel = PeopleViewModel(FakePeopleRepository(), enrollment)
        runCurrent()

        viewModel.onNameChange("Ana")
        viewModel.onEnrollFromCamera()
        runCurrent()
        assertTrue(viewModel.uiState.value.enrolling)

        viewModel.onCancelEnrollment()
        runCurrent()

        assertEquals(1, enrollment.cancelCalls)
        assertNull(viewModel.uiState.value.pending)
        assertTrue(viewModel.uiState.value.enrolling)
    }

    @Test
    fun startRuntimeClearsTheRecoveryFlag() = runTest(dispatcher) {
        val people = FakePeopleRepository()
        val viewModel = PeopleViewModel(people, FakeEnrollmentRepository())
        runCurrent()

        viewModel.onStartRuntime()
        runCurrent()

        assertEquals(1, people.startRuntimeCalls)
        assertFalse(viewModel.uiState.value.canRestartRuntime)
        assertEquals(PeopleMessage.RuntimeStarted, viewModel.uiState.value.message?.message)
    }
}
