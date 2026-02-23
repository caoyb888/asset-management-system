package com.asset.file.service;

import com.asset.file.config.FileProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储 Service
 * 文件按日期分目录存放，文件名使用 UUID 避免冲突
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileService {

    private final FileProperties props;

    /**
     * 保存文件到本地磁盘，返回可访问的相对URL
     *
     * @param file 上传的文件
     * @return 访问URL，格式如 /file/2026/02/uuid.jpg
     * @throws IOException 文件写入失败时抛出
     */
    public String save(MultipartFile file) throws IOException {
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "file";
        // 保留原始扩展名
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : "";

        // 按年月分目录存放
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(props.getUploadDir(), datePath);
        Files.createDirectories(dir);

        Path target = dir.resolve(fileName);
        file.transferTo(target.toFile());

        log.info("文件上传成功: {}", target.toAbsolutePath());

        return props.getAccessUrlPrefix() + "/" + datePath + "/" + fileName;
    }
}
