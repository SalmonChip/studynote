package com.studynote.notes.service.impl;

import com.studynote.notes.annotation.NeedLogin;
import com.studynote.notes.mapper.NoteLikeMapper;
import com.studynote.notes.mapper.NoteMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.message.MessageDTO;
import com.studynote.notes.model.entity.Note;
import com.studynote.notes.model.entity.NoteLike;
import com.studynote.notes.model.enums.message.MessageTargetType;
import com.studynote.notes.model.enums.message.MessageType;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.MessageService;
import com.studynote.notes.service.NoteLikeService;
import com.studynote.notes.utils.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NoteLikeServiceImpl implements NoteLikeService {

    private final NoteLikeMapper noteLikeMapper;
    private final NoteMapper noteMapper;
    private final RequestScopeData requestScopeData;
    private final MessageService messageService;

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> likeNote(Integer noteId) {
        Long userId = requestScopeData.getUserId();

        // 查询笔记
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            return ApiResponseUtil.error("笔记不存在");
        }

        // 幂等：已点赞则直接返回成功，避免重复插入和重复计数
        NoteLike existing = noteLikeMapper.findByUserIdAndNoteId(userId, noteId);
        if (existing != null) {
            return ApiResponseUtil.success("点赞成功");
        }

        try {
            // 创建点赞记录
            NoteLike noteLike = new NoteLike();
            noteLike.setNoteId(noteId);
            noteLike.setUserId(userId);
            noteLike.setCreatedAt(new Date());
            noteLikeMapper.insert(noteLike);

            // 增加笔记点赞数
            noteMapper.likeNote(noteId);

            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setType(MessageType.LIKE);
            messageDTO.setReceiverId(note.getAuthorId());
            messageDTO.setSenderId(userId);

            messageDTO.setTargetType(MessageTargetType.NOTE);
            messageDTO.setTargetId(noteId);
            messageDTO.setIsRead(false);

            System.out.println(messageDTO);

            messageService.asyncCreateMessage(messageDTO);

            return ApiResponseUtil.success("点赞成功");
        } catch (Exception e) {
            return ApiResponseUtil.error("点赞失败");
        }
    }

    @Override
    @NeedLogin
    @Transactional
    public ApiResponse<EmptyVO> unlikeNote(Integer noteId) {
        Long userId = requestScopeData.getUserId();

        // 查询笔记
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            return ApiResponseUtil.error("笔记不存在");
        }

        try {
            // 删除点赞记录
            NoteLike noteLike = noteLikeMapper.findByUserIdAndNoteId(userId, noteId);
            if (noteLike != null) {
                noteLikeMapper.delete(noteLike);
                // 减少笔记点赞数
                noteMapper.unlikeNote(noteId);
            }
            return ApiResponseUtil.success("取消点赞成功");
        } catch (Exception e) {
            return ApiResponseUtil.error("取消点赞失败");
        }
    }

    @Override
    public Set<Integer> findUserLikedNoteIds(Long userId, List<Integer> noteIds) {
        List<Integer> likedIds = noteLikeMapper.findUserLikedNoteIds(userId, noteIds);
        return new HashSet<>(likedIds);
    }
}
