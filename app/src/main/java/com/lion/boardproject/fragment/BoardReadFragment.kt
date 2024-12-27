package com.lion.boardproject.fragment

import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.lion.boardproject.BoardActivity
import com.lion.boardproject.R
import com.lion.boardproject.databinding.FragmentBoardReadBinding
import com.lion.boardproject.databinding.RowCommentListBinding
import com.lion.boardproject.model.BoardModel
import com.lion.boardproject.model.ReplyModel
import com.lion.boardproject.service.BoardService
import com.lion.boardproject.service.ReplyService
import com.lion.boardproject.viewmodel.BoardReadViewModel
import com.lion.boardproject.viewmodel.RowCommentListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.format.DateFormat
import android.view.ViewTreeObserver
import kotlinx.coroutines.async

class BoardReadFragment(val boardMainFragment: BoardMainFragment) : Fragment() {

    lateinit var fragmentBoardReadBinding: FragmentBoardReadBinding
    lateinit var boardActivity: BoardActivity

    // 현재 글의 문서 id를 담을 변수
    lateinit var boardDocumentId: String

    // 글 데이터를 담을 변수
    lateinit var boardModel: BoardModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentBoardReadBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_board_read, container, false)
        fragmentBoardReadBinding.boardReadViewModel = BoardReadViewModel(this@BoardReadFragment)
        fragmentBoardReadBinding.lifecycleOwner = this@BoardReadFragment


        boardActivity = activity as BoardActivity

        // 이미지 뷰를 안보이는 상태로 설정한다.
        fragmentBoardReadBinding.imageViewBoardRead.isVisible = false

        // arguments의 값을 변수에 담아주는 메서드를 호출한다.
        gettingArguments()
        // 툴바를 구성하는 메서드를 호출한다.
        settingToolbar()
        // 글 데이터를 가져와 보여주는 메서드를 호출한다.
        settingBoardData()
        // 댓글 데이터 RecyclerView 구성 메서드를 호출한다.
        settingCommentRecyclerView()

        fragmentBoardReadBinding.buttonSubmitComment.setOnClickListener {
            // 댓글 작성 메서드를 호춯한다.
            submitReply()
        }

        // 키보드 상태에 따라 레이아웃을 조정하는 메서드를 호출한다.
        adjustLayoutForKeyboard()

        return fragmentBoardReadBinding.root
    }
    // 이전 화면으로 돌아가는 메서드
    fun movePrevFragment(){
        boardMainFragment.removeFragment(BoardSubFragmentName.BOARD_WRITE_FRAGMENT)
        boardMainFragment.removeFragment(BoardSubFragmentName.BOARD_READ_FRAGMENT)
    }

    // 툴바를 구성하는 메서드
    fun settingToolbar(){
        fragmentBoardReadBinding.apply {
            // 메뉴를 보이지 않게 설정한다.
            toolbarBoardRead.menu.children.forEach {
                it.isVisible = false
            }

            toolbarBoardRead.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuItemBoardReadModify -> {
                        // 글의 문서 번호를 전달한다.
                        val dataBundle = Bundle()
                        dataBundle.putString("boardDocumentId", boardDocumentId)
                        boardMainFragment.replaceFragment(BoardSubFragmentName.BOARD_MODIFY_FRAGMENT, true, true, dataBundle)
                    }
                    R.id.menuItemBoardReadDelete -> {
                        val builder = MaterialAlertDialogBuilder(boardActivity)
                        builder.setTitle("글 삭제")
                        builder.setMessage("삭제시 복구할 수 없습니다")
                        builder.setNegativeButton("취소", null)
                        builder.setPositiveButton("삭제"){ dialogInterface: DialogInterface, i: Int ->
                            proBoardDelete()
                        }
                        builder.show()
                    }
                }
                true
            }
        }
    }

    // arguments의 값을 변수에 담아준다.
    fun gettingArguments(){
        boardDocumentId = arguments?.getString("boardDocumentId")!!
    }

    // 글 데이터를 가져와 보여주는 메서드
    fun settingBoardData(){
        // 서버에서 데이터를 가져온다.
        CoroutineScope(Dispatchers.Main).launch {
            val work1 = async(Dispatchers.IO){
                BoardService.selectBoardDataOneById(boardDocumentId)
            }
            boardModel = work1.await()

            fragmentBoardReadBinding.apply {
                boardReadViewModel?.textFieldBoardReadTitleText?.value = boardModel.boardTitle
                boardReadViewModel?.textFieldBoardReadTextText?.value = boardModel.boardText
                boardReadViewModel?.textFieldBoardReadTypeText?.value = boardModel.boardTypeValue.str
                boardReadViewModel?.textFieldBoardReadNickName?.value = boardModel.boardWriterNickName

                // 작성자와 로그인한 사람이 같으면 메뉴를 보기에 한다.
                if(boardModel.boardWriteId == boardActivity.loginUserDocumentId){
                    toolbarBoardRead.menu.children.forEach {
                        it.isVisible = true
                    }
                }
            }

            // 첨부 이미지가 있다면
            if(boardModel.boardFileName != "none"){
                val work1 = async(Dispatchers.IO) {
                    // 이미지에 접근할 수 있는 uri를 가져온다.
                    BoardService.gettingImage(boardModel.boardFileName)
                }

                val imageUri = work1.await()
                boardActivity.showServiceImage(imageUri, fragmentBoardReadBinding.imageViewBoardRead)
                fragmentBoardReadBinding.imageViewBoardRead.isVisible = true
            }
        }
    }

    // 글 삭제 처리 메서드
    fun proBoardDelete(){
        CoroutineScope(Dispatchers.Main).launch {
            // 만약 첨부 이미지가 있다면 삭제한다.
            if(boardModel.boardFileName != "none"){
                val work1 = async(Dispatchers.IO){
                    BoardService.removeImageFile(boardModel.boardFileName)
                }
                work1.join()
            }
            // 글 정보를 삭제한다.
            val work2 = async(Dispatchers.IO){
                BoardService.deleteBoardData(boardDocumentId)
            }
            work2.join()
            // 글 목록 화면으로 이동한다.
            boardMainFragment.removeFragment(BoardSubFragmentName.BOARD_READ_FRAGMENT)
        }
    }

    // 댓글 RecyclerView를 설정하는 메서드
    fun settingCommentRecyclerView(){
        fragmentBoardReadBinding.apply {
            recyclerViewComments.adapter = CommentRecyclerViewAdapter()
            recyclerViewComments.layoutManager = LinearLayoutManager(boardActivity)
            val deco = MaterialDividerItemDecoration(boardActivity, MaterialDividerItemDecoration.VERTICAL)
            recyclerViewComments.addItemDecoration(deco)
        }
        loadReplies()
    }

    // 댓글 작성 메서드
    fun submitReply(){
        // 입력된 댓글 내용을 가져옴
        val inputText = fragmentBoardReadBinding.editTextCommentInput.text.toString().trim()
        if (inputText.isNotEmpty()) {
            // 댓글 데이터 모델 생성
            val replyModel = ReplyModel().apply {
                replyBoardId = boardDocumentId // 현재 게시글 ID
                replyNickName = boardActivity.loginUserNickName // 로그인된 사용자 닉네임
                replyText = inputText // 입력된 댓글 내용
                replyTimeStamp = System.currentTimeMillis() // 현재 시간
            }

            // 서버에 댓글 추가
            CoroutineScope(Dispatchers.IO).launch {
                ReplyService.addReply(replyModel) // 댓글 저장
                withContext(Dispatchers.Main) {
                    // 입력란 초기화
                    fragmentBoardReadBinding.editTextCommentInput.text?.clear()
                    // 댓글 목록을 서버에서 가져오는 메서드를 호출한다.
                    loadReplies()
                }
            }
        }
    }

    // 댓글 목록을 서버에서 가져오는 메서드
    fun loadReplies(){
        CoroutineScope(Dispatchers.IO).launch {
            val replies = ReplyService.getRepliesForBoard(boardDocumentId)
            val replyList = replies.toMutableList()

            withContext(Dispatchers.Main) {
                // 댓글이 없는 경우 "댓글이 없습니다" 메시지를 표시
                if (replyList.isEmpty()) {
                    fragmentBoardReadBinding.textViewNoComments.visibility = View.VISIBLE
                    fragmentBoardReadBinding.recyclerViewComments.visibility = View.GONE
                } else {
                    // 댓글이 있는 경우 RecyclerView를 표시
                    fragmentBoardReadBinding.textViewNoComments.visibility = View.GONE
                    fragmentBoardReadBinding.recyclerViewComments.visibility = View.VISIBLE
                    (fragmentBoardReadBinding.recyclerViewComments.adapter as CommentRecyclerViewAdapter).submitList(replyList)
                }
            }
        }
    }

    // 날짜 형식 변환 메서드
    fun formatTimestamp(timestamp: Long): String{
        return DateFormat.format("yyyy-MM-dd HH:mm", timestamp).toString()
    }

    // 댓글 RecyclerView의 어댑터 클래스
    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<CommentRecyclerViewAdapter.CommentViewHolder>(){
        private val replyList = mutableListOf<ReplyModel>()

        inner class CommentViewHolder(val binding: RowCommentListBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val binding = DataBindingUtil.inflate<RowCommentListBinding>(layoutInflater, R.layout.row_comment_list, parent, false)
            binding.lifecycleOwner = this@BoardReadFragment
            return CommentViewHolder(binding)
        }

        override fun getItemCount(): Int = replyList.size

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val reply = replyList[position]
            holder.binding.rowCommentListViewModel = RowCommentListViewModel(this@BoardReadFragment).apply {
                textViewRowCommentListNameText.value = reply.replyNickName
                textViewRowCommentListCommentText.value = reply.replyText
                // 날짜 형식 변환 메서드를 호출하여 저장한다.
                textViewRowCommentListTimeText.value = formatTimestamp(reply.replyTimeStamp)
            }

            // 로그인된 사용자와 댓글 작성자가 같은 경우만 메뉴 아이콘 표시
            val isAuthor = reply.replyNickName == boardActivity.loginUserNickName
            holder.binding.menuIcon.isVisible = isAuthor

            holder.binding.menuIcon.setOnClickListener {
                showReplyMenu(holder.binding.menuIcon, reply)
            }
        }

        fun submitList(newReplies: List<ReplyModel>) {
            replyList.clear()
            replyList.addAll(newReplies)
            notifyDataSetChanged()
        }
    }

    // 댓글 수정, 삭제 메뉴를 표시하는 메서드
    fun showReplyMenu(view: View, reply: ReplyModel){
        val popupMenu = PopupMenu(boardActivity, view)
        popupMenu.menuInflater.inflate(R.menu.menu_comment, popupMenu.menu)

        // 메뉴 항목 클릭 이벤트 처리
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuCommentEdit -> {
                    // 댓글 수정 다이얼로그 메서드를 호춯한다.
                    modifyReply(reply)
                    true
                }
                R.id.menuCommentDelete -> {
                    // 댓글 삭제 메서드를 호출한다.
                    deleteReply(reply.replyDocumentId)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // 댓글 수정 다이얼로그 메서드
    fun modifyReply(reply: ReplyModel){
        val editText = EditText(boardActivity).apply {
            setText(reply.replyText)
        }

        MaterialAlertDialogBuilder(boardActivity)
            .setTitle("댓글 수정")
            .setView(editText)
            .setNegativeButton("취소", null)
            .setPositiveButton("수정") { _, _ ->
                val updatedText = editText.text.toString()
                if (updatedText.isNotBlank()) {
                    reply.replyText = updatedText
                    // 댓글을 업데이트 메서드를 호출한다.
                    updateReply(reply)
                }
            }
            .show()
    }

    // 댓글 삭제 메서드
    fun deleteReply(replyDocumentId: String){
        MaterialAlertDialogBuilder(boardActivity)
            .setTitle("댓글 삭제")
            .setMessage("이 댓글을 삭제하시겠습니까?")
            .setNegativeButton("취소", null) // 취소 버튼
            .setPositiveButton("삭제") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    ReplyService.deleteReply(replyDocumentId) // 댓글 삭제
                    withContext(Dispatchers.Main) {
                        loadReplies() // 댓글 목록 새로고침
                    }
                }
            }
            .show()
    }

    // 댓글을 업데이트 메서드
    fun updateReply(reply: ReplyModel){
        CoroutineScope(Dispatchers.IO).launch {
            ReplyService.updateReply(reply) // 댓글 업데이트 요청
            withContext(Dispatchers.Main) {
                loadReplies() // 댓글 목록 새로고침
            }
        }
    }

    // 키보드 상태에 따라 레이아웃을 조정하는 메서드
    fun adjustLayoutForKeyboard(){
        val rootView = fragmentBoardReadBinding.root
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 프래그먼트가 Context에 연결되었는지 확인
                if (!isAdded) return

                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)

                val displayMetrics = requireContext().resources.displayMetrics
                val totalHeight = displayMetrics.heightPixels
                val visibleHeight = rect.bottom - rect.top
                val keyboardHeight = totalHeight - visibleHeight

                if (keyboardHeight > totalHeight * 0.15) {
                    fragmentBoardReadBinding.linearLayoutCommentInput.translationY = -keyboardHeight.toFloat()
                } else {
                    fragmentBoardReadBinding.linearLayoutCommentInput.translationY = 0f
                }
            }
        })
    }
}