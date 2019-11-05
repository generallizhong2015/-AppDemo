package com.ljkj.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * 定位相关服务器
 *
 * @author zhangbiao
 */
public class LocationUtils {
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static OnLocationChangeListener mListener;
    private static MyLocationListener myLocationListener;
    private static LocationManager mLocationManager;

    private LocationUtils() {
        throw new UnsupportedOperationException("Do not need instantiate!");
    }

    /**
     * 判断Gps是否可用
     */
    public static boolean isGpsEnabled() {
        LocationManager lm = (LocationManager) ActivityUtils.getApp().getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 判断定位是否可用
     */
    public static boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) ActivityUtils.getApp().getSystemService(Context.LOCATION_SERVICE);
        //noinspection ConstantConditions
        return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 打开Gps设置界面
     */
    public static void openGpsSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        ActivityUtils.getApp().startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     * 注册
     * <uses-permission android:name="android.permission.INTERNET" />
     * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
     *
     * @param minTime     位置信息更新周期（单位：毫秒）
     * @param minDistance 位置变化最小距离：当位置距离变化超过此值时，将更新位置信息（单位：米）
     * @param listener    位置刷新的回调接口
     * @return true: 初始化成功 false: 初始化失败
     */
    @SuppressLint("MissingPermission")
    public static boolean register(long minTime, long minDistance, OnLocationChangeListener listener) {
        if (listener == null) return false;
        mLocationManager = (LocationManager) ActivityUtils.getApp().getSystemService(Context.LOCATION_SERVICE);
        //noinspection ConstantConditions
        if (!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LogUtils.d("LocationUtils", "无法定位，请打开定位服务");
            return false;
        }
        mListener = listener;
        String provider = mLocationManager.getBestProvider(getCriteria(), true);
        @SuppressLint("MissingPermission") Location location = mLocationManager.getLastKnownLocation(provider);
        if (location != null) listener.getLastKnownLocation(location);
        if (myLocationListener == null) myLocationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(provider, minTime, minDistance, myLocationListener);
        return true;
    }

    /**
     * 注销
     */
    public static void unregister() {
        if (mLocationManager != null) {
            if (myLocationListener != null) {
                mLocationManager.removeUpdates(myLocationListener);
                myLocationListener = null;
            }
            mLocationManager = null;
        }
        if (mListener != null) {
            mListener = null;
        }
    }

    /**
     * 设置定位参数
     *
     * @return {@link Criteria}
     */
    private static Criteria getCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(false);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

    /**
     * 根据经纬度获取地理位置
     *
     * @param latitude  纬度
     * @param longitude 经度
     */
    public static Address getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(ActivityUtils.getApp(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) return addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据经纬度获取所在国家
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 所在国家
     */
    public static String getCountryName(double latitude, double longitude) {
        Address address = getAddress(latitude, longitude);
        return address == null ? "unknown" : address.getCountryName();
    }

    /**
     * 根据经纬度获取所在地
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 所在地
     */
    public static String getLocality(double latitude, double longitude) {
        Address address = getAddress(latitude, longitude);
        return address == null ? "unknown" : address.getLocality();
    }

    /**
     * 根据经纬度获取所在街道
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 所在街道
     */
    public static String getStreet(double latitude, double longitude) {
        Address address = getAddress(latitude, longitude);
        return address == null ? "unknown" : address.getAddressLine(0);
    }

    /**
     * 是否更好的位置
     *
     * @param newLocation         新的位置
     * @param currentBestLocation 之前的位置
     * @return
     */
    public static boolean isBetterLocation(Location newLocation, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), currentBestLocation.getProvider());
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * 是否相同的提供者
     *
     * @param provider0 提供者1
     * @param provider1 提供者2
     */
    public static boolean isSameProvider(String provider0, String provider1) {
        if (provider0 == null) {
            return provider1 == null;
        }
        return provider0.equals(provider1);
    }

    private static class MyLocationListener
            implements LocationListener {
        /**
         * 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
         *
         * @param location 坐标
         */
        @Override
        public void onLocationChanged(Location location) {
            if (mListener != null) {
                mListener.onLocationChanged(location);
            }
        }

        /**
         * provider的在可用、暂时不可用和无服务三个状态直接切换时触发此函数
         *
         * @param provider 提供者
         * @param status   状态
         * @param extras   provider可选包
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (mListener != null) {
                mListener.onStatusChanged(provider, status, extras);
            }
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d("LocationUtils", "当前GPS状态为可见状态");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("LocationUtils", "当前GPS状态为服务区外状态");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("LocationUtils", "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * provider被enable时触发此函数，比如GPS被打开
         */
        @Override
        public void onProviderEnabled(String provider) {
        }

        /**
         * provider被disable时触发此函数，比如GPS被关闭
         */
        @Override
        public void onProviderDisabled(String provider) {
        }
    }

    public interface OnLocationChangeListener {

        /**
         * 获取最后一次保留的坐标
         *
         * @param location 坐标
         */
        void getLastKnownLocation(Location location);

        /**
         * 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
         *
         * @param location 坐标
         */
        void onLocationChanged(Location location);

        /**
         * provider的在可用、暂时不可用和无服务三个状态直接切换时触发此函数
         *
         * @param provider 提供者
         * @param status   状态
         * @param extras   provider可选包
         */
        void onStatusChanged(String provider, int status, Bundle extras);//位置状态发生改变
    }
}