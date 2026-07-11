package com.project.ciaklog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// Fix: mancava @EnableCaching — senza questa annotazione i @Cacheable su
// ChartServiceImpl (topFilms/topSeries/topUsers/trending) non hanno ALCUN
// effetto: Spring li ignora silenziosamente e ogni chiamata ricalcola da zero.
// Necessaria prima ancora di poter aggiungere la scadenza (@CacheEvict).
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class CiaklogApplication {

	public static void main(String[] args) {
		SpringApplication.run(CiaklogApplication.class, args);
	}
}
