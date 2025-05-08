/*
package com.example.notaflow.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.notaflow.MainActivity
import com.example.notaflow.data.local.entity.Note
import com.example.notaflow.domain.usecase.GetNotesUseCase
import com.example.notaflow.ui.navigation.Screen
import com.example.notaflow.ui.screens.notelist.NoteListScreen
import com.example.notaflow.ui.screens.notelist.NoteListViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Date
import javax.inject.Inject

@HiltAndroidTest
class NoteListScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var getNotesUseCase: GetNotesUseCase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun emptyState_isDisplayed_whenNoNotes() {
        // Set up the empty state
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "noteList"
            ) {
                composable("noteList") {
                    NoteListScreen(
                        onNoteClick = { },
                        onNewNoteClick = { },
                        onSettingsClick = { }
                    )
                }
            }
        }

        // Verify empty state is displayed
        composeRule.onNodeWithText("No notes yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Tap + to create a new note.").assertIsDisplayed()
    }

    @Test
    fun floatingActionButton_navigatesToEditScreen() {
        // Set up UI with mocked navigation
        var navigatedToEditScreen = false

        composeRule.setContent {
            NoteListScreen(
                onNoteClick = { },
                onNewNoteClick = { navigatedToEditScreen = true },
                onSettingsClick = { }
            )
        }

        // Click the FAB
        composeRule.onNodeWithContentDescription("Add Note").performClick()

        // Verify navigation occurred
        assert(navigatedToEditScreen)
    }
}

 */