package searchengine.controllers;

import jakarta.websocket.Encoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.Massage;
import searchengine.dto.search.SearchDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@ResponseBody
public class ApiController {

    private final IndexingService indexingService;
    private final StatisticsService statisticsService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Massage> startIndexing() throws IOException {
        return ResponseEntity.ok(indexingService.startIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Massage> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @SneakyThrows
    @PostMapping("/indexPage")
    public ResponseEntity<Massage> pageIndexing(@RequestBody() String page) {
        return ResponseEntity.ok(indexingService.pageIndexing(page));
    }

    @SneakyThrows
    @GetMapping("/search")
    public ResponseEntity<Massage> search(SearchDto searchDto) {
        return ResponseEntity.ok(searchService.search(searchDto.getQuery(),
                searchDto.getSite(), searchDto.getOffset(), searchDto.getLimit()));
    }
}
