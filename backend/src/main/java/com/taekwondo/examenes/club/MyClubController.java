package com.taekwondo.examenes.club;

import com.taekwondo.examenes.user.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** El club del propio usuario, para cualquiera con sesión (RF-41). */
@RestController
@RequestMapping("/api/users/me/club")
public class MyClubController {

    private final ClubService clubService;

    public MyClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PutMapping
    public UserResponse choose(@RequestBody MyClubRequest request, @AuthenticationPrincipal Long userId) {
        return clubService.chooseMyClub(request.clubId(), userId);
    }
}
