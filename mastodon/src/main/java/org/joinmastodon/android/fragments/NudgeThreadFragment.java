package org.joinmastodon.android.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetNudgeThread;
import org.joinmastodon.android.api.requests.accounts.SendNudge;
import org.joinmastodon.android.api.requests.accounts.UploadNudgeVoice;
import org.joinmastodon.android.api.requests.statuses.UploadAttachment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.NudgeResult;
import org.joinmastodon.android.model.NudgeThreadMessage;
import org.joinmastodon.android.model.NudgeThreadResponse;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NudgeThreadFragment extends MastodonToolbarFragment {

	private static final int MEDIA_PICK_REQUEST = 1001;
	private static final int RECORD_AUDIO_PERMISSION = 1002;
	private static final int MAX_VOICE_SECONDS = 60;
	private static final int MAX_WORDS = 100;

	private String accountID;       // logged-in user account id
	private String partnerAccountId;
	private Account partnerAccount;

	private RecyclerView messageList;
	private ProgressBar progress;
	private EditText messageInput;
	private ImageButton attachBtn, voiceBtn, sendBtn;
	private ImageButton removeMediaBtn, removeVoiceBtn, voicePlayBtn;
	private View mediaPreviewContainer, voicePreviewContainer, recordingIndicator;
	private ImageView mediaPreviewImage;
	private VideoView mediaPreviewVideo;
	private TextView voicePreviewDuration, voiceTimer, wordCount;

	// Partner banner
	private View partnerBanner;
	private ImageView bannerAvatar;
	private TextView bannerName, bannerAcct, bannerStreak;

	private final List<NudgeThreadMessage> messages = new ArrayList<>();
	private MessageAdapter adapter;
	private boolean canNudgeBack;
	private int streak;

	// Compose state
	private String pendingMediaId;
	private Uri pendingMediaUri;
	private boolean pendingMediaIsVideo;
	private File voiceFile;
	private String pendingVoiceId;
	private int recordedSeconds;
	private boolean isPlaying;

	// Optimistic voice upload: starts immediately when recording stops
	private boolean voiceUploadInProgress = false;
	private boolean sendWhenUploadDone = false;

	// Recording
	private MediaRecorder mediaRecorder;
	private int voiceSeconds;
	private Handler timerHandler;
	private Runnable timerRunnable;

	// Voice playback
	private MediaPlayer mediaPlayer;

	public NudgeThreadFragment() {
		super();
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_nudge_thread, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
		partnerAccountId = getArguments().getString("partnerAccountId");
		timerHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(partnerAccountId);

		progress = view.findViewById(R.id.progress);
		messageList = view.findViewById(R.id.message_list);
		messageInput = view.findViewById(R.id.message_input);
		attachBtn = view.findViewById(R.id.attach_btn);
		voiceBtn = view.findViewById(R.id.voice_btn);
		sendBtn = view.findViewById(R.id.send_btn);
		removeMediaBtn = view.findViewById(R.id.remove_media_btn);
		removeVoiceBtn = view.findViewById(R.id.remove_voice_btn);
		voicePlayBtn = view.findViewById(R.id.voice_play_btn);
		mediaPreviewContainer = view.findViewById(R.id.media_preview_container);
		voicePreviewContainer = view.findViewById(R.id.voice_preview_container);
		recordingIndicator = view.findViewById(R.id.recording_indicator);
		mediaPreviewImage = view.findViewById(R.id.media_preview_image);
		mediaPreviewVideo = view.findViewById(R.id.media_preview_video);
		voicePreviewDuration = view.findViewById(R.id.voice_preview_duration);
		voiceTimer = view.findViewById(R.id.voice_timer);
		wordCount = view.findViewById(R.id.word_count);
		partnerBanner = view.findViewById(R.id.partner_banner);
		bannerAvatar = view.findViewById(R.id.banner_avatar);
		bannerName = view.findViewById(R.id.banner_name);
		bannerAcct = view.findViewById(R.id.banner_acct);
		bannerStreak = view.findViewById(R.id.banner_streak);

		adapter = new MessageAdapter();
		LinearLayoutManager llm = new LinearLayoutManager(getActivity());
		llm.setStackFromEnd(true);
		messageList.setLayoutManager(llm);
		messageList.setAdapter(adapter);

		attachBtn.setOnClickListener(v -> openMediaPicker());
		voiceBtn.setOnClickListener(v -> onVoiceBtnClick());
		sendBtn.setOnClickListener(v -> sendMessage());
		removeMediaBtn.setOnClickListener(v -> clearMedia());
		removeVoiceBtn.setOnClickListener(v -> clearVoice());
		voicePlayBtn.setOnClickListener(v -> toggleVoicePlayback());

		messageInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void afterTextChanged(Editable s) {
				updateWordCount();
				updateSendButton();
			}
		});

		loadThread();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		stopRecording(false);
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		timerHandler.removeCallbacksAndMessages(null);
		// Clean up any temp voice file left behind by an in-progress upload
		if (voiceFile != null) deleteVoiceFile();
	}

	private void loadThread() {
		progress.setVisibility(View.VISIBLE);
		messageList.setVisibility(View.GONE);
		new GetNudgeThread(partnerAccountId)
				.setCallback(new Callback<NudgeThreadResponse>() {
					@Override
					public void onSuccess(NudgeThreadResponse result) {
						if (getActivity() == null) return;
						if (result.account != null) {
							try { result.account.postprocess(); } catch (Exception ignored) {}
							partnerAccount = result.account;
							String displayName = partnerAccount.displayName == null || partnerAccount.displayName.isEmpty()
									? partnerAccount.username : partnerAccount.displayName;
							setTitle(displayName);
							getToolbar().setSubtitle("@" + partnerAccount.acct);
							bannerName.setText(displayName);
							bannerAcct.setText("@" + partnerAccount.acct);
							if (partnerAccount.avatar != null) {
								ViewImageLoader.load(bannerAvatar, null,
										new UrlImageLoaderRequest(partnerAccount.avatar, V.dp(40), V.dp(40)));
							}
							if (result.streak > 0) {
								bannerStreak.setText(getString(R.string.nudge_thread_streak, result.streak));
								bannerStreak.setVisibility(View.VISIBLE);
							} else {
								bannerStreak.setVisibility(View.GONE);
							}
							partnerBanner.setVisibility(View.VISIBLE);
						}
						messages.clear();
						if (result.messages != null) messages.addAll(result.messages);
						canNudgeBack = result.can_nudge_back;
						streak = result.streak;
						progress.setVisibility(View.GONE);
						messageList.setVisibility(View.VISIBLE);
						adapter.notifyDataSetChanged();
						messageList.scrollToPosition(messages.size() - 1);
						updateSendButton();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						progress.setVisibility(View.GONE);
						messageList.setVisibility(View.VISIBLE);
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	private void openMediaPicker() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
		startActivityForResult(intent, MEDIA_PICK_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MEDIA_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
			Uri uri = data.getData();
			if (uri == null) return;
			String mimeType = getActivity().getContentResolver().getType(uri);
			boolean isVideo = mimeType != null && mimeType.startsWith("video/");
			uploadMediaAndConfirm(uri, isVideo);
		}
	}

	private void uploadMediaAndConfirm(Uri uri, boolean isVideo) {
		attachBtn.setEnabled(false);
		new UploadAttachment(uri)
				.setCallback(new Callback<Attachment>() {
					@Override
					public void onSuccess(Attachment result) {
						if (getActivity() == null) return;
						attachBtn.setEnabled(true);
						// Messenger-style: go directly to compose bar, no confirm dialog
						pendingMediaId = result.id;
						pendingMediaUri = uri;
						pendingMediaIsVideo = isVideo;
						showMediaPreview();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						attachBtn.setEnabled(true);
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}


	private void showMediaPreview() {
		if (pendingMediaUri == null) return;
		mediaPreviewContainer.setVisibility(View.VISIBLE);
		if (pendingMediaIsVideo) {
			mediaPreviewImage.setVisibility(View.GONE);
			mediaPreviewVideo.setVisibility(View.VISIBLE);
			mediaPreviewVideo.setVideoURI(pendingMediaUri);
		} else {
			mediaPreviewVideo.setVisibility(View.GONE);
			mediaPreviewImage.setVisibility(View.VISIBLE);
			mediaPreviewImage.setImageURI(pendingMediaUri);
		}
		voiceBtn.setVisibility(View.GONE);
		updateSendButton();
	}

	private void showVoicePreview() {
		voicePreviewContainer.setVisibility(View.VISIBLE);
		voicePreviewDuration.setText(getString(R.string.nudge_voice_duration, recordedSeconds));
		attachBtn.setEnabled(false);
		voiceBtn.setVisibility(View.GONE);
		updateSendButton();
	}

	private void clearMedia() {
		pendingMediaId = null;
		pendingMediaUri = null;
		pendingMediaIsVideo = false;
		mediaPreviewContainer.setVisibility(View.GONE);
		mediaPreviewImage.setVisibility(View.GONE);
		mediaPreviewVideo.setVisibility(View.GONE);
		mediaPreviewVideo.stopPlayback();
		voiceBtn.setVisibility(View.VISIBLE);
		attachBtn.setEnabled(true);
		updateSendButton();
	}

	private void clearVoice() {
		pendingVoiceId = null;
		recordedSeconds = 0;
		voiceUploadInProgress = false;
		sendWhenUploadDone = false;
		voicePreviewContainer.setVisibility(View.GONE);
		if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
		deleteVoiceFile();
		attachBtn.setEnabled(true);
		voiceBtn.setVisibility(View.VISIBLE);
		updateSendButton();
	}

	private void deleteVoiceFile() {
		if (voiceFile != null) { voiceFile.delete(); voiceFile = null; }
	}

	// ── Voice recording ───────────────────────────────────────────────────────

	private void onVoiceBtnClick() {
		if (mediaRecorder != null) {
			stopRecording(true);
		} else {
			if (getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
					== PackageManager.PERMISSION_GRANTED) {
				startRecording();
			} else {
				requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
						RECORD_AUDIO_PERMISSION);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == RECORD_AUDIO_PERMISSION
				&& grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			startRecording();
		}
	}

	private void startRecording() {
		try {
			voiceFile = File.createTempFile("nudge_voice_", ".aac", getActivity().getCacheDir());
		} catch (IOException e) {
			return;
		}

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mediaRecorder.setAudioEncodingBitRate(128_000);
		mediaRecorder.setAudioSamplingRate(44_100);
		mediaRecorder.setAudioChannels(1);
		mediaRecorder.setOutputFile(voiceFile.getAbsolutePath());
		try {
			mediaRecorder.prepare();
			mediaRecorder.start();
		} catch (IOException e) {
			mediaRecorder.release();
			mediaRecorder = null;
			deleteVoiceFile();
			return;
		}

		voiceSeconds = 0;
		recordingIndicator.setVisibility(View.VISIBLE);
		voiceBtn.setImageResource(R.drawable.ic_close_20px);
		voiceBtn.setImageTintList(android.content.res.ColorStateList.valueOf(
				UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error)));
		attachBtn.setEnabled(false);
		sendBtn.setEnabled(false);

		timerRunnable = new Runnable() {
			@Override
			public void run() {
				voiceSeconds++;
				voiceTimer.setText(voiceSeconds + "s");
				if (voiceSeconds >= MAX_VOICE_SECONDS) {
					stopRecording(true);
				} else {
					timerHandler.postDelayed(this, 1000);
				}
			}
		};
		timerHandler.postDelayed(timerRunnable, 1000);
	}

	private void stopRecording(boolean save) {
		if (mediaRecorder == null) return;
		timerHandler.removeCallbacks(timerRunnable);
		try { mediaRecorder.stop(); } catch (RuntimeException ignored) {}
		mediaRecorder.release();
		mediaRecorder = null;

		recordingIndicator.setVisibility(View.GONE);
		voiceBtn.setImageResource(R.drawable.ic_mic_24px);
		voiceBtn.setImageTintList(android.content.res.ColorStateList.valueOf(
				UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant)));
		attachBtn.setEnabled(true);
		updateSendButton();

		if (save && voiceFile != null && voiceFile.exists()) {
			recordedSeconds = voiceSeconds;
			showVoicePreview();
			// Start uploading in background immediately so Send is instant
			voiceUploadInProgress = true;
			final File fileToUpload = voiceFile;
			new UploadNudgeVoice(fileToUpload)
					.setCallback(new Callback<Attachment>() {
						@Override
						public void onSuccess(Attachment result) {
							if (getActivity() == null) return;
							pendingVoiceId = result.id;
							voiceUploadInProgress = false;
							if (sendWhenUploadDone) {
								sendWhenUploadDone = false;
								String text = messageInput.getText().toString().trim();
								doSend(text, pendingMediaId, pendingVoiceId);
							}
						}
						@Override
						public void onError(ErrorResponse error) {
							if (getActivity() == null) return;
							voiceUploadInProgress = false;
							boolean wasSendPending = sendWhenUploadDone;
							sendWhenUploadDone = false;
							sendBtn.setEnabled(true);
							if (wasSendPending) {
								Toast.makeText(getActivity(),
										R.string.nudge_voice_upload_failed, Toast.LENGTH_SHORT).show();
							} else {
								error.showToast(getActivity());
							}
						}
					})
					.exec(accountID);
		} else {
			deleteVoiceFile();
		}
	}

	// ── Voice playback (preview in compose) ──────────────────────────────────

	private void toggleVoicePlayback() {
		if (voiceFile == null || !voiceFile.exists()) return;
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			voicePlayBtn.setImageResource(R.drawable.ic_play_24);
			isPlaying = false;
		} else {
			if (mediaPlayer == null) {
				mediaPlayer = new MediaPlayer();
				try {
					mediaPlayer.setDataSource(voiceFile.getAbsolutePath());
					mediaPlayer.prepare();
				} catch (IOException e) {
					mediaPlayer.release();
					mediaPlayer = null;
					return;
				}
				mediaPlayer.setOnCompletionListener(mp -> {
					voicePlayBtn.setImageResource(R.drawable.ic_play_24);
					isPlaying = false;
				});
			}
			mediaPlayer.start();
			voicePlayBtn.setImageResource(R.drawable.ic_pause_24);
			isPlaying = true;
		}
	}

	// ── Sending ───────────────────────────────────────────────────────────────

	private void sendMessage() {
		if (getActivity() == null) return;
		String text = messageInput.getText().toString().trim();
		boolean hasText = !text.isEmpty();
		boolean hasMedia = pendingMediaId != null;
		boolean hasVoice = voiceFile != null;

		if (!hasText && !hasMedia && !hasVoice && !canNudgeBack) return;

		sendBtn.setEnabled(false);

		if (hasVoice && pendingVoiceId == null) {
			if (voiceUploadInProgress) {
				// Background upload still running — queue send for when it completes
				sendWhenUploadDone = true;
			} else {
				// Fallback: upload now (shouldn't normally happen)
				new UploadNudgeVoice(voiceFile)
						.setCallback(new Callback<Attachment>() {
							@Override
							public void onSuccess(Attachment result) {
								if (getActivity() == null) return;
								pendingVoiceId = result.id;
								doSend(text, pendingMediaId, pendingVoiceId);
							}
							@Override
							public void onError(ErrorResponse error) {
								if (getActivity() == null) return;
								sendBtn.setEnabled(true);
								error.showToast(getActivity());
							}
						})
						.exec(accountID);
			}
		} else {
			doSend(text, pendingMediaId, pendingVoiceId);
		}
	}

	private void doSend(String text, String mediaId, String voiceId) {
		new SendNudge(partnerAccountId,
				text.isEmpty() ? null : text,
				mediaId,
				voiceId)
				.setCallback(new Callback<NudgeResult>() {
					@Override
					public void onSuccess(NudgeResult result) {
						if (getActivity() == null) return;
						clearCompose();
						streak = result.streak;
						canNudgeBack = result.can_nudge;
						loadThread();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						sendBtn.setEnabled(true);
						if (error instanceof org.joinmastodon.android.api.MastodonErrorResponse mr
								&& mr.httpStatus == 422) {
							Toast.makeText(getActivity(),
									R.string.nudge_waiting_for_reply, Toast.LENGTH_SHORT).show();
						} else {
							error.showToast(getActivity());
						}
					}
				})
				.exec(accountID);
	}

	private void clearCompose() {
		messageInput.setText("");
		pendingMediaId = null;
		pendingMediaUri = null;
		pendingMediaIsVideo = false;
		pendingVoiceId = null;
		recordedSeconds = 0;
		deleteVoiceFile();
		if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
		mediaPreviewContainer.setVisibility(View.GONE);
		mediaPreviewImage.setVisibility(View.GONE);
		mediaPreviewVideo.setVisibility(View.GONE);
		voicePreviewContainer.setVisibility(View.GONE);
		attachBtn.setEnabled(true);
		voiceBtn.setVisibility(View.VISIBLE);
		updateWordCount();
		updateSendButton();
	}

	private void updateWordCount() {
		String text = messageInput.getText().toString().trim();
		int wc = text.isEmpty() ? 0 : text.split("\\s+").length;
		if (wc > 0) {
			wordCount.setVisibility(View.VISIBLE);
			wordCount.setText(wc + " / " + MAX_WORDS);
			wordCount.setTextColor(wc > MAX_WORDS
					? UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error)
					: UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
		} else {
			wordCount.setVisibility(View.GONE);
		}
	}

	private void updateSendButton() {
		String text = messageInput.getText().toString().trim();
		int wc = text.isEmpty() ? 0 : text.split("\\s+").length;
		boolean overLimit = wc > MAX_WORDS;
		boolean hasContent = !text.isEmpty() || pendingMediaId != null || voiceFile != null;
		boolean canSend = !overLimit && (hasContent || canNudgeBack);
		sendBtn.setEnabled(canSend && mediaRecorder == null);

		if (hasContent) {
			sendBtn.setImageResource(R.drawable.ic_arrow_upward_20px);
		} else {
			sendBtn.setImageResource(R.drawable.ic_partner_exchange_24px);
		}
	}

	// ── Time formatting ───────────────────────────────────────────────────────

	private String formatTime(String isoStr) {
		if (isoStr == null) return "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date d = sdf.parse(isoStr);
			if (d == null) return "";
			long diff = System.currentTimeMillis() - d.getTime();
			if (diff < 60_000) return getString(R.string.nudge_time_now);
			return DateUtils.getRelativeTimeSpanString(d.getTime(),
					System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
		} catch (Exception e) {
			return "";
		}
	}

	// ── RecyclerView adapter ──────────────────────────────────────────────────

	private static final int TYPE_SENT     = 0;
	private static final int TYPE_RECEIVED = 1;
	private static final int TYPE_PING     = 2;

	private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@Override
		public int getItemViewType(int position) {
			NudgeThreadMessage msg = messages.get(position);
			boolean isPing = msg.body == null && msg.media_url == null && msg.voice_url == null;
			if (isPing) return TYPE_PING;
			return "sent".equals(msg.direction) ? TYPE_SENT : TYPE_RECEIVED;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inf = LayoutInflater.from(getActivity());
			return switch (viewType) {
				case TYPE_SENT     -> new BubbleViewHolder(inf.inflate(R.layout.item_nudge_message_sent, parent, false));
				case TYPE_RECEIVED -> new BubbleViewHolder(inf.inflate(R.layout.item_nudge_message_received, parent, false));
				default            -> new PingViewHolder(inf.inflate(R.layout.item_nudge_message_ping, parent, false));
			};
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			NudgeThreadMessage msg = messages.get(position);
			if (holder instanceof BubbleViewHolder bh) bh.bind(msg);
			else if (holder instanceof PingViewHolder ph) ph.bind(msg);
		}

		@Override
		public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
			super.onViewRecycled(holder);
			if (holder instanceof BubbleViewHolder bh) bh.recycle();
		}

		@Override
		public int getItemCount() { return messages.size(); }
	}

	private class BubbleViewHolder extends RecyclerView.ViewHolder {
		TextView textView, timeView, audioDuration;
		ImageView imageView, avatarView;
		VideoView videoView;
		View audioContainer;
		ImageButton audioPlayBtn;
		MediaPlayer bubblePlayer;

		BubbleViewHolder(View v) {
			super(v);
			textView = v.findViewById(R.id.message_text);
			timeView = v.findViewById(R.id.message_time);
			imageView = v.findViewById(R.id.message_image);
			videoView = v.findViewById(R.id.message_video);
			avatarView = v.findViewById(R.id.partner_avatar); // null for sent layout
			v.findViewById(R.id.message_video_container);
			audioContainer = v.findViewById(R.id.message_audio_container);
			audioPlayBtn = v.findViewById(R.id.audio_play_btn);
			audioDuration = v.findViewById(R.id.audio_duration);
		}

		void recycle() {
			if (bubblePlayer != null) {
				bubblePlayer.release();
				bubblePlayer = null;
			}
			if (videoView != null) videoView.stopPlayback();
		}

		void bind(NudgeThreadMessage msg) {
			recycle();
			if (avatarView != null && partnerAccount != null
					&& partnerAccount.avatar != null) {
				ViewImageLoader.load(avatarView, null,
						new UrlImageLoaderRequest(partnerAccount.avatar, V.dp(32), V.dp(32)));
			}
			// Reset all
			textView.setVisibility(View.GONE);
			imageView.setVisibility(View.GONE);
			if (videoView != null) {
				videoView.stopPlayback();
				videoView.setVisibility(View.GONE);
				if (v(R.id.message_video_container) != null)
					v(R.id.message_video_container).setVisibility(View.GONE);
			}
			audioContainer.setVisibility(View.GONE);

			if (msg.body != null && !msg.body.isEmpty()) {
				textView.setText(msg.body);
				textView.setVisibility(View.VISIBLE);
			}

			if (msg.media_url != null) {
				boolean isVideo = msg.media_content_type != null
						&& msg.media_content_type.startsWith("video/");
				if (isVideo && videoView != null) {
					itemView.findViewById(R.id.message_video_container).setVisibility(View.VISIBLE);
					videoView.setVisibility(View.VISIBLE);
					videoView.setVideoPath(msg.media_url);
					android.widget.MediaController mc = new android.widget.MediaController(itemView.getContext());
					mc.setAnchorView(videoView);
					videoView.setMediaController(mc);
					videoView.setOnPreparedListener(mp -> mp.setLooping(false));
				} else {
					imageView.setVisibility(View.VISIBLE);
					ViewImageLoader.load(imageView, null,
							new UrlImageLoaderRequest(msg.media_url, V.dp(220), V.dp(180)));
				}
			}

			if (msg.voice_url != null) {
				audioContainer.setVisibility(View.VISIBLE);
				audioDuration.setText(R.string.nudge_voice_memo);
				audioPlayBtn.setOnClickListener(btn -> {
					if (bubblePlayer != null && bubblePlayer.isPlaying()) {
						bubblePlayer.pause();
						audioPlayBtn.setImageResource(R.drawable.ic_play_24);
					} else {
						if (bubblePlayer == null) {
							bubblePlayer = new MediaPlayer();
							try {
								bubblePlayer.setDataSource(msg.voice_url);
								bubblePlayer.prepareAsync();
								bubblePlayer.setOnPreparedListener(MediaPlayer::start);
							} catch (IOException e) {
								bubblePlayer.release();
								bubblePlayer = null;
								return;
							}
							bubblePlayer.setOnCompletionListener(mp ->
									audioPlayBtn.setImageResource(R.drawable.ic_play_24));
						} else {
							bubblePlayer.start();
						}
						audioPlayBtn.setImageResource(R.drawable.ic_pause_24);
					}
				});
			}

			timeView.setText(formatTime(msg.created_at));
		}

		private View v(int id) { return itemView.findViewById(id); }
	}

	private class PingViewHolder extends RecyclerView.ViewHolder {
		TextView label, time;
		PingViewHolder(View v) {
			super(v);
			label = v.findViewById(R.id.ping_label);
			time = v.findViewById(R.id.ping_time);
		}
		void bind(NudgeThreadMessage msg) {
			boolean isSent = "sent".equals(msg.direction);
			label.setText(isSent ? getString(R.string.nudge_thread_you_nudged)
					: getString(R.string.nudge_thread_nudged_you));
			time.setText(formatTime(msg.created_at));
		}
	}
}
