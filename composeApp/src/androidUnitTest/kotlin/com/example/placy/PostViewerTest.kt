package com.example.placy

import org.junit.Test
import kotlin.test.assertEquals

class PostViewerTest {
    private val a = GeoMark("a", 59.9, 30.3)
    private val b = GeoMark("b", -1.0, 2.0)
    @Test fun selectingFromMapOpensCard() {
        assertEquals(PostViewerState.Card(a), reducePostViewer(PostViewerState.Hidden, PostViewerAction.Select(a)))
    }
    @Test fun cardOpensItsOwnPhoto() {
        assertEquals(PostViewerState.Photo(a), reducePostViewer(PostViewerState.Card(a), PostViewerAction.OpenPhoto))
    }
    @Test fun dismissingCardReturnsToMap() {
        assertEquals(PostViewerState.Hidden, reducePostViewer(PostViewerState.Card(a), PostViewerAction.Dismiss))
    }
    @Test fun dismissingPhotoReturnsToMap() {
        assertEquals(PostViewerState.Hidden, reducePostViewer(PostViewerState.Photo(a), PostViewerAction.Dismiss))
    }
    @Test fun openingPhotoWithoutSelectionDoesNothing() {
        assertEquals(PostViewerState.Hidden, reducePostViewer(PostViewerState.Hidden, PostViewerAction.OpenPhoto))
    }
    @Test fun dismissingMapDoesNothing() {
        assertEquals(PostViewerState.Hidden, reducePostViewer(PostViewerState.Hidden, PostViewerAction.Dismiss))
    }
    @Test fun openingAlreadyOpenPhotoKeepsSelection() {
        assertEquals(PostViewerState.Photo(a), reducePostViewer(PostViewerState.Photo(a), PostViewerAction.OpenPhoto))
    }
    @Test fun selectingDifferentMarkReplacesCard() {
        assertEquals(PostViewerState.Card(b), reducePostViewer(PostViewerState.Card(a), PostViewerAction.Select(b)))
    }
    @Test fun selectingMarkAfterPhotoShowsNewCard() {
        assertEquals(PostViewerState.Card(b), reducePostViewer(PostViewerState.Photo(a), PostViewerAction.Select(b)))
    }
    @Test fun photoAfterChangingSelectionUsesNewMark() {
        val card = reducePostViewer(PostViewerState.Card(a), PostViewerAction.Select(b))
        assertEquals(PostViewerState.Photo(b), reducePostViewer(card, PostViewerAction.OpenPhoto))
    }
    @Test fun selectionCanBeReopenedAfterDismiss() {
        val hidden = reducePostViewer(PostViewerState.Card(a), PostViewerAction.Dismiss)
        assertEquals(PostViewerState.Card(a), reducePostViewer(hidden, PostViewerAction.Select(a)))
    }
}
