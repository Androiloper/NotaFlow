package com.example.notaflow.ui.navigation

sealed class Screen(val route: String) {
    object NoteList : Screen("note_list")
    object NoteEdit : Screen("note_edit?noteId={noteId}") {
        fun createRoute(noteId: Long? = null): String {
            return if (noteId != null) {
                "note_edit?noteId=$noteId"
            } else {
                "note_edit?noteId="
            }
        }
    }
    object Settings : Screen("settings")
}