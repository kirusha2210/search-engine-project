package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.Massage;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final IndexingService indexingService;
    private final StatisticsService statisticsService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Massage> startIndexing() throws IOException {
        return ResponseEntity.ok(indexingService.startIndexing());
    }
//TODO: проставить респонз окей
    @GetMapping("/stopIndexing")
    public ResponseEntity<Massage> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @SneakyThrows
    @PostMapping("/pageIndexing")
    public ResponseEntity<Massage> pageIndexing(@RequestBody String url) {
        return indexingService.pageIndexing(url);
    }

    @DeleteMapping("/deleteByPage")
    public ResponseEntity<Massage> deleteByPage(@RequestBody String url) throws IOException {
        return indexingService.deleteByPage(url);
    }
}
