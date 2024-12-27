package com.lion.boardproject.service

import com.lion.boardproject.model.ReplyModel
import com.lion.boardproject.repository.ReplyRepository
import com.lion.boardproject.vo.ReplyVO

class ReplyService {

    companion object{

        // 댓글 데이터를 저장하는 메서드
        suspend fun addReply(replyModel: ReplyModel) {
            // VO 객체를 생성한다.
            val replyVO = replyModel.toReplyVO()
            // 저장한다.
            ReplyRepository.addReply(replyVO)
        }

        // 특정 글의 댓글 목록을 가져오는 메서드
        suspend fun getRepliesForBoard(boardId: String): MutableList<ReplyModel> {
            // 댓글 정보를 가져온다.
            val replyList = mutableListOf<ReplyModel>()
            val resultList = ReplyRepository.getRepliesForBoard(boardId)

            resultList.forEach {
                val replyVO = it["replyVO"] as ReplyVO
                val documentId = it["documentId"] as String
                val replyModel = replyVO.toReplyModel(documentId)
                replyList.add(replyModel)
            }

            return replyList
        }

        // 댓글 정보를 수정하는 메서드
        suspend fun updateReply(replyModel: ReplyModel) {
            // VO 객체를 생성한다.
            val replyVO = replyModel.toReplyVO()
            // 수정 메서드를 호출한다.
            ReplyRepository.updateReply(replyModel.replyDocumentId, replyVO.replyText)
        }

        // 댓글 데이터를 삭제하는 메서드
        suspend fun deleteReply(replyDocumentId: String) {
            ReplyRepository.deleteReply(replyDocumentId)
        }
    }
}