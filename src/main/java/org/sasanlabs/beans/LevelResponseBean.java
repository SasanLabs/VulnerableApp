package org.sasanlabs.beans;

import org.sasanlabs.internal.utility.LevelEnum;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class LevelResponseBean implements Comparable<LevelResponseBean>{

	@JsonProperty("Level")
	private LevelEnum levelEnum;

	@JsonProperty("Description")
	private String description;

	public LevelEnum getLevelEnum() {
		return levelEnum;
	}

	public void setLevelEnum(LevelEnum levelEnum) {
		this.levelEnum = levelEnum;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int compareTo(LevelResponseBean levelResponseBean) {
		return this.levelEnum.ordinal() - levelResponseBean.levelEnum.ordinal();
	}
	
	
}
