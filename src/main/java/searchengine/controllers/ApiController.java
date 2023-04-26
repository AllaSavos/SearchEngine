package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    @Autowired
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

   /* @Autowired
    private SiteRepository siteRepository;*/

   /* public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }*/

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        //indexingService.parsingLinks();
        boolean indexing = indexingService.startIndexing();
        JSONObject response = new JSONObject();
        try {
            if (indexing) {
                response.put("result", false);
                response.put("error", "Индексация уже запущена");
            } else {
                response.put("result", true);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        boolean stopIndexing = indexingService.stopIndexing();
        JSONObject response = new JSONObject();

        try {
            if (stopIndexing) {
                response.put("result", false);
                response.put("error", "Индексация не запущена");
            } else {
                response.put("result", true);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

   /* @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() throws InterruptedException {
        return ResponseEntity.ok(indexingService.startIndexing());
    }
        - url: https://www.lenta.ru
      name: Лента.ру
    - url: https://www.skillbox.ru
      name: Skillbox
      https://www.playback.ru
      name: PlayBack.Ru
    */

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

}
