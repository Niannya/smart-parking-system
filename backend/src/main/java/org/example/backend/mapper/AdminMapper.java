package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.backend.entity.Admin;

import java.util.List;

@Mapper
public interface AdminMapper {

    List<Admin> findAll();
}