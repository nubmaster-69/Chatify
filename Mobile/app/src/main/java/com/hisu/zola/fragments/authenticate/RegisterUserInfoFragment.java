package com.hisu.zola.fragments.authenticate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hisu.zola.MainActivity;
import com.hisu.zola.R;
import com.hisu.zola.database.entity.User;
import com.hisu.zola.database.repository.UserRepository;
import com.hisu.zola.databinding.FragmentRegisterUserInfoBinding;
import com.hisu.zola.fragments.greet_new_user.WelcomeOnBoardingFragment;
import com.hisu.zola.util.network.ApiService;
import com.hisu.zola.util.network.Constraints;
import com.hisu.zola.util.network.NetworkUtil;
import com.hisu.zola.util.RealPathUtil;
import com.hisu.zola.util.converter.ImageConvertUtil;
import com.hisu.zola.util.converter.ObjectConvertUtil;
import com.hisu.zola.util.dialog.LoadingDialog;
import com.hisu.zola.util.local.LocalDataManager;

import java.io.File;

import gun0912.tedimagepicker.builder.TedImagePicker;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterUserInfoFragment extends Fragment {

    public static final String REGISTER_KEY = "NEW_USER";
    private User user;
    private LoadingDialog loadingDialog;
    private UserRepository userRepository;
    private FragmentRegisterUserInfoBinding mBinding;
    private MainActivity mainActivity;
    private Uri avatarUri;
    private ActivityResultLauncher<Intent> resultLauncher;

    public static RegisterUserInfoFragment newInstance(User user) {
        Bundle args = new Bundle();
        args.putSerializable(REGISTER_KEY, user);
        RegisterUserInfoFragment fragment = new RegisterUserInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();

        if (getArguments() != null)
            user = (User) getArguments().getSerializable(REGISTER_KEY);
        else
            user = new User();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentRegisterUserInfoBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository(mainActivity.getApplication());
        init();
    }

    private void init() {
        loadingDialog = new LoadingDialog(mainActivity, Gravity.CENTER);

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        avatarUri = result.getData().getData();
                        mBinding.cimvAvatar.setImageURI(avatarUri);
                    }
                });

        generateDefaultPfp();
        addActionForBtnSkip();
        addActionForChangeAvatarButton();
        addActionForBtnSave();
    }

    private void generateDefaultPfp() {
        Bitmap bitmap = ImageConvertUtil.createImageFromText(mainActivity, 150, 150, user.getUsername());
        mBinding.cimvAvatar.setImageBitmap(bitmap);
    }

    private void addActionForBtnSkip() {
        mBinding.btnSkip.setOnClickListener(view -> {
            new iOSDialogBuilder(mainActivity)
                    .setTitle(getString(R.string.notification_warning))
                    .setSubtitle(getString(R.string.confirm_skip))
                    .setNegativeListener(getString(R.string.change), iOSDialog::dismiss)
                    .setPositiveListener(getString(R.string.skip), dialog -> {
                        dialog.dismiss();
                        updateProfile();
                    }).build().show();
        });
    }

    private void addActionForChangeAvatarButton() {
        mBinding.cimvAvatar.setOnClickListener(view -> {
            if(isCameraPermissionGranted()) {
                TedImagePicker.with(mainActivity)
                        .title(mainActivity.getString(R.string.pick_img))
                        .buttonText(mainActivity.getString(R.string.choose_as_pfp))
                        .image()
                        .start(uri -> {
                            mBinding.cimvAvatar.setImageURI(uri);
                            avatarUri = uri;
                        });
            } else {
                requestCameraPermission();
            }
        });
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        String[] permissions = {Manifest.permission.CAMERA};
        ActivityCompat.requestPermissions(mainActivity, permissions, Constraints.CAMERA_PERMISSION_CODE);
    }

    private void addActionForBtnSave() {
        mBinding.btnSave.setOnClickListener(view -> {
            updateProfile();
        });
    }

    private void updateProfile() {
        loadingDialog.showDialog();

        if (NetworkUtil.isConnectionAvailable(mainActivity)) {
            if (avatarUri != null) {

                File file = new File(RealPathUtil.getRealPath(mainActivity, avatarUri));
                RequestBody requestBody = RequestBody.create(MediaType.parse(Constraints.MULTIPART_FORM_DATA_TYPE), file);
                String fileName = file.getName();
                MultipartBody.Part part = MultipartBody.Part.createFormData("media", fileName, requestBody);

                ApiService.apiService.postImage(part).enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                        if (response.isSuccessful() && response.code() == 200) {

                            Gson gson = new Gson();

                            String json = gson.toJson(response.body());
                            JsonObject obj = gson.fromJson(json, JsonObject.class);
                            String avatarURL = obj.get("data").toString().replaceAll("\"", "");

                            user.setGender(mBinding.rBtnGenderMale.isChecked());
                            user.setAvatarURL(avatarURL);

                            updateUserProfile(user);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                        mainActivity.runOnUiThread(() -> {
                            loadingDialog.dismissDialog();
                            new iOSDialogBuilder(mainActivity)
                                    .setTitle(getString(R.string.notification_warning))
                                    .setSubtitle(getString(R.string.notification_warning_msg))
                                    .setPositiveListener(getString(R.string.confirm), iOSDialog::dismiss).build().show();
                        });
                        Log.e(RegisterUserInfoFragment.class.getName(), t.getLocalizedMessage());
                    }
                });
            } else {
                user.setGender(mBinding.rBtnGenderMale.isChecked());
                user.setAvatarURL("");
                updateUserProfile(user);
            }
        } else {
            new iOSDialogBuilder(mainActivity)
                    .setTitle(getString(R.string.no_network_connection))
                    .setSubtitle(getString(R.string.no_network_connection_desc))
                    .setPositiveListener(getString(R.string.confirm), iOSDialog::dismiss).build().show();
        }
    }

    private void updateUserProfile(User user) {
        ApiService.apiService.signUp(user).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {

                if (response.isSuccessful() && response.code() == 200) {

                    User newUser = ObjectConvertUtil.getResponseUser(response);

                    LocalDataManager.setUserLoginState(true);
                    LocalDataManager.setCurrentUserInfo(newUser);
                    userRepository.insertOrUpdate(newUser);

                    mainActivity.runOnUiThread(() -> {
                        loadingDialog.dismissDialog();
                        mainActivity.setBottomNavVisibility(View.GONE);
                        mainActivity.addFragmentToBackStack(new WelcomeOnBoardingFragment());
                    });
                } else {
                    mainActivity.runOnUiThread(() -> {
                        loadingDialog.dismissDialog();
                        new iOSDialogBuilder(mainActivity)
                                .setTitle(getString(R.string.notification_warning))
                                .setSubtitle(getString(R.string.notification_warning_msg))
                                .setPositiveListener(getString(R.string.confirm), iOSDialog::dismiss).build().show();
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                mainActivity.runOnUiThread(() -> {
                    loadingDialog.dismissDialog();
                    new iOSDialogBuilder(mainActivity)
                            .setTitle(getString(R.string.notification_warning))
                            .setSubtitle(getString(R.string.notification_warning_msg))
                            .setPositiveListener(getString(R.string.confirm), iOSDialog::dismiss).build().show();
                });
                Log.e(RegisterUserInfoFragment.class.getName(), t.getLocalizedMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}