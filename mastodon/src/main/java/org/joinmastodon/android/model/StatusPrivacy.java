package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum StatusPrivacy{
	@SerializedName("public")
	PUBLIC(0),
	@SerializedName("unlisted")
	UNLISTED(1),
	// Kronk's reach ladder. The server has carried these since the 2.0
	// rebuild, and its migrations remap existing posts onto them —
	// `direct` and `unlisted` become `self_only`, `private` becomes
	// `mates`. Without these constants GSON maps the strings to null and
	// the first switch over a status's visibility throws; the timeline
	// binds one per row.
	//
	// ORBIT is mates-of-mates, so it sits between a quiet public post and
	// followers-only. MATES is mutual follows, a subset of followers, so
	// it is narrower than PRIVATE. SELF_ONLY is nobody.
	@SerializedName("orbit")
	ORBIT(2),
	@SerializedName("private")
	PRIVATE(3),
	@SerializedName("mates")
	MATES(4),
	@SerializedName("direct")
	DIRECT(5),
	@SerializedName("self_only")
	SELF_ONLY(6);

	private int privacy;

	StatusPrivacy(int privacy) {
		this.privacy = privacy;
	}

	public boolean isLessVisibleThan(StatusPrivacy other) {
		return privacy > other.getPrivacy();
	}

	public int getPrivacy() {
		return privacy;
	}

	/** True for the reach-ladder values the composer cannot author. */
	public boolean isKronkReach(){
		return this==ORBIT || this==MATES || this==SELF_ONLY;
	}
}
