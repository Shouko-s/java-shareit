package ru.practicum.server.item.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.server.item.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByItem_IdOrderByCreatedDesc(Long itemId);

    List<Comment> findAllByItem_IdIn(List<Long> itemIds);
}
