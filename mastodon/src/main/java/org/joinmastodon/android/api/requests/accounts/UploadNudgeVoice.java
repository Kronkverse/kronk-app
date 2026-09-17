package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Attachment;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class UploadNudgeVoice extends MastodonAPIRequest<Attachment> {
	private final File file;

	public UploadNudgeVoice(File file) {
		super(HttpMethod.POST, "/media", Attachment.class);
		this.file = file;
	}

	@Override
	protected String getPathPrefix() {
		return "/api/v2";
	}

	@Override
	public RequestBody getRequestBody() throws IOException {
		return new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file", "voice.aac",
						RequestBody.create(MediaType.parse("audio/aac"), file))
				.build();
	}
}
