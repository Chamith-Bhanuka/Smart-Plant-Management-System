package lk.ijse.spring.smartplantmanagementsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + Objects.requireNonNull(file.getOriginalFilename().replaceAll("\\s+", "_"));
        Path base = Paths.get(uploadDir);
        Files.createDirectories(base);
        Path target = base.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "uploads/" + fileName;
    }
}
