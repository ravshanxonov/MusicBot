package org.example.musicbot.repo;



import org.example.musicbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User,Integer> {
  User findByChatId(Long chatId);

}
