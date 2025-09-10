package lk.ijse.spring.smartplantmanagementsystem.controller;

import lk.ijse.spring.smartplantmanagementsystem.dto.CommentDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.CreatePostRequest;
import lk.ijse.spring.smartplantmanagementsystem.dto.PostDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.VoteRequest;
import lk.ijse.spring.smartplantmanagementsystem.service.LearnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/learn")
@RequiredArgsConstructor
public class LearnController {
    private final LearnService learnService;

    @GetMapping("/posts")
    public Page<PostDTO> list(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return learnService.list(filter, search, pageable, user.getUsername());
    }

    @GetMapping("/posts/{id}")
    public PostDTO get(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return learnService.get(id, user.getUsername());
    }

    // Create with multipart cover image
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostDTO create(
            @RequestPart("meta") CreatePostRequest meta,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        return learnService.create(meta, cover, user.getUsername());
    }

    // Update (supports partial update + optional cover)
    @PutMapping(value = "/posts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostDTO update(
            @PathVariable Long id,
            @RequestPart("meta") CreatePostRequest meta,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        return learnService.update(id, meta, cover, user.getUsername());
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        learnService.delete(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Comments
    @GetMapping("/posts/{id}/comments")
    public List<CommentDTO> listComments(@PathVariable Long id) {
        return learnService.listComments(id);
    }

    @PostMapping("/posts/{id}/comments")
    public CommentDTO addComment(@PathVariable Long id, @RequestBody Map<String, String> body,
                                 @AuthenticationPrincipal UserDetails user) {
        String text = body.get("text");
        return learnService.addComment(id, text, user.getUsername());
    }

    // Votes
    @PostMapping("/posts/{id}/vote")
    public PostDTO vote(@PathVariable Long id, @RequestBody VoteRequest vote,
                        @AuthenticationPrincipal UserDetails user) {
        return learnService.vote(id, vote.getVote(), user.getUsername());
    }

    // Trending topics
    @GetMapping("/trending")
    public Map<String, Integer> trending() {
        return learnService.trending();
    }
}
