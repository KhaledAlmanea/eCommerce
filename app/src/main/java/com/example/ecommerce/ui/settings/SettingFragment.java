package com.example.ecommerce.ui.settings;

import android.app.ProgressDialog;
import android.content.Intent;
import android.drm.ProcessedData;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.ecommerce.HomeActivity;
import com.example.ecommerce.MainActivity;
import com.example.ecommerce.Prevalent.Prevalent;
import com.example.ecommerce.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class SettingFragment extends Fragment {

    private CircleImageView profileImageView;
    private EditText fullNameEditText, userPhoneEditText, addressEditText;
    private TextView profileChangeTextBtn, closeTextBtn, saveTextBtn;

    private Uri imageUri;
    private String myUrl ="";
    private StorageTask uploadTask;
    private StorageReference storageProfilePictureRef;
    private String checker = "";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_setting, container, false);

        storageProfilePictureRef = FirebaseStorage.getInstance().getReference().child("Profile pictures");

        profileImageView = (CircleImageView) root.findViewById(R.id.settings_profile_image);
        fullNameEditText = (EditText) root.findViewById(R.id.settings_full_name);
        userPhoneEditText = (EditText) root.findViewById(R.id.settings_phone_number);
        addressEditText = (EditText) root.findViewById(R.id.settings_address);
        profileChangeTextBtn = (TextView) root.findViewById(R.id.profile_image_change_btn);
        closeTextBtn = (TextView) root.findViewById(R.id.close_settings_btn);
        saveTextBtn = (TextView) root.findViewById(R.id.update_account_settings_btn);

        userInfoDisplay(profileImageView, fullNameEditText, userPhoneEditText, addressEditText);

        closeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        saveTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checker.equals("clicked")){
                    userInfoSaved();
                }
                else {
                    updateUserInfo();
                }
            }
        });

        profileChangeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";
                // start cropping activity for pre-acquired image saved on the device
                CropImage.activity(imageUri)
                        .setAspectRatio(1, 1)
                        .start(getActivity(), SettingFragment.this);


            }
        });

        return root;
    }

    private void updateUserInfo() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("Users");
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", fullNameEditText.getText().toString());
        userMap.put("address", addressEditText.getText().toString());
        userMap.put("phoneOrder", userPhoneEditText.getText().toString());
        ref.child(Prevalent.currentOnlineUsers.getPhone()).updateChildren(userMap);

        startActivity(new Intent(getActivity(), HomeActivity.class));
        Toast.makeText(getActivity(), "Profile Info Updated Successfully",
                Toast.LENGTH_SHORT).show();
        getActivity().finish();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
                && resultCode==RESULT_OK && data!=null){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            profileImageView.setImageURI(imageUri);
        }

        else {
            Toast.makeText(getActivity(), "Error: Try Again", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(getActivity(), getClass()));
            getActivity().finish();
        }
    }

    private void userInfoSaved() {
        if (TextUtils.isEmpty(fullNameEditText.getText().toString())){
            Toast.makeText(getActivity(), "Name is Mandatory", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(userPhoneEditText.getText().toString())){
            Toast.makeText(getActivity(), "Phone is Mandatory", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(addressEditText.getText().toString())){
            Toast.makeText(getActivity(), "address is Mandatory", Toast.LENGTH_SHORT).show();
        }
        else if(checker.equals("clicked")){
            uploadImage();

        }
    }

    private void uploadImage() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Update Profile");
        progressDialog.setMessage("Please wait, while we are updating your account information");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        if (imageUri != null){
            final StorageReference fileRef = storageProfilePictureRef
                    .child(Prevalent.currentOnlineUsers.getPhone() + ".jpg");

            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                                .child("Users");
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("name", fullNameEditText.getText().toString());
                        userMap.put("address", addressEditText.getText().toString());
                        userMap.put("phoneOrder", userPhoneEditText.getText().toString());
                        userMap.put("image", myUrl);

                        ref.child(Prevalent.currentOnlineUsers.getPhone()).updateChildren(userMap);

                        progressDialog.dismiss();

                        startActivity(new Intent(getActivity(), HomeActivity.class));
                        Toast.makeText(getActivity(), "Profile Info Updated Successfully",
                                Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                    else {
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "Error.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else {
            Toast.makeText(getActivity(), "Image is not selected",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void userInfoDisplay(final CircleImageView profileImageView, final EditText fullNameEditText,
                                 final EditText userPhoneEditText, final EditText addressEditText) {
        DatabaseReference UserRef = FirebaseDatabase.getInstance().getReference().child("Users").
                child(Prevalent.currentOnlineUsers.getPhone());

        UserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()){
                    if(dataSnapshot.child("image").exists()){
                        String image = dataSnapshot.child("image").getValue().toString();
                        String name = dataSnapshot.child("name").getValue().toString();
                        String phone = dataSnapshot.child("phone").getValue().toString();
                        String address = dataSnapshot.child("address").getValue().toString();

                        Picasso.get().load(image).into(profileImageView);
                        fullNameEditText.setText(name);
                        userPhoneEditText.setText(phone);
                        addressEditText.setText(address);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
