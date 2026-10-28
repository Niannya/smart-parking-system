package org.example.backend.controller;

import org.example.backend.entity.Admin;
import org.example.backend.mapper.AdminMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/instructors")
public class AdminController {

    private final AdminMapper adminMapper;

    public AdminController(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @GetMapping
    public List<Admin> findAll() {
        return adminMapper.findAll();
    }
}