package com.hisu.zola.fragments.conversation;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.ThreeBounce;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.hisu.zola.MainActivity;
import com.hisu.zola.R;
import com.hisu.zola.adapters.MessageAdapter;
import com.hisu.zola.database.entity.Conversation;
import com.hisu.zola.database.entity.Media;
import com.hisu.zola.database.entity.Message;
import com.hisu.zola.database.entity.User;
import com.hisu.zola.databinding.FragmentConversationBinding;
import com.hisu.zola.util.ApiService;
import com.hisu.zola.util.RealPathUtil;
import com.hisu.zola.util.SocketIOHandler;
import com.hisu.zola.util.local.LocalDataManager;
import com.hisu.zola.view_model.ConversationViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

import gun0912.tedimagepicker.builder.TedImagePicker;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationFragment extends Fragment {

    public static final String CONVERSATION_ARGS = "CONVERSATION_INFO";
    public static final String CONVERSATION_NAME_ARGS = "CONVERSATION_NAME";

    private FragmentConversationBinding mBinding;
    private MainActivity mMainActivity;
    private ConversationViewModel viewModel;
    private Socket mSocket;
    private Conversation conversation;
    private String conversationName;
    private boolean isToggleEmojiButton = false;

    public static ConversationFragment newInstance(Conversation conversation, String conversationName) {
        Bundle args = new Bundle();
        args.putSerializable(CONVERSATION_ARGS, conversation);
        args.putString(CONVERSATION_NAME_ARGS, conversationName);

        ConversationFragment fragment = new ConversationFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            conversation = (Conversation) getArguments().getSerializable(CONVERSATION_ARGS);
            conversationName = getArguments().getString(CONVERSATION_NAME_ARGS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainActivity = (MainActivity) getActivity();
        mBinding = FragmentConversationBinding.inflate(inflater, container, false);

        SocketIOHandler.getInstance().establishSocketConnection();

        mSocket = SocketIOHandler.getInstance().getSocketConnection();

        mSocket.on("msg-receive", onMessageReceive);
        mSocket.on("typing", onTyping);

        initRecyclerView();

        initEmojiKeyboard();
        initProgressBar();

        setConversationInfo(conversationName, getString(R.string.user_active));
        addActionForBackBtn();
        addActionForAudioCallBtn();
        addActionForVideoCallBtn();
        addActionForSideMenu();

        addActionForSendMessageBtn();
        addToggleShowSendIcon();

        mBinding.btnSendImg.setOnClickListener(view -> openBottomImagePicker());

        loadConversation(conversation.getId());

        return mBinding.getRoot();
    }

    private void initEmojiKeyboard() {
        mBinding.btnEmoji.setOnClickListener(view -> {
            isToggleEmojiButton = !isToggleEmojiButton;

            toggleEmojiButtonIcon();
        });
    }

    private void toggleEmojiButtonIcon() {
        if (isToggleEmojiButton) {
            mBinding.btnEmoji.setImageDrawable(ContextCompat.getDrawable(mMainActivity, R.drawable.ic_keyboard));
        } else {
            mBinding.btnEmoji.setImageDrawable(ContextCompat.getDrawable(mMainActivity, R.drawable.ic_sticker));
        }
    }

    private void openBottomImagePicker() {
        TedImagePicker.with(mMainActivity)
                .title(getString(R.string.pick_img))
                .buttonText(getString(R.string.send))
                .startMultiImage(uris -> {
                    uris.forEach(this::uploadFileToServer);
                });
    }

    private void uploadFileToServer(Uri uri) {

        File file = new File(RealPathUtil.getRealPath(mMainActivity, uri));
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        String fileName = file.getName();
        MultipartBody.Part part = MultipartBody.Part.createFormData("media", fileName, requestBody);

        ApiService.apiService.postImage(part).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {

                    Gson gson = new Gson();

                    String json = gson.toJson(response.body());
                    JsonObject obj = gson.fromJson(json, JsonObject.class);

                    sendMessageViaApi("", obj.get("data").toString().replaceAll("\"", ""), "image/" + fileName.substring(fileName.lastIndexOf('.') + 1), "image");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Log.e("ERR post img", t.getLocalizedMessage());
            }
        });
    }

    private void initProgressBar() {
        Sprite threeBounce = new ThreeBounce();
        threeBounce.setColor(ContextCompat.getColor(mMainActivity, R.color.primary_color));
        mBinding.progressBar.setIndeterminateDrawable(threeBounce);
    }

    private void initRecyclerView() {
        MessageAdapter messageAdapter = new MessageAdapter(mMainActivity);

        viewModel = new ViewModelProvider(mMainActivity).get(ConversationViewModel.class);

        viewModel.getData(conversation.getId()).observe(mMainActivity, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messageAdapter.setMessages(messages);
                mBinding.rvConversation.setAdapter(messageAdapter);
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mMainActivity);
        linearLayoutManager.setStackFromEnd(true);

        mBinding.rvConversation.setLayoutManager(
                linearLayoutManager
        );
    }

    private void setConversationInfo(String conversationName, String conversationStatus) {
        mBinding.tvUsername.setText(conversationName);
        mBinding.tvLastActive.setText(conversationStatus);
    }

    private void addActionForBackBtn() {
        mBinding.btnBack.setOnClickListener(view -> {
            mMainActivity.setProgressbarVisibility(View.VISIBLE);
            mMainActivity.setBottomNavVisibility(View.VISIBLE);
            mMainActivity.getSupportFragmentManager().popBackStackImmediate();
        });
    }

    private void addActionForAudioCallBtn() {
        mBinding.btnAudioCall.setOnClickListener(view -> {
            Toast.makeText(mMainActivity, "Audio call", Toast.LENGTH_SHORT).show();
        });
    }

    private void addActionForVideoCallBtn() {
        mBinding.btnVideoCall.setOnClickListener(view -> {
            Toast.makeText(mMainActivity, "Video call", Toast.LENGTH_SHORT).show();
        });
    }

    private void addActionForSideMenu() {
        mBinding.btnConversationMenu.setOnClickListener(view -> {
            mMainActivity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_left, R.anim.slide_out_left,
                            R.anim.slide_out_right, R.anim.slide_out_right)
                    .replace(
                            mMainActivity.getViewContainerID(),
                            ConversationDetailFragment.newInstance(getFriendInfo())
                    )
                    .addToBackStack(null)
                    .commit();
        });
    }

    private User getFriendInfo() {
        return conversation.getMember().stream()
                .filter(member -> !member.getId()
                        .equalsIgnoreCase(LocalDataManager.getCurrentUserInfo().getId()))
                .findAny().orElse(null);
    }

    private void addActionForSendMessageBtn() {
        mBinding.btnSend.setOnClickListener(view -> {
            sendMessageViaApi(mBinding.edtChat.getText().toString().trim(), "", "", "text");
        });
    }

    private void sendMessageViaApi(String text, String url, String imgType, String type) {

        JsonObject object = new JsonObject();
        Gson gson = new Gson();
        object.add("conversation", gson.toJsonTree(conversation));
        object.addProperty("sender", LocalDataManager.getCurrentUserInfo().getId());
        object.addProperty("text", text);
        object.addProperty("type", type);

        JsonObject media = new JsonObject();
        media.addProperty("url", url);
        media.addProperty("type", imgType);

        object.add("media", gson.toJsonTree(media));

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), object.toString());

        ApiService.apiService.sendMessage(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful() && response.code() == 200) {
                    String json = gson.toJson(response.body());

                    JsonObject obj = gson.fromJson(json, JsonObject.class);

                    Message message = gson.fromJson(obj.get("data"), Message.class);
                    sendMessage(message);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Log.e("API_ERR", t.getLocalizedMessage());
            }
        });
    }

    private void sendMessage(Message message) {
        if (!mSocket.connected()) {
            mSocket.connect();
        }

        Gson gson = new Gson();
        viewModel.insertOrUpdate(message);

        JsonObject emitMsg = new JsonObject();
        emitMsg.add("conversation", gson.toJsonTree(conversation));
        emitMsg.add("sender", gson.toJsonTree(LocalDataManager.getCurrentUserInfo()));
        emitMsg.addProperty("text", message.getText());
        emitMsg.addProperty("type", message.getType());
        emitMsg.add("media", gson.toJsonTree(message.getMedia()));

        mSocket.emit("send-msg", emitMsg);

        mBinding.edtChat.setText("");
        mBinding.edtChat.requestFocus();
    }

    private void closeSoftKeyboard() {
        InputMethodManager manager = (InputMethodManager) mMainActivity.getSystemService(
                Context.INPUT_METHOD_SERVICE
        );

        manager.hideSoftInputFromWindow(mBinding.edtChat.getWindowToken(), 0);
    }

    private void addToggleShowSendIcon() {
        mBinding.edtChat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int textLength = editable.toString().trim().length();

                if (textLength > 0) {
                    mBinding.btnSend.setVisibility(View.VISIBLE);
                    mBinding.btnSendImg.setVisibility(View.GONE);
                } else {
                    mBinding.btnSend.setVisibility(View.GONE);
                    mBinding.btnSendImg.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadConversation(String conversationID) {

        Executors.newSingleThreadExecutor().execute(() -> {

            JsonObject object = new JsonObject();
            object.addProperty("conversation", conversationID);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), object.toString());

            ApiService.apiService.getConversationMessages(body).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                    Gson gson = new Gson();

                    String json = gson.toJson(response.body());

                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    JsonArray array = obj.getAsJsonArray("data");

                    Message[] listArr = new Gson().fromJson(array, Message[].class);

                    for (Message message : listArr)
                        viewModel.insertOrUpdate(message);
                }

                @Override
                public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                    Log.e("API_ERR", t.getLocalizedMessage());
                }
            });
        });
    }

    private final Emitter.Listener onMessageReceive = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mMainActivity.runOnUiThread(() -> {
                JSONObject data = (JSONObject) args[0];
                if (data != null) {

                    try {

                        Gson gson = new Gson();

                        Conversation conversation = gson.fromJson(data.getString("conversation"), Conversation.class);

                        User sender = gson.fromJson(data.getString("sender"), User.class);

                        List<Media> media = gson.fromJson(data.get("media").toString(), new TypeToken<List<Media>>() {
                        }.getType());

                        Message message = new Message(data.getString("_id"), conversation.getId(), sender, data.getString("text"),
                                data.getString("type"), data.getString("createdAt"), data.getString("updatedAt"), media);

                        viewModel.insertOrUpdate(message);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private final Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mMainActivity.runOnUiThread(() -> {
                String data = (String) args[0];
                if (data != null) {
                    boolean isTyping = Boolean.parseBoolean(data.replaceAll("\"", ""));
                    if (isTyping)
                        mBinding.typing.setVisibility(View.VISIBLE);
                    else
                        mBinding.typing.setVisibility(View.GONE);
                }
            });
        }
    };
}