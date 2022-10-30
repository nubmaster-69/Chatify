package com.hisu.zola.fragments.authenticate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hisu.zola.MainActivity;
import com.hisu.zola.R;
import com.hisu.zola.databinding.FragmentResetPasswordBinding;
import com.hisu.zola.util.EditTextUtil;

import java.util.regex.Pattern;

public class ResetPasswordFragment extends Fragment {

    public static final String RESET_PWD_ARGS = "RESET_PWD_ARGS";
    public static final String FORGOT_PWD_ARGS = "FORGOT_PWD_ARGS";
    private static final String USER_OPTION_ARGS = "USER_OPTIONS";

    private FragmentResetPasswordBinding mBinding;
    private MainActivity mainActivity;
    private String arguments;

    public static ResetPasswordFragment newInstance(String argsValue) {
        Bundle args = new Bundle();
        args.putString(USER_OPTION_ARGS, argsValue);
        ResetPasswordFragment fragment = new ResetPasswordFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        if (getArguments() != null) {
            arguments = getArguments().getString(USER_OPTION_ARGS);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addChangeBackgroundColorOnFocusForPasswordEditText(mBinding.edtNewPwd, mBinding.linearLayout);

        addChangeBackgroundColorOnFocusForPasswordEditText(mBinding.edtConfirmNewPwd, mBinding.linearLayout2);

        addChangeBackgroundColorOnFocusForPasswordEditText(mBinding.edtOldPwd, mBinding.linearLayout3);

        addToggleShowPasswordEvent(mBinding.tvTogglePassword, mBinding.edtNewPwd);

        addToggleShowPasswordEvent(mBinding.tvToggleConfirmPassword, mBinding.edtConfirmNewPwd);

        addToggleShowPasswordEvent(mBinding.tvToggleOldPassword, mBinding.edtConfirmNewPwd);

        EditTextUtil.toggleShowClearIconOnEditText(mainActivity, mBinding.edtOldPwd);
        EditTextUtil.toggleShowClearIconOnEditText(mainActivity, mBinding.edtNewPwd);
        EditTextUtil.toggleShowClearIconOnEditText(mainActivity, mBinding.edtConfirmNewPwd);

        EditTextUtil.clearTextOnSearchEditText(mBinding.edtOldPwd);
        EditTextUtil.clearTextOnSearchEditText(mBinding.edtNewPwd);
        EditTextUtil.clearTextOnSearchEditText(mBinding.edtConfirmNewPwd);

        addActionForBtnBack();
        addActionForBtnSaveChangePwd();
    }

    private void addChangeBackgroundColorOnFocusForPasswordEditText(
            EditText editText,
            LinearLayout linearLayout
    ) {
        editText.setOnFocusChangeListener((view, isFocus) -> {
            if (isFocus)
                linearLayout.setBackground(
                        ContextCompat.getDrawable(mainActivity.getApplicationContext(),
                                R.drawable.edit_text_outline_focus));
            else
                linearLayout.setBackground(
                        ContextCompat.getDrawable(mainActivity.getApplicationContext(),
                                R.drawable.edit_text_outline));
        });
    }

    private void addToggleShowPasswordEvent(TextView tvTogglePwd, EditText editText) {
        String showText = getString(R.string.show);
        String hideText = getString(R.string.hide);

        tvTogglePwd.setOnClickListener(view -> {

            if (tvTogglePwd.getText().toString().equalsIgnoreCase(showText)) {
                tvTogglePwd.setText(hideText);
                editText.setTransformationMethod(null);
            } else {
                tvTogglePwd.setText(showText);
                editText.setTransformationMethod(new PasswordTransformationMethod());
            }

            editText.setSelection(editText.getText().length());
        });
    }

    private void addActionForBtnBack() {
        mBinding.iBtnBack.setOnClickListener(view -> {
            mainActivity.getSupportFragmentManager().popBackStack();
        });
    }

    private void addActionForBtnSaveChangePwd() {
        mBinding.btnSave.setOnClickListener(view -> {
            if (validateNewPassword(mBinding.edtNewPwd.getText().toString().trim(),
                    mBinding.edtConfirmNewPwd.getText().toString().trim())) {
                updateUserInfo(mBinding.edtNewPwd.getText().toString().trim());
            }
        });
    }

    private void updateUserInfo(String newPwd) {
        //Todo: call api to update

        new AlertDialog.Builder(mainActivity)
                .setMessage(getString(R.string.reset_password_success))
                .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) ->
                        switchToNextPage()
                ).show();
    }

    private void switchToNextPage() {
        switch (arguments) {
            case RESET_PWD_ARGS:
                mainActivity.getSupportFragmentManager().popBackStackImmediate();
                break;

            case FORGOT_PWD_ARGS:
                mainActivity.setFragment(new LoginFragment());
                break;
        }
    }

    /**
     * @author Huy
     */
    private boolean validateNewPassword(String newPwd, String confirmPwd) {
        if (TextUtils.isEmpty(newPwd)) {
            mBinding.edtNewPwd.setError(getString(R.string.empty_pwd_err));
            mBinding.edtNewPwd.requestFocus();
            return false;
        }

        Pattern patternPwd = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?])[A-Za-z\\d@$!%*?]{8,}$");
        if (!patternPwd.matcher(newPwd).matches()) {
            mBinding.edtNewPwd.setError(getString(R.string.invalid_pwd_format_err));
            mBinding.edtNewPwd.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPwd)) {
            mBinding.edtConfirmNewPwd.setError(getString(R.string.empty_confirm_pwd_err));
            mBinding.edtConfirmNewPwd.requestFocus();
            return false;
        }

        if (!newPwd.equals(confirmPwd)) {
            mBinding.edtConfirmNewPwd.setError(getString(R.string.not_match_confirm_pwd_err));
            mBinding.edtConfirmNewPwd.requestFocus();
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}