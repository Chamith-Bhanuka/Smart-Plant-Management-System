package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.entity.Plant;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.repository.PlantRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//@RestController
//@RequestMapping("/uploads")
//public class FileController {
//    @GetMapping("/{filename:.+}")
//    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
//        Path filePath = Paths.get("uploads").resolve(filename).normalize();
//        Resource resource = new FileSystemResource(filePath.toFile());
//
//        if (!resource.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.IMAGE_JPEG) // or detect dynamically
//                .body(resource);
//    }
//}


@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class FileController {

    private final PlantRepository plantRepository;
    private final UserRepository userRepository;

    @GetMapping("/last")
    public ResponseEntity<Resource> getLastUploadedImage(@AuthenticationPrincipal UserDetails userDetails) {
        // find current logged in user
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // get the last uploaded plant for this user
        Plant lastPlant = plantRepository.findTopByUserOrderByIdDesc(user)
                .orElseThrow(() -> new RuntimeException("No plant found for this user"));

        Path filePath = Paths.get(lastPlant.getImagePath()).normalize();
        Resource resource = new FileSystemResource(filePath.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Detect MIME type dynamically (so PNG/JPG both work)
        MediaType mediaType = MediaType.IMAGE_JPEG;
        try {
            String mimeType = Files.probeContentType(filePath);
            if (mimeType != null) {
                mediaType = MediaType.parseMediaType(mimeType);
            }
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) throws IOException {
        Path path = Paths.get("uploads").resolve(filename).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String mimeType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/uploads/diagnoses/{filename:.+}")
    public ResponseEntity<Resource> getDFile(@PathVariable String filename) throws IOException {
        Path path = Paths.get("uploads/diagnoses").resolve(filename).normalize();
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String mimeType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
