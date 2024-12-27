package com.lion.boardproject.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lion.boardproject.vo.ReplyVO
import kotlinx.coroutines.tasks.await

class ReplyRepository {

    companion object{
        // 댓글 데이터를 저장하는 메서드
        suspend fun addReply(replyVO: ReplyVO) {
            val fireStore = FirebaseFirestore.getInstance()
            val collectionReference = fireStore.collection("Replies")
            collectionReference.add(replyVO).await()
        }

        // 특정 글의 댓글 목록을 가져오는 메서드
        suspend fun getRepliesForBoard(boardId: String): MutableList<Map<String, *>> {
            val fireStore = FirebaseFirestore.getInstance()
            val collectionReference = fireStore.collection("Replies")

            // 댓글 데이터를 시간순으로 가져오기
            val querySnapshot = collectionReference.whereEqualTo("replyBoardId", boardId).orderBy("replyTimeStamp", Query.Direction.ASCENDING).get().await()

            // 반환할 리스트 생성
            val resultList = mutableListOf<Map<String, *>>()
            querySnapshot.forEach {
                val map = mapOf(
                    // 댓글 문서 ID
                    "documentId" to it.id,
                    // 댓글 데이터를 VO 객체로 변환
                    "replyVO" to it.toObject(ReplyVO::class.java)
                )
                resultList.add(map)
            }
            return resultList
        }

        // 댓글 데이터를 삭제하는 메서드
        suspend fun deleteReply(replyDocumentId: String) {
            val fireStore = FirebaseFirestore.getInstance()
            val collectionReference = fireStore.collection("Replies")
            val documentReference = collectionReference.document(replyDocumentId)
            documentReference.delete().await()
        }

        // 댓글 데이터를 수정하는 메서드
        suspend fun updateReply(replyDocumentId: String, updatedText: String) {
            val fireStore = FirebaseFirestore.getInstance()
            val collectionReference = fireStore.collection("Replies")
            val documentReference = collectionReference.document(replyDocumentId)

            // 수정할 데이터를 맵으로 생성
            val updateMap = mapOf(
                "replyText" to updatedText
            )
            documentReference.update(updateMap).await()
        }
    }
}