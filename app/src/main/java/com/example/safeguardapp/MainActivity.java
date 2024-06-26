package com.example.safeguardapp;

import static com.naver.maps.map.NaverMap.MapType.Basic;
import static com.naver.maps.map.NaverMap.MapType.Hybrid;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.safeguardapp.Emergency.MyEmergencyFragment;
import com.example.safeguardapp.Group.AddGroupPopupFragment;
import com.example.safeguardapp.Group.FindChildPWCertFragment;
import com.example.safeguardapp.Group.FindChildPWFragment;
import com.example.safeguardapp.Group.GetChildIDRequest;
import com.example.safeguardapp.Group.GroupFragment;
import com.example.safeguardapp.Group.GroupSettingFragment;
import com.example.safeguardapp.Group.Sector.SectorDetails;
import com.example.safeguardapp.Group.Sector.SectorInquireRequest;
import com.example.safeguardapp.Group.Sector.SectorMapFragment;
import com.example.safeguardapp.LogIn.LoginPageFragment;
import com.example.safeguardapp.Map.LocationRequest;
import com.example.safeguardapp.Map.LocationResponse;
import com.example.safeguardapp.Notice.NoticeFragment;
import com.example.safeguardapp.Setting.LoadImageRequest;
import com.example.safeguardapp.Setting.SettingFragment;
import com.example.safeguardapp.data.repository.GroupRepository;
import com.example.safeguardapp.data.repository.OtherGroupRepository;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    RetrofitClient retrofitClient;
    UserRetrofitInterface userRetrofitInterface;
    public MapFragment mapFragment;
    public GroupFragment groupFragment;
    public NoticeFragment noticeFragment;
    public SettingFragment settingFragment;
    private ImageButton emergencyBtn;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap mNaverMap;
    public static double latitude, longitude, battery;
    private Boolean doubleBackToExitPressedOnce = false;
    private Intent serviceIntent;

    // member와 연결되어 있는 chlid 위치 관리하는 자료구조
    private ArrayList<String> childList = new ArrayList<>();
    private HashMap<Integer, String> dynamicVariables = new HashMap<>();
    private HashMap<String, Marker> childMarkers = new HashMap<>();

    // helper와 연결되어 있는 child 위치 관리하는 자료구조
    private ArrayList<String> childList2 = new ArrayList<>();
    private HashMap<Integer, String> dynamicVariables2 = new HashMap<>();
    private HashMap<String, Marker> childMarkers2 = new HashMap<>();
    private HashMap<String, Bitmap> childImages = new HashMap<>();

    private String markerName;

    private List<PolygonOverlay> polygonOverlays = new ArrayList<>();
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
    };

    private Handler handler;
    private Runnable updateMarkerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        retrofitClient = RetrofitClient.getInstance();
        userRetrofitInterface = RetrofitClient.getInstance().getUserRetrofitInterface();

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // -----v----- BottomNavigationView 구현 -----v-----
        mapFragment = new MapFragment();
        groupFragment = new GroupFragment();
        noticeFragment = new NoticeFragment();
        settingFragment = new SettingFragment();

        emergencyBtn = findViewById(R.id.add_emergency_btn);
        emergencyBtn.setOnClickListener(v -> emergency());

        NavigationBarView navigationBarView = findViewById(R.id.bottom_navigationview);
        getSupportFragmentManager().beginTransaction().replace(R.id.containers, mapFragment).commit();
        navigationBarView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.containers);

                if (item.getItemId() == R.id.map) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
                    transaction.replace(R.id.containers, mapFragment).commit();
                    mapFragment.getMapAsync(MainActivity.this);
                    // mapFragment를 실행시 mapModeNav 보이게 설정
                    LinearLayout mapModeNav = findViewById(R.id.mapModeNav);
                    mapModeNav.setVisibility(View.VISIBLE);
                    // mapFragment를 실행시 나침반 보이게 설정
                    CompassView compassView = findViewById(R.id.compass);
                    compassView.setVisibility(View.VISIBLE);

                    emergencyBtn.setVisibility(View.VISIBLE);
                    return true;
                } else if (item.getItemId() == R.id.setting) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    transaction.replace(R.id.containers, settingFragment).commit();
                    // SettingFragment를 실행시 mapModeNav를 사라지게 설정
                    LinearLayout mapModeNav = findViewById(R.id.mapModeNav);
                    mapModeNav.setVisibility(View.GONE);
                    // SettingFragment를 실행시 나침반 안 보이게 설정
                    CompassView compassView = findViewById(R.id.compass);
                    compassView.setVisibility(View.GONE);

                    emergencyBtn.setVisibility(View.GONE);
                    return true;
                } else if (item.getItemId() == R.id.notice) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    if (currentFragment instanceof SettingFragment) {
                        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
                    } else {
                        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                    transaction.replace(R.id.containers, noticeFragment).commit();
                    // NoticeFragment를 실행시 mapModeNav를 사라지게 설정
                    LinearLayout mapModeNav = findViewById(R.id.mapModeNav);
                    mapModeNav.setVisibility(View.GONE);
                    // NoticeFragment를 실행시 나침반 안 보이게 설정
                    CompassView compassView = findViewById(R.id.compass);
                    compassView.setVisibility(View.GONE);

                    emergencyBtn.setVisibility(View.GONE);
                    return true;
                } else if (item.getItemId() == R.id.group) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    if (currentFragment instanceof NoticeFragment || currentFragment instanceof SettingFragment) {
                        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
                    } else if (currentFragment instanceof GroupSettingFragment || currentFragment instanceof SectorMapFragment || currentFragment instanceof FindChildPWFragment || currentFragment instanceof FindChildPWCertFragment) {
                        transaction.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top, R.anim.slide_in_top, R.anim.slide_out_bottom);
                    } else if (currentFragment instanceof AddGroupPopupFragment) {
                        transaction.setCustomAnimations(R.anim.slide_in_top, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_top);
                    } else {
                        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                    transaction.replace(R.id.containers, groupFragment).commit();
                    // GroupFragment를 실행시 mapModeNav를 사라지게 설정
                    LinearLayout mapModeNav = findViewById(R.id.mapModeNav);
                    mapModeNav.setVisibility(View.GONE);
                    // GroupFragment를 실행시 나침반 안 보이게 설정
                    CompassView compassView = findViewById(R.id.compass);
                    compassView.setVisibility(View.GONE);

                    emergencyBtn.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });
        // -----^----- BottomNavigationView 구현 -----^-----

        //getMapAsync 호출해 비동기로 onMapReady 콜백 메서드 호출
        //onMapReady에서 NaverMap 객체를 받음.
        mapFragment.getMapAsync(this);

        //위치를 반환하는 구현체인 FusedLocationSource 생성
        locationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        SharedPreferences sharedPreferences2 = getSharedPreferences("loginID", Context.MODE_PRIVATE);
        Boolean isAutoLogin = sharedPreferences2.getBoolean("autoLogin", false);

        handler = new Handler(Looper.getMainLooper());

        //childList Create(member의 child 리스트 생성)
        String memberID = LoginPageFragment.saveID;
        GetChildIDRequest memberIDDTO = new GetChildIDRequest(memberID);

        Call<ResponseBody> childCall = userRetrofitInterface.getChildID(memberIDDTO);
        childCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 응답 본문을 문자열로 변환
                        String responseBodyString = response.body().string();
                        JSONObject json = new JSONObject(responseBodyString);

                        // 각 키-값 쌍을 처리
                        for (Iterator<String> keys = json.keys(); keys.hasNext(); ) {
                            String key = keys.next();
                            String value = json.getString(key);
                            if (key.equals("status")) {
                            } else {
                                childList.add(value);
                                loadImageToServer(value);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("getChildID", "Response body is null or request failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e("getChildID", "Request failed", t);
            }
        });


        for (int i = 0; i < childList.size(); i++) {
            dynamicVariables.put(i, childList.get(i));
            markerName = dynamicVariables.get(i);
        }

        //childList2 Create(helper의 child 리스트 생성)
        String helperID = LoginPageFragment.saveID;
        GetChildIDRequest helperIDDTO = new GetChildIDRequest(helperID);

        Call<ResponseBody> childCall2 = userRetrofitInterface.getHelperID(helperIDDTO);
        childCall2.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // 응답 본문을 문자열로 변환
                        String responseBodyString = response.body().string();
                        JSONObject json = new JSONObject(responseBodyString);

                        // "Helping" 키를 가져와서 그 안의 키-값 쌍을 처리
                        if (json.has("Helping")) {
                            JSONObject helpingJson = json.getJSONObject("Helping");
                            for (Iterator<String> keys = helpingJson.keys(); keys.hasNext(); ) {
                                String key = keys.next();
                                String value = helpingJson.getString(key);
                                childList2.add(value);
                                loadImageToServer(value);
                            }
                        } else {
                            Log.e("getChildID", "'Helping' key is missing in the response");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("getChildID", "Response body is null or request failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("getChildID", "Request failed", t);
            }
        });


        for (int i = 0; i < childList2.size(); i++) {
            dynamicVariables2.put(i, childList2.get(i));
            markerName = dynamicVariables2.get(i);
        }


        // 마커를 업데이트하는 Runnable 정의
        updateMarkerRunnable = new Runnable() {
            @Override
            public void run() {
                getChildLocation(childList, dynamicVariables, childMarkers); // member의 child 위치 가져오기
                handler.postDelayed(this, 3000); // 3초마다 실행
            }
        };

        // 주기적인 마커 업데이트 시작
        handler.post(updateMarkerRunnable);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationService();
        } else
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    if (isAutoLogin) {
                        finishAffinity(); // 현재 액티비티와 관련된 모든 액티비티를 종료
                        return;
                    } else {
                        GroupRepository groupRepository = GroupRepository.getInstance(MainActivity.this);
                        groupRepository.removeAllGroups();
                        OtherGroupRepository otherGroupRepository = OtherGroupRepository.getInstance(MainActivity.this);
                        otherGroupRepository.removeAllOtherGroups();
                        Intent intent = new Intent(MainActivity.this, StartScreenActivity.class);
                        startActivity(intent);
                    }
                }

                doubleBackToExitPressedOnce = true;
                Toast.makeText(MainActivity.this, "앱을 종료하시려면 한번 더 눌러주세요", Toast.LENGTH_SHORT).show();

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000); // 2초 안에 두 번 눌러야 종료
            }
        });

        // BatteryManager 객체를 통해 배터리 잔량 가져오기
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (batteryManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } else {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                if (batteryStatus != null) {
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    battery = level / (double) scale * 100.0;
                }
            }
        }

        // 주기적인 배터리 업데이트
        Runnable updateBatteryRunnable = new Runnable() {
            @Override
            public void run() {
                if (batteryManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    } else {
                        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        Intent batteryStatus = registerReceiver(null, ifilter);
                        if (batteryStatus != null) {
                            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                            battery = level / (double) scale * 100.0;
                        }
                    }
                }
                handler.postDelayed(this, 10000); // 10초마다 실행
            }
        };
        handler.post(updateBatteryRunnable);

    }

    private void startLocationService() {
        /*Log.e("POST", "Latitude: " + latitude + ", Longitude: " + longitude);*/

        serviceIntent = new Intent(this, LocationService2.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.map_types,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = findViewById(R.id.map_type);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CharSequence mapType = adapter.getItem(position);

                if (mapType.equals("일반지도")) {
                    naverMap.setMapType(Basic);
                } else if (mapType.equals("위성지도")) {
                    naverMap.setMapType(Hybrid);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(locationSource);
        mNaverMap.setIndoorEnabled(true);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // CompassView를 NaverMap에 바인딩
        CompassView compassView = findViewById(R.id.compass);
        uiSettings.setCompassEnabled(false); // 기본 나침반 비활성화
        compassView.setMap(naverMap); // 커스텀 나침반 설정

        // 권한 확인, 결과는 onRequestPermissionResult 콜백 메서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);

        mNaverMap.addOnLocationChangeListener(location -> {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            LocationService2.transmitCoordinate(latitude, longitude, battery);
        });
        sectorInquire();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // request code와 권한 획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    private void getChildLocation(ArrayList<String> childArrayList, HashMap<Integer, String> dynamicVariablesMap, HashMap<String, Marker> childMarkersMap) {
        for (int i = 0; i < childArrayList.size(); i++) {
            String getChildId = childArrayList.get(i);
            String type = "Child";

            LocationRequest locationRequest = new LocationRequest(type, getChildId, battery);

            Call<LocationResponse> call = userRetrofitInterface.getLocation(locationRequest);
            int finalI = i;
            call.clone().enqueue(new Callback<LocationResponse>() {
                @Override
                public void onResponse(Call<LocationResponse> call, Response<LocationResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LocationResponse result = response.body();

                        double latitude = result.getLatitude();
                        double longitude = result.getLongitude();
                        int battery = (int) (result.getBattery());

                        dynamicVariablesMap.put(finalI, childArrayList.get(finalI));
                        markerName = dynamicVariablesMap.get(finalI);

                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View markerView = inflater.inflate(R.layout.custom_marker, null);
                        ImageView imageView = markerView.findViewById(R.id.marker_image);

                        if (childImages.containsKey(getChildId)) {
                            Bitmap childImage = childImages.get(getChildId);
                            imageView.setImageBitmap(childImage);
                        }

                        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
                        markerView.buildDrawingCache();
                        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        markerView.draw(canvas);

                        // Bitmap을 마커 이미지로 설정
                        OverlayImage markerIcon = OverlayImage.fromBitmap(bitmap);

                        if (childMarkersMap.get(markerName) == null) { // 해당 childMarker가 한번도 생성되지 않은 경우
                            Marker marker = new Marker();
                            childMarkersMap.put(markerName, marker);

                            marker.setCaptionText(getChildId);
                            marker.setCaptionAligns(Align.Bottom);
                            marker.setCaptionOffset(5);
                            marker.setWidth(130);
                            marker.setHeight(195);
                            marker.setIcon(markerIcon);
                            marker.setHideCollidedSymbols(true);
                            marker.setCaptionTextSize(16);
                            marker.setSubCaptionText(battery + "%");
                            marker.setPosition(new LatLng(latitude, longitude));
                            marker.setMap(mNaverMap);
                        } else { // 해당 childMarker가 이미 생성되어 있는 경우
                            childMarkersMap.get(markerName).setMap(null);

                            Marker marker = new Marker();
                            childMarkersMap.replace(markerName, marker);

                            marker.setCaptionText(getChildId);
                            marker.setCaptionAligns(Align.Bottom);
                            marker.setCaptionOffset(5);
                            marker.setWidth(130);
                            marker.setHeight(195);
                            marker.setIcon(markerIcon);
                            marker.setHideCollidedSymbols(true);
                            marker.setCaptionTextSize(16);
                            marker.setSubCaptionText(battery + "%");
                            marker.setPosition(new LatLng(latitude, longitude));
                            marker.setMap(mNaverMap);
                        }
                    }
                }

                @Override
                public void onFailure(Call<LocationResponse> call, Throwable t) {
                    Log.e("POST", "통신 실패: " + t.getMessage());
                }
            });
        }
    }

    private void sectorInquire() {
        final Gson gson = new Gson();

        removeAllPolygons();

        retrofitClient = RetrofitClient.getInstance();
        userRetrofitInterface = RetrofitClient.getInstance().getUserRetrofitInterface();
        for (String childId : childList) {
            // 요청 JSON 로그 출력
            SectorInquireRequest sectorInquireDTO = new SectorInquireRequest(childId);
            String requestJson = gson.toJson(sectorInquireDTO);

            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), requestJson);
            Call<ResponseBody> call = userRetrofitInterface.getSectorLocation(body);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LinkedHashMap<String, SectorDetails> sectors = new LinkedHashMap<>();

                        try {
                            JSONObject json = new JSONObject(response.body().string());

                            for (Iterator<String> keys = json.keys(); keys.hasNext(); ) {
                                String key = keys.next();

                                SectorDetails sector = gson.fromJson(json.getJSONObject(key).toString(), SectorDetails.class);
                                sectors.put(key, sector);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        if (sectors.isEmpty()) {
                            Log.e("SectorInquire", "No sectors found or sectorResponse is null");
                            return;
                        }

                        for (Map.Entry<String, SectorDetails> entry : sectors.entrySet()) {
                            String coordinateId = entry.getKey(); // 키 값 저장
                            SectorDetails details = entry.getValue();
                            boolean isLiving = Boolean.parseBoolean(details.getIsLiving());

                            // 좌표를 가져와서 LatLng 리스트를 생성합니다.
                            List<LatLng> polygonCoords = new ArrayList<>();
                            polygonCoords.add(new LatLng(details.getYofPointA(), details.getXofPointA()));
                            polygonCoords.add(new LatLng(details.getYofPointB(), details.getXofPointB()));
                            polygonCoords.add(new LatLng(details.getYofPointC(), details.getXofPointC()));
                            polygonCoords.add(new LatLng(details.getYofPointD(), details.getXofPointD()));

                            // 폴리곤 오버레이 생성
                            PolygonOverlay polygonOverlay = new PolygonOverlay();
                            polygonOverlay.setCoords(polygonCoords);

                            // 색상 설정
                            if (isLiving) {
                                polygonOverlay.setColor(Color.argb(75, 0, 100, 0)); // 초록색
                            } else {
                                polygonOverlay.setColor(Color.argb(75, 100, 0, 0)); // 빨간색
                            }

                            // 폴리곤을 지도에 추가
                            polygonOverlay.setMap(mNaverMap);
                            polygonOverlays.add(polygonOverlay);
                        }
                    } else {
                        // 응답 본문이 null일 때 처리
                        Log.e("SectorInquire", "Response body is null");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    // 요청 실패 처리
                    Log.e("SectorInquire", "Request failed", t);
                }
            });
        }
    }

    private void emergency() {
        LinearLayout mapModeNav = findViewById(R.id.mapModeNav);
        mapModeNav.setVisibility(View.GONE);
        // SettingFragment를 실행시 나침반 안 보이게 설정
        CompassView compassView = findViewById(R.id.compass);
        compassView.setVisibility(View.GONE);

        emergencyBtn.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.containers, new MyEmergencyFragment()).commit();
    }

    private void loadImageToServer(String childID) {
        LoadImageRequest loadImageRequest = new LoadImageRequest("Child", childID);
        Call<ResponseBody> call = userRetrofitInterface.getloadFile(loadImageRequest);
        String fileUrl = "http://223.130.152.254:8080/imagePath/";
        call.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String reponseBodyString = response.body().string();
                        JSONObject json = new JSONObject(reponseBodyString);
                        for (Iterator<String> keys = json.keys(); keys.hasNext(); ) {
                            String key = keys.next();
                            String value = json.getString(key);
                            if (key.equals("filePath")) {
                                String loadfilepath = value;
                                Log.e("POST", loadfilepath);
                                Glide.with(MainActivity.this)
                                        .asBitmap()
                                        .load(fileUrl + loadfilepath)
                                        .into(new SimpleTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                childImages.put(childID, resource);
                                            }
                                        });
                            } else {
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

    private void removeAllPolygons() {
        for (PolygonOverlay overlay : polygonOverlays) {
            overlay.setMap(null);
        }
        polygonOverlays.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateMarkerRunnable); // 액티비티가 종료될 때 주기적인 업데이트를 중지
    }
}