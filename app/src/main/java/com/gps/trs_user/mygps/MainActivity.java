package com.gps.trs_user.mygps;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private static final String TAG = "@@@";
    private GoogleApiClient mGoogleApiClient = null; //구글 플레이 서비스 클래스
    private LocationRequest mLocationRequest; //현재 위치를 실시간 파악하는 클래스
    private static final int REQUEST_CODE_LOCATION = 2000; //임의 정수
    private static final int REQUEST_CODE_GPS = 2001; //임의 정수
    private GoogleMap googleMap; //GoogleMapAPI
    private ArrayList<LatLng> latLngArrayList = new ArrayList<>();
    LocationManager locationManager; //위치정보를 가져오는 클래스
    MapFragment mapFragment;
    boolean setGPS = false;
    LatLng SEOUL = new LatLng(37.56, 126.97); //서울 경도, 위도

    //하나의 객체에 여러 개의 객체가 동시에 접근 처리하는 것을 막기 위해 동기화.
    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this) //Google API 클라이언트의 인스턴스를 생성
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    //GPS 활성화를 위한 다이얼로그 보여주기
    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this); //알림창 띄우기
        alertDialogBuilder.setMessage("GPS가 비활성화 되어있습니다. 활성화 하시겠습니까?")
                .setCancelable(false)
                // OK 를 누르게 되면 설정창으로 이동합니다.
                .setPositiveButton("설정", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(callGPSSettingIntent, REQUEST_CODE_GPS); //설정값 저장
                    }
                });
        alertDialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        //Dialog 띄우기
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    //GPS 활성화를 위한 다이얼로그의 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_GPS:
                //사용자가 GPS 활성화 시켰는지 여부 파악
                if(locationManager == null)
                    //locationManager의 값을 구하기 위해 Activity의 상위 클래스인 getSystemService 메소드 호출
                    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

                // LocationManaer.NETWORK_PROVIDER : 기지국들로부터 현재 위치 확인
                // LocationManaer.GPS_PROVIDER : GPS들로부터 현재 위치 확인
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    //isProviderEnabled() 메소드는 리턴값으로, GPS가 켜져있으면 true, 아니면 false를 반환한다.
                    setGPS = true;
                    //mapFragment에서 콜백을 설정
                    mapFragment.getMapAsync(MainActivity.this);
                }
                break;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    public boolean checkLocationPermission(){
        Log.d(TAG, "checkLocationPermission");

        //VERSION.SDK_INT : 단말기의 SDK 레벨을 가져온다.
        //Build.VERSION_CODES.M : 안드로이드 6.0 마시멜로우
        //현재 단말기의 버젼이 마쉬멜로우보다 높거나 같으면
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //checkSelfPermission : 특정 권한이 이미 획득됐는지 확인하는 메소드
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

                //퍼미션 요청을 위해 UI를 보여줘야 하는지 검사
                //만약 사용자가 권한 요청 팝업에서 수락을 누르지 않으면
                // ActivityCompat의 shouldShowRequestPermissionRationale 메서드 반환 값이 true가 됩니다.
                //사용자에게 제시되는 팝업 모양도 조금 변하는데 다시는 이 팝업을 띄우지 않도록
                // 설정하는 체크박스가 추가됩니다. 만약 사용자가 이 체크박스를 선택하고
                // 거절을 누른다면, 앱 상에서 다시는 권한 설정 팝업을 보여줄 수 없고,
                // 사용자가 직접 설정에서 앱을 찾아 해당 권한을 부여하기 전까지 영영 그 기능을 사용할 수 없습니다.
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                    //권한이 획득되었다면 특정 권한을 사용하는 기능을 바로 수행할 수 있지만,
                    //획득되지 않았다면 ActivityCompat의 requestPermissions 메서드로 권한을 요청해야 합니다.
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                }else{
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
                    return false;
                }
            }else{
                Log.d(TAG, "checkLocationPermission" + "이미 퍼미션 획득한 경우");

                //리턴 값으로, GPS가 켜져있으면 true, 아니면 false를 반환한다.
                // LocationManaer.GPS_PROVIDER : GPS들로부터 현재 위치 확인
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                    Log.d(TAG, "checkLocationPermission Version >= M");
                    //GPS 활성화를 위한 다이얼로그 보여주는 메소드 실행
                    showGPSDisabledAlertToUser();
                }
                if(mGoogleApiClient == null){
                    Log.d(TAG, "checkLocationPermission" + "mGoogleApiClient == NULL");
                    //상단 참조
                    buildGoogleApiClient();
                }else Log.d(TAG, "checkLoctionPermission" + "mGoogleApiClient != NULL");

                if(mGoogleApiClient.isConnected()) Log.d(TAG, "checkLocationPermission" + "mGoogleApiClient 연결되있음.");
                else Log.d(TAG, "checkLocationPermission" + "mGoogleApiClient 끊어져있음.");

                mGoogleApiClient.reconnect();
                //setMyLocationEnabled(true)
                // My Location 버튼이 지도 오른쪽 상단 모서리에 나타납니다.
                // 사용자가 버튼을 클릭했을 때 현재 위치를 알 경우,
                // 카메라가 기기의 현재 위치를 지도의 중앙에 표시합니다.
                // 기기가 정지해 있을 때는 위치가 지도 위에 작은 파란 점으로 나타나고,
                // 이동 중일 때는 V자 기호로 나타납니다.
                googleMap.setMyLocationEnabled(true);
            }
        }else{
            //리턴 값으로, GPS가 켜져있으면 true, 아니면 false를 반환한다.
            // LocationManaer.GPS_PROVIDER : GPS들로부터 현재 위치 확인
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                Log.d(TAG, "checkLocationPermission Version < M");
                //상단참조
                showGPSDisabledAlertToUser();
            }
            if(mGoogleApiClient == null){
                //상단참조
                buildGoogleApiClient();
            }
            googleMap.setMyLocationEnabled(true);
        }
        return true;
    }

    //GoogleMap 초기 세팅
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        //카메라 위치를 변경하려면 CameraUpdate를 사용하여 카메라를 이동할 위치를 지정해야 하며,
        // Maps API를 사용하면 CameraUpdateFactory를 사용하여 다양한 유형의 CameraUpdate를 생성할 수 있다

        //맵 이동 메소드
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));

        //맵 이동시, 움직이는 애니메이션 효과 메소드
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback(){
            @Override
            public void onMapLoaded() {
                Log.d(TAG, "onMapLoaded");

                //상단 참조
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    checkLocationPermission();
                }else{
                    //리턴 값으로, GPS가 켜져있으면 true, 아니면 false를 반환한다.
                    // LocationManaer.GPS_PROVIDER : GPS들로부터 현재 위치 확인
                    if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS){
                        Log.d(TAG, "onMapLoaded");
                        showGPSDisabledAlertToUser();
                    }
                    //상단 참조
                    if(mGoogleApiClient == null) buildGoogleApiClient();
                    googleMap.setMyLocationEnabled(true);
                }
            }
        });

        //구글 플레이 서비스 초기화
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();

                googleMap.setMyLocationEnabled(true);
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }
        }else{
            buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) setGPS = true;
        //현재 위치를 실시간 파악하는 클래스
        mLocationRequest = new LocationRequest();

        //Priority는 4가지의 설정값이 있다.
        //PRIORITY_HIGH_ACCURACY : 배터리소모를 고려하지 않으며 정확도를 최우선으로 고려
        //PRIORITY_LOW_POWER : 저전력을 고려하며 정확도가 떨어짐
        //PRIORITY_NO_POWER : 추가적인 배터리 소모없이 위치정보 획득
        //PRIORITY_BALANCED_POWER_ACCURACY : 전력과 정확도의 밸런스를 고려. 정확도 다소 높음
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //setInteval : 위치가 update되는 주기
        //setFastestInterval : 위치 획득 후 update되는 주기
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(3000);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "onConnected" + "getLocationAvailability mGoogleApiClient.isConnected() = " + mGoogleApiClient.isConnected());
            if(!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();

            if ( setGPS && mGoogleApiClient.isConnected())
            {
                Log.d( TAG, "onConnected " + "requestLocationUpdates" );

                //접근승인 요청이 성공하면 Fused Location Provider API에 대한 LocationServices Class를 사용할 수 있다
                //위치찾는 메소드
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if(location == null) return;

                //현재 위치에 마커 생성
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude()); //경도, 위도 입력
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("현재 위치");
                googleMap.addMarker(markerOptions);

                //지도상에서 보여주는 영역 이동
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        //구글 플레이 서비스 연결이 해제되었을 때, 재연결 시도
        Log.d(TAG,"Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mGoogleApiClient != null) mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "OnDestroy");

        if(mGoogleApiClient != null){
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if(mGoogleApiClient.isConnected()){
                //위치찾기 중지 메소드
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        String errorMsg = "";

        googleMap.clear();

        //현재 위치에 마커 생성
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("현재 위치");
        googleMap.addMarker(markerOptions);

        //이동경로를 표시하는 Polyline 추가
        //Polyline을 생성하려면 먼저 PolylineOptions 객체를 생성
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        latLngArrayList.add(latLng);
        polylineOptions.addAll(latLngArrayList);

        //지도에 폴리라인을 추가할 수 있습니다.
        //addPolyline 메서드는 Polyline 객체를 반환하며, 나중에 이 객체를 사용하여 Polyline을 변경할 수 있습니다.
        googleMap.addPolyline(polylineOptions);

        //지도상에서 보여주는 영역 이동
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        //레벨 선택기 컨트롤을 활성화/비활성화하는 메소드
        //ex) 1 = 나라, 2 = 도시....
        googleMap.getUiSettings().setCompassEnabled(true);

        //지오코더 GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try{
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        }catch (IOException ioException){
            errorMsg = "지오코더 서비스 사용불가";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }catch (IllegalArgumentException illegalArgumentException){
            errorMsg = "잘못된 GPS 좌표";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }

        if(addresses == null || addresses.size() == 0){
            if(errorMsg.isEmpty()){
                errorMsg = "주소 미발견";
                Log.e(TAG, errorMsg);
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }else{
            Address address = addresses.get(0);
            Toast.makeText(this, address.getAddressLine(0).toString(), Toast.LENGTH_LONG).show();
        }
    }
}
