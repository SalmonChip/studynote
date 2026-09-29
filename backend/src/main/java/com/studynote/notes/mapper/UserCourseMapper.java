package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.UserCourse;
import com.studynote.notes.model.vo.course.UserCourseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCourseMapper {

    /**
     * 插入一条已购课程记录，唯一索引 uk_user_course 防重复。
     */
    int insert(UserCourse userCourse);

    /**
     * 统计用户是否已拥有某课程。
     */
    int countByUserIdAndCourseId(@Param("userId") Long userId,
                                 @Param("courseId") Integer courseId);

    /**
     * 查询用户的全部已购课程（JOIN 课程表）。
     */
    List<UserCourseVO> findByUserId(@Param("userId") Long userId);
}
