package com.example.placy

/** State of the publication overlay. Dismissing either view returns to the map. */
internal sealed interface PostViewerState {
    data object Hidden : PostViewerState
    data class Card(val mark: GeoMark) : PostViewerState
    data class Photo(val mark: GeoMark) : PostViewerState
}

internal sealed interface PostViewerAction {
    data class Select(val mark: GeoMark) : PostViewerAction
    data object OpenPhoto : PostViewerAction
    data object Dismiss : PostViewerAction
}

internal fun reducePostViewer(state: PostViewerState, action: PostViewerAction): PostViewerState =
    when (action) {
        is PostViewerAction.Select -> PostViewerState.Card(action.mark)
        PostViewerAction.OpenPhoto -> if (state is PostViewerState.Card) PostViewerState.Photo(state.mark) else state
        PostViewerAction.Dismiss -> PostViewerState.Hidden
    }
