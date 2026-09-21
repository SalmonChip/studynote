package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.Course;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CourseMapper {

    /**
     * 插入一条课程。主键自增，插入后回填到 course.courseId。
     *
     * @param course 课程实体
     * @return 受影响行数
     */
    int insert(Course course);

    /**
     * 查询全部课程（管理端列表，当前不做分页）。
     *
     * @return 课程列表
     */
    List<Course> courseList();

    /**
     * 根据课程ID查询课程。
     *
     * @param courseId 课程ID
     * @return 课程实体，不存在返回 null
     */
    Course findById(Integer courseId);

    /**
     * 更新课程（动态更新：只更新非 null 字段）。
     *
     * @param course 待更新的课程（courseId 必填，其余字段非 null 才更新）
     * @return 受影响行数
     */
    int update(Course course);

    /**
     * 根据课程ID删除课程。
     *
     * @param courseId 课程ID
     * @return 受影响行数
     */
    int deleteById(Integer courseId);
}
