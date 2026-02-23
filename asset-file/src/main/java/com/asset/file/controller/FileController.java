package com.asset.file.controller;

import com.asset.file.service.LocalFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传 Controller
 *
 * <pre>
 * POST /file/upload  上传单个文件，返回 { url: "/file/2026/02/xxx.jpg" }
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final LocalFileService fileService;

    /**
     * 上传单个文件
     *
     * @param file 待上传的文件（表单字段名为 file）
     * @return 包含访问URL的Map：{ "url": "/file/yyyy/MM/uuid.ext" }
     * @throws IOException 文件存储失败时抛出
     */
    @PostMapping("/file/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String url = fileService.save(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return result;
    }
}
