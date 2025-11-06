package dev.douglas.lesson3;

import java.util.List;

public record ArtistBean(String artistName, String origin, String style, List<MusicBean> topSongs) {}
