package com.tao.fenuasmash.event;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.tao.fenuasmash.tournament.Discipline;

/**
 * A class rather than a record: a setter is only called when its field is present in the
 * JSON body, which tells an absent field (kept) from a null one (cleared).
 */
public class UpdateEventRequest {

	@Size(max = 100)
	@Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
	private String name;

	private Discipline discipline;

	private CategoryType categoryType;

	@Size(max = 50)
	private String categoryLabel;

	private Instant plannedDate;

	private boolean plannedDateSent;

	@Min(2)
	@Max(256)
	private Integer maxEntries;

	private boolean maxEntriesSent;

	@Min(1)
	private Integer poolCount;

	@Min(3)
	@Max(8)
	private Integer poolSize;

	@Min(1)
	@Max(7)
	private Integer qualifiersPerPool;

	public void setName(String name) {
		this.name = name;
	}

	public void setDiscipline(Discipline discipline) {
		this.discipline = discipline;
	}

	public void setCategoryType(CategoryType categoryType) {
		this.categoryType = categoryType;
	}

	public void setCategoryLabel(String categoryLabel) {
		this.categoryLabel = categoryLabel;
	}

	public void setPlannedDate(Instant plannedDate) {
		this.plannedDate = plannedDate;
		this.plannedDateSent = true;
	}

	public void setMaxEntries(Integer maxEntries) {
		this.maxEntries = maxEntries;
		this.maxEntriesSent = true;
	}

	public void setPoolCount(Integer poolCount) {
		this.poolCount = poolCount;
	}

	public void setPoolSize(Integer poolSize) {
		this.poolSize = poolSize;
	}

	public void setQualifiersPerPool(Integer qualifiersPerPool) {
		this.qualifiersPerPool = qualifiersPerPool;
	}

	String name() {
		return name;
	}

	Discipline discipline() {
		return discipline;
	}

	CategoryType categoryType() {
		return categoryType;
	}

	String categoryLabel() {
		return categoryLabel;
	}

	Instant plannedDate() {
		return plannedDate;
	}

	boolean plannedDateSent() {
		return plannedDateSent;
	}

	Integer maxEntries() {
		return maxEntries;
	}

	boolean maxEntriesSent() {
		return maxEntriesSent;
	}

	Integer poolCount() {
		return poolCount;
	}

	Integer poolSize() {
		return poolSize;
	}

	Integer qualifiersPerPool() {
		return qualifiersPerPool;
	}

}
