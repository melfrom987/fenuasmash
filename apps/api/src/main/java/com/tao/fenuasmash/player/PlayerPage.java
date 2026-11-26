package com.tao.fenuasmash.player;

import java.util.List;

/** The `PlayerPage` schema. */
public record PlayerPage(List<PlayerResponse> items, int page, int size, long totalItems) {
}
