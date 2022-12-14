package com.hisu.zola.fragments.profile;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.gdacciaro.iOSDialog.iOSDialog;
import com.gdacciaro.iOSDialog.iOSDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hisu.zola.MainActivity;
import com.hisu.zola.R;
import com.hisu.zola.database.entity.User;
import com.hisu.zola.database.repository.UserRepository;
import com.hisu.zola.databinding.FragmentEditProfileBinding;
import com.hisu.zola.util.network.ApiService;
import com.hisu.zola.util.RealPathUtil;
import com.hisu.zola.util.converter.ImageConvertUtil;
import com.hisu.zola.util.dialog.LoadingDialog;
import com.hisu.zola.util.local.LocalDataManager;
import com.hisu.zola.util.network.Constraints;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import gun0912.tedimagepicker.builder.TedImagePicker;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding mBinding;
    private MainActivity mainActivity;
    private Calendar mCalendar;
    private Uri newAvatarUri;
    private ActivityResultLauncher<Intent> resultLauncher;
    private User currentUser;
    private boolean isGenderChanged;
    private LoadingDialog loadingDialog;
    private UserRepository repository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        currentUser = LocalDataManager.getCurrentUserInfo();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new UserRepository(mainActivity.getApplication());

        init();
        loadUserInfo();
        addActionForBtnBackToPrevPage();
        addActionForEditTextDateOfBirth();
        addActionForChangeAvatarButton();
        addActionForBtnSave();
        genderRadioGroupEvent();
    }

    private void init() {
        loadingDialog = new LoadingDialog(mainActivity, Gravity.CENTER);
        mainActivity.setBottomNavVisibility(View.GONE);
        mCalendar = Calendar.getInstance();

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        newAvatarUri = result.getData().getData();
                        mBinding.imvAvatar.setImageURI(result.getData().getData());
                    }
                });
    }

    private void loadUserInfo() {
        if (currentUser.getAvatarURL() == null || currentUser.getAvatarURL().isEmpty())
            mBinding.imvAvatar.setImageBitmap(ImageConvertUtil.createImageFromText(mainActivity, 150, 150, currentUser.getUsername()));
        else
            Glide.with(mainActivity).asBitmap()
                    .load(currentUser.getAvatarURL())
                    .placeholder(R.drawable.bg_profile).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            mBinding.imvAvatar.setImageBitmap(resource);
                            mBinding.imvAvatar.setVisibility(View.VISIBLE);
                        }
                    });

        mBinding.edtDisplayName.setText(currentUser.getUsername());
        mBinding.edtDisplayName.setHint(currentUser.getUsername());


        Date date = Date.from(Instant.parse(currentUser.getDob()));
        if (date != null) {
            mCalendar.setTime(date);
            updateDateOfBirthEditText();
        }

        if (currentUser.isGender())
            mBinding.rBtnGenderM.setChecked(true);
        else
            mBinding.rBtnGenderF.setChecked(true);

        isGenderChanged = currentUser.isGender();
    }

    private void addActionForBtnBackToPrevPage() {
        mBinding.iBtnBack.setOnClickListener(view -> {
            if (!isDataChanged()) {
                backToPrevPage();
            } else {
                new iOSDialogBuilder(mainActivity)
                        .setTitle(mainActivity.getString(R.string.notification_warning))
                        .setSubtitle(mainActivity.getString(R.string.changes_not_save))
                        .setPositiveListener(mainActivity.getString(R.string.yes), dialog -> {
                            dialog.dismiss();
                            backToPrevPage();
                        })
                        .setNegativeListener(mainActivity.getString(R.string.no), iOSDialog::dismiss).build().show();
            }
        });
    }

    private void backToPrevPage() {
        mainActivity.setBottomNavVisibility(View.VISIBLE);
        mainActivity.getSupportFragmentManager().popBackStackImmediate();
    }

    private boolean isDataChanged() {

        if (!mBinding.edtDisplayName.getText().toString().equalsIgnoreCase(currentUser.getUsername()))
            return true;

        if (newAvatarUri != null)
            return true;

        return isGenderChanged != currentUser.isGender();
    }

    private void genderRadioGroupEvent() {
        mBinding.radioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            int checkedId = radioGroup.getCheckedRadioButtonId();

            if (checkedId == mBinding.rBtnGenderM.getId())
                isGenderChanged = true;
            else if (checkedId == mBinding.rBtnGenderF.getId())
                isGenderChanged = false;
        });
    }

//    private void addActionForChangeAvatarButton() {
//        mBinding.imvAvatar.setOnClickListener(view -> {
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.setType("image/*");
//            resultLauncher.launch(intent);
//        });
//    }

    private void addActionForChangeAvatarButton() {
        mBinding.imvAvatar.setOnClickListener(view -> {
            if(isCameraPermissionGranted()) {
                TedImagePicker.with(mainActivity)
                        .title(mainActivity.getString(R.string.pick_img))
                        .buttonText(mainActivity.getString(R.string.choose_as_pfp))
                        .image()
                        .start(uri -> {
                            mBinding.imvAvatar.setImageURI(uri);
                            newAvatarUri = uri;
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

    private void addActionForEditTextDateOfBirth() {
        mBinding.edtDob.setOnClickListener(view -> {

            Locale locale = new Locale("vi");
            Locale.setDefault(locale);

            DatePickerDialog datePickerDialog = new DatePickerDialog(mainActivity,
                    android.R.style.Theme_Holo_Light_Dialog_MinWidth, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                    mCalendar.set(Calendar.YEAR, year);
                    mCalendar.set(Calendar.MONTH, month);
                    mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateOfBirthEditText();
                }
            }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.setTitle(mainActivity.getString(R.string.dob));
            datePickerDialog.setIcon(R.drawable.ic_calendar);
            datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, mainActivity.getString(R.string.cancel), datePickerDialog);
            datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, mainActivity.getString(R.string.confirm), datePickerDialog);
            datePickerDialog.show();
        });
    }

    private void updateDateOfBirthEditText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        mBinding.edtDob.setText(dateFormat.format(mCalendar.getTime()));
    }

    private void addActionForBtnSave() {
        mBinding.btnSave.setOnClickListener(view -> {
            confirmUpdateProfile();
        });
    }

    private void confirmUpdateProfile() {

        if (validateUserUpdateData()) {
            new iOSDialogBuilder(mainActivity)
                    .setTitle(mainActivity.getString(R.string.confirm_update_profile))
                    .setSubtitle(mainActivity.getString(R.string.confirm_update_profile_desc))
                    .setPositiveListener(mainActivity.getString(R.string.confirm), dialog -> {
                        dialog.dismiss();
                        updateProfile();
                    })
                    .setNegativeListener(mainActivity.getString(R.string.cancel), iOSDialog::dismiss).build().show();

        }
    }


    private String getDobFormat() {
        //2022-11-16
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return dateFormat.format(mCalendar.getTime()) + "T03:06:25.220Z";
    }

    private void updateProfile() {

        loadingDialog.showDialog();

        if (newAvatarUri != null) {
            File file = new File(RealPathUtil.getRealPath(mainActivity, newAvatarUri));
            RequestBody requestBody = RequestBody.create(MediaType.parse(Constraints.MULTIPART_FORM_DATA_TYPE), file);
            String fileName = file.getName();
            MultipartBody.Part part = MultipartBody.Part.createFormData("media", fileName, requestBody);

            ApiService.apiService.postImage(part).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                    if (response.isSuccessful()) {

                        Gson gson = new Gson();

                        String json = gson.toJson(response.body());
                        JsonObject obj = gson.fromJson(json, JsonObject.class);
                        String avatarURL = obj.get("data").toString().replaceAll("\"", "");

                        User user = LocalDataManager.getCurrentUserInfo();
                        user.setUsername(mBinding.edtDisplayName.getText().toString().trim());
                        user.setDob(getDobFormat());
                        user.setGender(mBinding.rBtnGenderM.isChecked());
                        user.setAvatarURL(avatarURL);

                        updateUserProfile(user);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                    Log.e(EditProfileFragment.class.getName(), t.getLocalizedMessage());
                }
            });
        } else {
            User user = LocalDataManager.getCurrentUserInfo();
            user.setUsername(mBinding.edtDisplayName.getText().toString().trim());
            user.setDob(getDobFormat());
            user.setGender(mBinding.rBtnGenderM.isChecked());

            if (user.getAvatarURL() != null)
                user.setAvatarURL(user.getAvatarURL());
            else
                user.setAvatarURL("");

            updateUserProfile(user);
        }
    }

    private void updateUserProfile(User user) {

        JsonObject userJson = new JsonObject();
        userJson.addProperty("username", user.getUsername());
        userJson.addProperty("avatarURL", user.getAvatarURL());
        userJson.addProperty("gender", user.isGender());
        userJson.addProperty("dob", user.getDob());

        RequestBody body = RequestBody.create(MediaType.parse(Constraints.JSON_TYPE), userJson.toString());
        ApiService.apiService.updateUser(body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful() && response.code() == 200) {
                    loadingDialog.dismissDialog();
                    LocalDataManager.setCurrentUserInfo(user);
                    repository.update(user);

                    new iOSDialogBuilder(mainActivity)
                            .setTitle(mainActivity.getString(R.string.notification_warning))
                            .setSubtitle(mainActivity.getString(R.string.update_self_info_success))
                            .setCancelable(false)
                            .setPositiveListener(mainActivity.getString(R.string.confirm), dialog -> {
                                dialog.dismiss();
                                currentUser = LocalDataManager.getCurrentUserInfo();
                                loadUserInfo();
                                backToPrevPage();
                            }).build().show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
            }
        });
    }

    /**
     * @author Huy
     */
    private boolean validateUserUpdateData() {

        if (TextUtils.isEmpty(mBinding.edtDisplayName.getText().toString().trim())) {
            mBinding.edtDisplayName.setError(mainActivity.getString(R.string.empty_err_displayname));
            mBinding.edtDisplayName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(mBinding.edtDob.getText().toString().trim())) {
            new iOSDialogBuilder(mainActivity)
                    .setTitle(mainActivity.getString(R.string.notification_warning))
                    .setSubtitle(mainActivity.getString(R.string.empty_dob_err))
                    .setPositiveListener(mainActivity.getString(R.string.confirm), iOSDialog::dismiss).build().show();
            return false;
        }

        if (calculateAge(mBinding.edtDob.getText().toString()) < 15) {
            new iOSDialogBuilder(mainActivity)
                    .setTitle(mainActivity.getString(R.string.notification_warning))
                    .setSubtitle(mainActivity.getString(R.string.err_age))
                    .setPositiveListener(mainActivity.getString(R.string.confirm), iOSDialog::dismiss).build().show();
            return false;
        }

        return true;
    }

    private static int calculateAge(String dobStr) {

        String[] dates = dobStr.trim().split("/");

        Date dob = new Date(
                Integer.parseInt(dates[2]),
                Integer.parseInt(dates[1]),
                Integer.parseInt(dates[0])
        );

        LocalDate today = LocalDate.now();
        Calendar birthDate = Calendar.getInstance();
        birthDate.setTime(dob);

        int age = today.getYear() - birthDate.get(Calendar.YEAR) + 1900;
        if (((birthDate.get(Calendar.MONTH)) > today.getMonthValue())) {
            age--;
        } else if ((birthDate.get(Calendar.MONTH) == today.getMonthValue()) &&
                (birthDate.get(Calendar.DAY_OF_MONTH) > today.getDayOfMonth())) {
            age--;
        }

        return age;
    }
}