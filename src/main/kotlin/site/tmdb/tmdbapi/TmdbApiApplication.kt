package site.tmdb.tmdbapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TmdbApiApplication

fun main(args: Array<String>) {
    runApplication<TmdbApiApplication>(*args)
}
