package org.example.backend.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface UserAccountMapper {

    record Credentials(Long id, String username, String displayName, String role, String status,
                       String passwordHash, OffsetDateTime updatedAt) {
        AccountView toView() {
            return new AccountView(id, username, displayName, role, status);
        }
    }

    record SessionState(String role, String status, OffsetDateTime updatedAt) {
    }

    Credentials findCredentials(@Param("username") String username);

    SessionState findSessionState(@Param("id") Long id);

    AccountView findViewById(@Param("id") Long id);

    AccountView findViewByUsername(@Param("username") String username);

    List<AccountView> list(@Param("role") String role, @Param("status") String status);

    int insertOwner(@Param("username") String username, @Param("passwordHash") String passwordHash,
                    @Param("displayName") String displayName);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
