package com.abcde.hdbresale.controller;

import com.abcde.hdbresale.dto.BookmarkDto;
import com.abcde.hdbresale.security.CurrentUser;
import com.abcde.hdbresale.service.BookmarkService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Boundary class for UC-08. Requires sign-in (FR-24). */
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /** GET /api/bookmarks (UC-09). Signed-in users only. */
    @GetMapping
    public List<BookmarkDto> listBookmarks() {
        return bookmarkService.list(CurrentUser.require().userId());
    }

    /** Just the bookmarked flat ids, for filling in the star icons on result cards. */
    @GetMapping("/ids")
    public List<Long> bookmarkIds() {
        return bookmarkService.ids(CurrentUser.require().userId());
    }

    /** POST /api/bookmarks with {"flatId": n} (UC-08). 409 if already bookmarked. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addBookmark(@RequestBody Map<String, Long> body) {
        bookmarkService.add(CurrentUser.require().userId(), body.get("flatId"));
    }

    /** DELETE /api/bookmarks/{flatId} (UC-08). */
    @DeleteMapping("/{flatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@PathVariable long flatId) {
        bookmarkService.remove(CurrentUser.require().userId(), flatId);
    }
}
