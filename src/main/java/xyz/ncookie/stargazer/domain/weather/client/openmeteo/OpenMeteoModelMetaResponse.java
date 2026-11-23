package xyz.ncookie.stargazer.domain.weather.client.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Open-Meteo /data/{model}/static/meta.json 응답 가운데 쓰는 부분. 시각은 모두 UTC 초 단위다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoModelMetaResponse(
	@JsonProperty("last_run_initialisation_time") Long lastRunInitialisationTime,
	@JsonProperty("update_interval_seconds") Long updateIntervalSeconds
) {}
