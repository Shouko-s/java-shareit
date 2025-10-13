package ru.practicum.shareit.item.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    public CommentDto buildDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }

    public Comment buildEntity(CommentDto dto, Item item, User author) {
        return Comment.builder()
                .id(dto.getId())
                .text(dto.getText())
                .item(item)
                .author(author)
                .created(dto.getCreated() != null ? dto.getCreated() : java.time.LocalDateTime.now())
                .build();
    }

}
