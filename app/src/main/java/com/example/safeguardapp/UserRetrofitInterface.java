package com.example.safeguardapp;

import com.example.safeguardapp.FindID.FindIDRequest;
import com.example.safeguardapp.FindID.FindIDResponse;
import com.example.safeguardapp.FindPW.CodeRequest;
import com.example.safeguardapp.FindPW.EmailRequest;
import com.example.safeguardapp.FindPW.ResetPwRequest;
import com.example.safeguardapp.Group.ChildDTO;
import com.example.safeguardapp.Group.DangerSectorRequest;
import com.example.safeguardapp.Group.GroupRemoveRequest;
import com.example.safeguardapp.Group.ResetChildPWRequest;
import com.example.safeguardapp.Group.SafeSectorRequest;
import com.example.safeguardapp.LogIn.LoginRequest;
import com.example.safeguardapp.LogIn.LoginResponse;
import com.example.safeguardapp.SignUp.SignUpRequestDTO;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserRetrofitInterface {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("signup")
    Call<ResponseBody> signUp(@Body SignUpRequestDTO jsonUser);

    @POST("find-member-id")
    Call<FindIDResponse> findID(@Body FindIDRequest request);

    @POST("verification-email-request")
    Call<ResponseBody> sendCode(@Body EmailRequest jsonUser);

    @POST("verification-email")
    Call<ResponseBody> codeCheck(@Body CodeRequest jsonUser);

    @POST("reset-member-password")
    Call<ResponseBody> resetPw(@Body ResetPwRequest jsonUser);

    @POST("childsignup")
    Call<ResponseBody> child(@Body ChildDTO jsonUser);

    @POST("add-safe")
    Call<ResponseBody> sectorSafe(@Body SafeSectorRequest jsonUser);

    @POST("add-dangerous")
    Call<ResponseBody> sectorDanger(@Body DangerSectorRequest jsonUser);

    @POST("chose-child")
    Call<ResponseBody> childResetPW(@Body ResetChildPWRequest jsonUser);

    @POST("childremove")
    Call<ResponseBody> removeGroup(@Body GroupRemoveRequest jsonUser);
}
