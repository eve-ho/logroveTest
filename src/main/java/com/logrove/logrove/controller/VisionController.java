package com.logrove.logrove.controller;

import com.logrove.logrove.service.VisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionService visionService;

    @PostMapping("/analyze")
    public List<String> analyzeImage(@RequestParam("file") MultipartFile file) throws IOException {
        // 이미지를 받아서 VisionService로 넘기고 분석 결과를 반환해!
        return visionService.detectLabels(file);
    }
}