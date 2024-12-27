package com.lion.boardproject.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lion.boardproject.fragment.BoardReadFragment

class RowCommentListViewModel(val boardReadFragment: BoardReadFragment) : ViewModel() {
    // textViewRowCommentListName - text
    val textViewRowCommentListNameText = MutableLiveData("")
    // textViewRowCommentListComment - text
    val textViewRowCommentListCommentText = MutableLiveData("")
    // textViewRowCommentListTime - text
    val textViewRowCommentListTimeText = MutableLiveData("")
}