package com.tao.fenuasmash.event;

record PoolStructure(Integer poolCount, Integer poolSize, Integer qualifiersPerPool) {

	static final PoolStructure NONE = new PoolStructure(null, null, null);

}
