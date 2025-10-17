package ru.practicum.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import ru.practicum.server.exception.AlreadyExists;
import ru.practicum.server.exception.NotFoundException;
import ru.practicum.server.user.dto.UserDto;
import ru.practicum.server.user.mapper.UserMapper;
import ru.practicum.server.user.model.User;
import ru.practicum.server.user.repository.UserRepository;
import ru.practicum.server.user.service.UserServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = UserServiceImpl.class)
@Import(UserMapper.class)
class UserServiceTests {

    @MockBean
    private UserRepository userRepository;

    private static User user(long id, String name, String email) {
        return User.builder().id(id).name(name).email(email).build();
    }

    @Test
    @DisplayName("updateUserById: обновление только имени — успех; email не меняем")
    void update_onlyName_success() {
        long id = 10L;
        User existing = user(id, "Old Name", "old@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto patch = UserDto.builder().name("New Name").build();
        UserDto updated = service.updateUserById(id, patch);

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("old@example.com");

        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).findByEmail(null);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserById: смена email на свободный — успех")
    void update_email_success() {
        long id = 11L;
        User existing = user(id, "User", "old@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto patch = UserDto.builder().email("new@example.com").build();
        UserDto updated = service.updateUserById(id, patch);

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getName()).isEqualTo("User");

        verify(userRepository).findByEmail("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserById: конфликт email — AlreadyExists")
    void update_email_conflict() {
        long id = 12L;
        User existing = user(id, "User", "old@example.com");
        User someone = user(99L, "Other", "taken@example.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(someone));

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto patch = UserDto.builder().email("taken@example.com").build();

        assertThatThrownBy(() -> service.updateUserById(id, patch))
                .isInstanceOf(AlreadyExists.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserById: пользователь не найден — NotFoundException")
    void update_user_notFound() {
        when(userRepository.findById(777L)).thenReturn(Optional.empty());

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto patch = UserDto.builder().name("X").email("x@example.com").build();

        assertThatThrownBy(() -> service.updateUserById(777L, patch))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    // -------------------- ДОБАВЛЕНО: create --------------------

    @Test
    @DisplayName("create: успех при свободном email — сохраняет и возвращает DTO")
    void create_success() {
        UserDto input = UserDto.builder().name("Alice").email("a@ex.com").build();
        User saved = user(100L, "Alice", "a@ex.com");

        when(userRepository.findByEmail("a@ex.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto out = service.create(input);

        assertThat(out.getId()).isEqualTo(100L);
        assertThat(out.getName()).isEqualTo("Alice");
        assertThat(out.getEmail()).isEqualTo("a@ex.com");

        verify(userRepository).findByEmail("a@ex.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("create: email уже занят — AlreadyExists, сохранения нет")
    void create_conflict() {
        UserDto input = UserDto.builder().name("Bob").email("b@ex.com").build();
        User existing = user(1L, "Other", "b@ex.com");

        when(userRepository.findByEmail("b@ex.com")).thenReturn(Optional.of(existing));

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        assertThatThrownBy(() -> service.create(input))
                .isInstanceOf(AlreadyExists.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any(User.class));
    }

    // -------------------- ДОБАВЛЕНО: getUserById --------------------

    @Test
    @DisplayName("getUserById: найден — маппится в DTO")
    void getById_success() {
        long id = 55L;
        User u = user(id, "Carl", "c@ex.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(u));

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        UserDto out = service.getUserById(id);

        assertThat(out.getId()).isEqualTo(55L);
        assertThat(out.getName()).isEqualTo("Carl");
        assertThat(out.getEmail()).isEqualTo("c@ex.com");

        verify(userRepository).findById(id);
    }

    @Test
    @DisplayName("getUserById: не найден — NotFoundException")
    void getById_notFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        UserServiceImpl service = new UserServiceImpl(new UserMapper(), userRepository);

        assertThatThrownBy(() -> service.getUserById(404L))
                .isInstanceOf(NotFoundException.class);

        // никаких save не должно быть
        verify(userRepository, never()).save(any(User.class));
    }
}
