package lk.ijse.spring.smartplantmanagementsystem.service;

import lk.ijse.spring.smartplantmanagementsystem.dto.CommentDTO;
import lk.ijse.spring.smartplantmanagementsystem.dto.CreatePostRequest;
import lk.ijse.spring.smartplantmanagementsystem.dto.PostDTO;
import lk.ijse.spring.smartplantmanagementsystem.entity.Comment;
import lk.ijse.spring.smartplantmanagementsystem.entity.Post;
import lk.ijse.spring.smartplantmanagementsystem.entity.User;
import lk.ijse.spring.smartplantmanagementsystem.entity.Vote;
import lk.ijse.spring.smartplantmanagementsystem.repository.CommentRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.PostRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.UserRepository;
import lk.ijse.spring.smartplantmanagementsystem.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnService {
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final VoteRepository voteRepo;
    private final UserRepository userRepo;
    private final FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private Page<PostDTO> list(String filter, String search, Pageable pageable, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Page<Post> page;

        if (search != null && !search.isBlank()) {
            page = switch (filter == null ? "all" : filter) {
                case "mine" -> postRepo.searchMine(user, search, pageable);
                default -> postRepo.searchAll(search, pageable);
            };
        } else {
            page = switch (filter == null ? "all" : filter) {
                case "mine" -> postRepo.findByUser(user, pageable);
                default -> postRepo.findAll(pageable);
            };
        }
        return page.map(p -> toDTO(p, user));
    }

    public PostDTO get(Long id, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post post = postRepo.findById(id).orElseThrow();
        return toDTO(post, user);
    }

    @Transactional
    public PostDTO create(CreatePostRequest req, MultipartFile cover, String userEmail) throws IOException {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post p = new Post();
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setTags(normalizeTags(req.getTags()));
        p.setUser(user);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        if (cover != null && !cover.isEmpty()) {
            String stored = fileStorageService.store(cover);
            p.setCoverImagePath(stored);
        }
        p = postRepo.save(p);
        return toDTO(p, user);
    }

    @Transactional
    public PostDTO update(Long id, CreatePostRequest req, MultipartFile cover, String userEmail) throws IOException {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post p = postRepo.findById(id).orElseThrow();
        if (!p.getUser().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        if (req.getTitle() != null) p.setTitle(req.getTitle());
        if (req.getContent() != null) p.setContent(req.getContent());
        if (req.getTags() != null) p.setTags(normalizeTags(req.getTags()));
        if (cover != null && !cover.isEmpty()) {
            String stored = fileStorageService.store(cover);
            p.setCoverImagePath(stored);
        }
        p.setUpdatedAt(LocalDateTime.now());
        return toDTO(p, user);
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post p = postRepo.findById(id).orElseThrow();
        if (!p.getUser().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        // cascade delete comments and votes or delete them explicitly
        commentRepo.deleteAll(commentRepo.findByPostIdOrderByCreatedAtAsc(id));
        voteRepo.findByPostIdAndUserId(id, user.getId()).ifPresent(voteRepo::delete); // optional
        postRepo.delete(p);
    }

    @Transactional
    public CommentDTO addComment(Long postId, String text, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post post = postRepo.findById(postId).orElseThrow();
        Comment c = new Comment();
        c.setText(text);
        c.setPost(post);
        c.setUser(user);
        c.setCreatedAt(LocalDateTime.now());
        c = commentRepo.save(c);
        return toDTO(c);
    }

    public List<CommentDTO> listComments(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId)
                .stream().map(this::toDTO).toList();
    }

    private PostDTO toDTO(Post p, User currentUser) {
        PostDTO dto = new PostDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setContent(p.getContent());
        dto.setCoverImageUrl(p.getCoverImagePath() != null ? absolute(p.getCoverImagePath()) : null);
        dto.setTags(splitTags(p.getTags()));
        dto.setUpVotes(p.getUpVotes());
        dto.setDownVotes(p.getDownVotes());
        dto.setAuthorName(p.getUser().getEmail());
        dto.setAuthorEmail(p.getUser().getEmail());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());

        // current user vote state
        voteRepo.findByPostIdAndUserId(p.getId(), currentUser.getId())
                .ifPresent(v -> dto.setUserVote(v.getType() == Vote.Type.UP ? "up" : "down"));
        return dto;
    }

    @Transactional
    public PostDTO vote(Long postId, String vote, String userEmail) {
        User user = userRepo.findByEmail(userEmail).orElseThrow();
        Post post = postRepo.findById(postId).orElseThrow();
        Optional<Vote> existing = voteRepo.findByPostIdAndUserId(postId, user.getId());

        // Remove existing adjustments from counts
        if (existing.isPresent()) {
            if (existing.get().getType() == Vote.Type.UP) post.setUpVotes(Math.max(0, post.getUpVotes() - 1));
            if (existing.get().getType() == Vote.Type.DOWN) post.setDownVotes(Math.max(0, post.getDownVotes() - 1));
        }

        if ("none".equalsIgnoreCase(vote)) {
            existing.ifPresent(voteRepo::delete);
        } else {
            Vote.Type newType = "up".equalsIgnoreCase(vote) ? Vote.Type.UP : Vote.Type.DOWN;
            Vote v = existing.orElseGet(Vote::new);
            v.setPost(post); v.setUser(user);
            v.setType(newType);
            v.setUpdatedAt(LocalDateTime.now());
            if (v.getId() == null) v.setCreatedAt(LocalDateTime.now());
            voteRepo.save(v);
            if (newType == Vote.Type.UP) post.setUpVotes(post.getUpVotes() + 1);
            else post.setDownVotes(post.getDownVotes() + 1);
        }

        post.setUpdatedAt(LocalDateTime.now());
        postRepo.save(post);
        return toDTO(post, user);
    }

    public Map<String, Integer> trending() {
        Map<String, Integer> map = new HashMap<>();
        postRepo.findAll().forEach(p -> {
            for (String t : splitTags(p.getTags())) {
                if (t.isBlank()) continue;
                map.merge(t, 1, Integer::sum);
            }
        });
        return map.entrySet().stream()
                .sorted((a,b)->Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .collect(LinkedHashMap::new,
                        (acc,e)->acc.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }

    private CommentDTO toDTO(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setText(c.getText());
        dto.setAuthorEmail(c.getUser().getEmail());
        dto.setAuthorName(c.getUser().getEmail());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private String absolute(String relative) {
        if (relative.startsWith("http")) return relative;
        String sep = baseUrl.endsWith("/") ? "" : "/";
        return baseUrl + sep + relative;
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(", "))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream()
                .map(s -> s.trim().toLowerCase().replaceAll("[^a-z0-9-]", ""))
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
