package org.example.backend.controller;

import org.example.backend.entity.Instructor;
import org.example.backend.mapper.InstructorMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/instructors")
public class InstructorController {

    private final InstructorMapper instructorMapper;

    public InstructorController(InstructorMapper instructorMapper) {
        this.instructorMapper = instructorMapper;
    }

    @GetMapping
    public List<Instructor> findAll() {
        return instructorMapper.findAll();
    }
}