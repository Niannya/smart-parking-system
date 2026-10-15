package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.backend.entity.Instructor;

import java.util.List;

@Mapper
public interface InstructorMapper {

    List<Instructor> findAll();
}