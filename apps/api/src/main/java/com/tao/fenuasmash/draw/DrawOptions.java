package com.tao.fenuasmash.draw;

/**
 * Settings of a draw; a null value uses the default of `contracts/openapi.yaml` (`createDraw`).
 *
 * @param poolCount number of pools, for pools then knockout
 * @param qualifiersPerPool entries of each pool going to the knockout bracket
 * @param seedCount number of seeds, for a single elimination
 */
record DrawOptions(Integer poolCount, Integer qualifiersPerPool, Integer seedCount) {

	static final DrawOptions DEFAULTS = new DrawOptions(null, null, null);

}
