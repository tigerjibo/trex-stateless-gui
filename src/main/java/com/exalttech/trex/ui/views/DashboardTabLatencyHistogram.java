package com.exalttech.trex.ui.views;

import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exalttech.trex.ui.models.json.stats.latency.JSONStatsStream;
import com.exalttech.trex.ui.models.json.stats.latency.JSONStatsLatency;
import com.exalttech.trex.ui.views.services.RefreshingService;
import com.exalttech.trex.ui.views.statistics.StatsLoader;
import com.exalttech.trex.util.Constants;
import com.exalttech.trex.util.Initialization;
import com.exalttech.trex.util.Util;


public class DashboardTabLatencyHistogram extends AnchorPane {
    @FXML
    private AnchorPane root;
    @FXML
    private BarChart histogram;

    private RefreshingService refreshingService;

    public DashboardTabLatencyHistogram() {
        Initialization.initializeFXML(this, "/fxml/Dashboard/tabs/latency/DashboardTabLatencyHistogram.fxml");

        refreshingService = new RefreshingService();
        refreshingService.setPeriod(Duration.seconds(Constants.REFRESH_ONE_INTERVAL_SECONDS));
        refreshingService.setOnSucceeded(this::onRefreshSucceeded);
        refreshingService.start();

        Initialization.initializeCloseEvent(root, this::onWindowCloseRequest);
    }

    private void onRefreshSucceeded(WorkerStateEvent event) {
        Map<String, String> latencyStatsByStreams = StatsLoader.getInstance().getLatencyStatsMap();
        List<XYChart.Series> seriesList = new LinkedList<XYChart.Series>();

        latencyStatsByStreams.forEach((String stream, String jsonLatencyStats) -> {
            JSONStatsStream latencyStats = (JSONStatsStream) Util.fromJSONString(
                    jsonLatencyStats,
                    JSONStatsStream.class
            );
            if (latencyStats == null) {
                return;
            }

            JSONStatsLatency latency = latencyStats.getLatency();
            if (latency == null) {
                return;
            }

            XYChart.Series series = new XYChart.Series();
            series.setName(stream);
            latency.getHistogram().forEach((String key, Integer value) -> {
                series.getData().add(new XYChart.Data<String, Number>(key, value));
            });
            series.getData().sort(new Comparator<XYChart.Data<String, Number>>() {
                @Override
                public int compare(XYChart.Data<String, Number> o1, XYChart.Data<String, Number> o2) {
                    return Integer.parseInt(o1.getXValue()) - Integer.parseInt(o2.getXValue());
                }
            });
            seriesList.add(series);
        });

        histogram.getData().clear();
        histogram.getData().addAll(seriesList);
    }

    private void onWindowCloseRequest(WindowEvent window) {
        if (refreshingService.isRunning()) {
            refreshingService.cancel();
        }
    }
}
