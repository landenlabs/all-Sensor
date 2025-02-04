/*
 * Unpublished Work © 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_sensor.utils;

import static com.landenlabs.all_sensor.utils.FragUtils.getServiceSafe;
import static com.landenlabs.all_sensor.utils.StrUtils.asString;
import static com.landenlabs.all_sensor.utils.StrUtils.joinStrings;
import static com.landenlabs.all_sensor.utils.Util.niceString;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.ui.view.BaseFragment;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class NetInfo {

    // Utility functions

    static SysInfoPhoneStateListener phoneStateListener = null;
    private static List<ScanResult> listWifi;
    private static final IntentFilter INTENT_FILTER_SCAN_AVAILABLE = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

    // ---------------------------------------------------------------------------------------------
    // Network stuff
    private static NetBroadcastReceiver mNetBroadcastReceiver;

    public static String getActiveType(Context context) {
        ConnectivityManager cm = getServiceSafe(context, Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        String netType = "Unknown";
        if (netInfo != null) {
            netType = joinStrings("\n", netInfo.getTypeName(), netInfo.getSubtypeName(), netInfo.getExtraInfo());  // Wifi or mobile
        }
        return netType;
    }

    // Put values in List ifValue true.
    private static <M extends Map<E, F>, E, F> void putIf(M listObj, E v1, F v2, boolean ifValue) {
        if (ifValue) {
            listObj.put(v1, v2);
        }
    }

    public static boolean haveNetwork(@NonNull Context context) {
        ConnectivityManager connMgr = getServiceSafe(context, Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return (netInfo != null && connMgr.getActiveNetwork() != null);
    }

    public static int getWifiLevelPercent(WifiManager wifiMgr, int rssi) {
        int numberOfLevels = 10;
        if (Build.VERSION.SDK_INT >= 30 /* Build.VERSION_CODES.R */) {
            numberOfLevels = wifiMgr.getMaxSignalLevel();
            return wifiMgr.calculateSignalLevel(rssi) * 100 / numberOfLevels;
        } else {
            return WifiManager.calculateSignalLevel(rssi, numberOfLevels) * 100 / numberOfLevels;
        }
    }

    public static SpannableString getStatus(
            @NonNull Context context, NetStatus netStatus, String trailer, @ColorInt int[] colors) {

        String netType = context.getString(R.string.no_network);
        String sigLevel = "";
        String sigFreq = "";
        String ssid = "";
        int sigLevelInt = 0;

        if (netStatus != null && netStatus.netInfo != null) {
            // final Network activeNet = connMgr.getActiveNetwork();
            // final NetworkCapabilities netCap = connMgr.getNetworkCapabilities(activeNet);

            netType = joinStrings(" ", netStatus.netInfo.getTypeName(),
                    netStatus.netInfo.getSubtypeName(), netStatus.netInfo.getExtraInfo());  // Wifi or mobile

            if (netStatus.netCapabilities != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    sigLevelInt = netStatus.netCapabilities.getSignalStrength();
                    sigLevel = String.format(Locale.US, " %d", sigLevelInt);
                }
            }
            if (netType.toLowerCase().contains("wifi")) {
                // Signal strength, SSID, ipv4/6
                final WifiManager wifiMgr = getServiceSafe(context, Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                if (wifiInfo != null) {
                    int numberOfLevels = 10;
                    sigLevelInt = getWifiLevelPercent(wifiMgr, wifiInfo.getRssi());
                    sigLevel = String.format(Locale.US, " %d%%", sigLevelInt);
                    sigFreq = String.format(Locale.US, " %.2f GHz ", wifiInfo.getFrequency() / 1000.0);

                    Map<String, Object> wifiMap = (Map<String, Object>) netInfoList.get(WIFI_TAG);
                    ssid = (wifiMap != null) ? asString(wifiMap.get(WIFI_SSID_TAG)) : "";
                }
            } else {
                // Signal strength, ipv4/6
            }
        }

        SpannableString span = new SpannableString(netType + sigLevel + sigFreq + "\n" + ssid + trailer);
        int sigPerColor = colors[0]; // 0xff309020; // Color.GREEN;
        if (sigLevelInt < 80)
            sigPerColor = colors[1]; // 0xffd08030; // Color.Orange;
        if (sigLevelInt < 50)
            sigPerColor = colors[2]; //  0xffffb0b0;  // Pink

        int startPer = netType.length();
        int endPer = startPer + sigLevel.length();
        span.setSpan(new ForegroundColorSpan(sigPerColor), 0, span.length(), 0);

        return span;
    }

    public static Map<String, Object> loadNetInfo(Context context) {
        Map<String, Object> netInfo = new LinkedHashMap<>();

        // checkPermissions( Manifest.permission.READ_PHONE_STATE);

        ConnectivityManager connMgr = getServiceSafe(context, Context.CONNECTIVITY_SERVICE);
        final Network activeNet = connMgr.getActiveNetwork();
        if (activeNet != null) {
            final Network[] allNet = connMgr.getAllNetworks();
            if (allNet.length > 0) {
                // TODO - add nested Map to handle case of multiple active networks.
                for (Network net : allNet) {
                    putIf(netInfo, "Connected", net.describeContents(), activeNet.equals(net));
                    final LinkProperties linkProp = connMgr.getLinkProperties(net);
                    if (linkProp != null) {
                        String netInterfaceName = linkProp.getInterfaceName();
                        putIf(netInfo, "Domain", linkProp.getDomains(), StrUtils.hasText(linkProp.getDomains()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            netInfo.put("MTU", linkProp.getMtu());

                        }
                        ProxyInfo proxyInfo = linkProp.getHttpProxy();
                        if (proxyInfo != null) {
                            netInfo.put("Proxy", proxyInfo.toString());
                        }

                        addNetCapToList(netInfo, netInterfaceName, connMgr.getNetworkCapabilities(activeNet));
                        int dnsNum = 0;
                        for (InetAddress dns : linkProp.getDnsServers()) {
                            if (dns.getHostAddress() != null) {
                                dnsNum++;
                                netInfo.put(netInterfaceName + " DNS ", formatInet(dns));
                            }
                        }
                        for (RouteInfo route : linkProp.getRoutes()) {
                            netInfo.put(route.getInterface() + " Gateway", formatInet(route.getGateway()));
                        }
                    }
                }
            }

            ProxyInfo proxyInfo = connMgr.getDefaultProxy();
            if (proxyInfo != null) {
                netInfo.put("Proxy ", proxyInfo.getHost() + ":" + proxyInfo.getPort());
            }
        }

        return netInfo;
    }

    private static void addNetCapToList(Map<String, Object> netInfo, String tag, NetworkCapabilities netCap) {
        if (netCap != null) {
            netInfo.put("Down limit", String.format(Locale.US, "%,d Mbps", netCap.getLinkDownstreamBandwidthKbps() / 1000));
            netInfo.put("Up limit", String.format(Locale.US, "%,d Mbps", netCap.getLinkUpstreamBandwidthKbps() / 1000));
            if (Build.VERSION.SDK_INT >= 29) {
                netInfo.put("Signal", String.valueOf(netCap.getSignalStrength()));
                // TransportInfo transportInfo = netCap.getTransportInfo();
            }
        }
    }

    @NonNull
    public static String formatInet(@Nullable InetAddress inetAddress) {
        String result = "";
        if (inetAddress != null) {
            if (inetAddress.getHostAddress() != null) {
                String ipType = (inetAddress instanceof Inet4Address) ? "IPv4 "
                        : (inetAddress instanceof Inet6Address ? "IPv6 "
                        : "");
                // return ipType + inetAddress.getHostName(); must not be on main_menu thread
                return ipType + inetAddress.getHostAddress();
            }
        }
        return result;
    }

    public static String formatIp(int ipAddrss) {
        byte[] myIPAddress = BigInteger.valueOf(ipAddrss).toByteArray();
        // you must reverse the byte array before conversion. Use Apache's commons library
        invertUsingFor(myIPAddress);
        try {
            InetAddress myInetIP = InetAddress.getByAddress(myIPAddress);
            return myInetIP.getHostAddress();
        } catch (Exception ex) {
            return "<unknownIP>";
        }
    }

    private static void invertUsingFor(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }

    public static Map<String, Object> netInfoList = new LinkedHashMap<>();
    public static final String WIFI_TAG = "WiFi";
    public static final String WIFI_SSID_TAG = "SSID";
    private static boolean wifiScanAllowed = true;

    @SuppressLint("MissingPermission")
    public static Map<String, Object> loadWifi(@NonNull Context context, @Nullable BaseFragment baseFrag) {
        final WifiManager wifiMgr = getServiceSafe(context, Context.WIFI_SERVICE);

        if (mNetBroadcastReceiver == null) {
            mNetBroadcastReceiver = new NetBroadcastReceiver(wifiMgr);
            // getActivitySafe().unregisterReceiver(mNetBroadcastReceiver);

            /*
            Error
            ReceiverCallNotAllowedException: BroadcastReceiver components are not allowed to register to receive intents

            context.registerReceiver(mNetBroadcastReceiver, INTENT_FILTER_SCAN_AVAILABLE);
             */
        }

        if (wifiScanAllowed) {
            if (wifiMgr.getScanResults() == null || wifiMgr.getScanResults().isEmpty()) {
                // checkPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (baseFrag != null) {
                    baseFrag.checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
                }

                wifiScanAllowed = wifiMgr.startScan();    // Throws warning in log "W WifiService: Permission violation - startScan not allowed"
            }
        }

        final Map<String, Object> mainList = new LinkedHashMap<>();

        try {
            DhcpInfo dhcpInfo = wifiMgr.getDhcpInfo();

            if (dhcpInfo != null) {
                Map<String, Object> dhcpList = new LinkedHashMap<>();
                mainList.put("DHCP", dhcpList);

                // java.net.InetAddress.
                dhcpList.put("DNS1", formatIp(dhcpInfo.dns1));
                dhcpList.put("DNS2", formatIp(dhcpInfo.dns2));
                dhcpList.put("Default Gateway", formatIp(dhcpInfo.gateway));
                dhcpList.put("IP Address", formatIp(dhcpInfo.ipAddress));
                dhcpList.put("Subnet Mask", formatIp(dhcpInfo.netmask));
                dhcpList.put("Server IP", formatIp(dhcpInfo.serverAddress));
                dhcpList.put("Lease Time(sec)", String.valueOf(dhcpInfo.leaseDuration));
            }

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            if (wifiInfo != null) {
                Map<String, Object> wifiList = new LinkedHashMap<>();
                mainList.put(WIFI_TAG, wifiList);

                wifiList.put(WIFI_SSID_TAG, wifiInfo.getSSID());
                wifiList.put("LinkSpeed Mbps", String.valueOf(wifiInfo.getLinkSpeed()));
                // wifiKey = String.format("Wifi %s %,dMB", wifiInfo.getSSID(), wifiInfo.getLinkSpeed());


                int level = getWifiLevelPercent(wifiMgr, wifiInfo.getRssi() );
                wifiList.put("Signal%", String.valueOf(level));
                // dhcpList.put("MAC", getMacAddr());
                if ("MHz".equals(WifiInfo.FREQUENCY_UNITS)) {
                    wifiList.put("Frequency", String.format(Locale.US, "Frequency %.2f GHz", wifiInfo.getFrequency() / 1000.0));
                } else {
                    wifiList.put("Frequency", wifiInfo.getFrequency() + WifiInfo.FREQUENCY_UNITS);
                }

            }
        } catch (Exception ex) {
            ALog.e.tagMsg("loadWifi", ex);
        }

        if (wifiScanAllowed) {
            try {

                // @SuppressLint("MissingPermission")
                // Logs warning "W WifiService: Permission violation - getConfiguredNetworks not allowed"
                List<WifiConfiguration> listWifiCfg = wifiMgr.getConfiguredNetworks();

                for (WifiConfiguration wifiCfg : listWifiCfg) {
                    String wifiTitle = String.format("WiFiCfg#%s %s", wifiCfg.networkId, wifiCfg.SSID);
                    Map<String, Object> wifiList = new LinkedHashMap<>();
                    mainList.put(wifiTitle, wifiList);

                    wifiList.put("Name", wifiCfg.providerFriendlyName);
                    wifiList.put(WIFI_SSID_TAG, wifiCfg.SSID);
                    String netStatus = "";
                    switch (wifiCfg.status) {
                        case WifiConfiguration.Status.CURRENT:
                            netStatus = "Connected";
                            break;
                        case WifiConfiguration.Status.DISABLED:
                            netStatus = "Disabled";
                            break;
                        case WifiConfiguration.Status.ENABLED:
                            netStatus = "Enabled";
                            break;
                    }
                    wifiList.put(" Status", netStatus);
                    wifiList.put(" Priority", String.valueOf(wifiCfg.priority));
                    if (null != wifiCfg.wepKeys) {
                        // info.addChild(" wepKeys", TextUtils.join(",", wifiCfg.wepKeys));
                    }
                    String protocols = "";
                    if (wifiCfg.allowedProtocols.get(WifiConfiguration.Protocol.RSN))
                        protocols = "RSN ";
                    if (wifiCfg.allowedProtocols.get(WifiConfiguration.Protocol.WPA))
                        protocols = protocols + "WPA ";
                    wifiList.put(" Protocols", protocols);

                    String keyProt = "";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE))
                        keyProt = "none";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP))
                        keyProt = "WPA+EAP ";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
                        keyProt = "WPA+PSK ";
                    wifiList.put(" Keys", keyProt);

                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                        // Remove network connections with no Password.
                        // wifiMgr.removeNetwork(wifiCfg.networkId);
                    }

                    String wifiCfgStr = wifiCfg.toString().replace("\n", " ");
                    // " cuid=" + creatorUid);
                    // " cname=" + creatorName);
                    String creator = wifiCfgStr.replaceAll(".* cname=([^ ]+) .*", "$1");
                    wifiList.put(" Creator", creator);
                }

            } catch (Exception ex) {
                ALog.e.tagMsg("loadWifi", niceString(ex));
            }
        }

        netInfoList = mainList;
        return netInfoList;
    }

    public static void release(BaseFragment baseFrag) {
        if (mNetBroadcastReceiver != null) {
            Context context = baseFrag.getContext();
            context.unregisterReceiver(mNetBroadcastReceiver);
            mNetBroadcastReceiver = null;
        }
    }

    public static String[] getNetworkInfo(Context context) {
        ArrayListEx<String> info = new ArrayListEx<>();

        if (phoneStateListener == null) {
            phoneStateListener = new SysInfoPhoneStateListener();
            TelephonyManager telephonyManager = getServiceSafe(context, Context.TELEPHONY_SERVICE);
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        }

        WifiManager wifiManager = getServiceSafe(context, Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        int rssi = wifiManager.getConnectionInfo().getRssi();
        info.add(String.format(Locale.getDefault(), "WiFi SigLvl=%d%%",
                getWifiLevelPercent(wifiManager, rssi)));

        ConnectivityManager cm = getServiceSafe(context, Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null) {
            info.add(String.format("NetType=%s", netInfo.getTypeName()));  // Wifi or mobile
            if (netInfo.isAvailable())
                info.add("NetAvail");
            if (netInfo.isConnected())
                info.add("NetConn");
        }

        if (phoneStateListener != null) {
            info.add("SigLvl=" + phoneStateListener.signalLevel);
            info.add("SigDbm=" + phoneStateListener.dBm);
        }

        return info.toArray(new String[0]);
    }

    static class SysInfoPhoneStateListener extends PhoneStateListener {

        public int signalLevel = -1;
        public int dBm = -1;

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            signalLevel = signalStrength.getGsmSignalStrength();
            dBm = (2 * signalLevel) - 113; // -> dBm
        }
    }

    // =============================================================================================
    public static class NetStatus {
        public final boolean airplaneMode;
        public final boolean netActive;  // Active means currently in high power for data transmission
        public final int netWifiLevelOutOf5;
        public final NetworkCapabilities netCapabilities;
        public final WifiInfo wifiInfo;
        public final NetworkInfo netInfo;

        public String netType = "Unknown";
        public NetworkInfo.DetailedState netState = null;
        public boolean netConn = false;
        public boolean netAvail = false;
        public final SupplicantState netWifi;    // COMPLETED,


        public NetStatus(Context context) {

            airplaneMode = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

            WifiManager wifiManager = getServiceSafe(context, Context.WIFI_SERVICE);
            wifiInfo = wifiManager.getConnectionInfo();
            netWifi = wifiInfo.getSupplicantState();
            int rssi = wifiManager.getConnectionInfo().getRssi();
            netWifiLevelOutOf5 = WifiManager.calculateSignalLevel(rssi, 5);

            ConnectivityManager cm = getServiceSafe(context, Context.CONNECTIVITY_SERVICE);
            netActive = cm.isDefaultNetworkActive();
            netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null) {
                netType = joinStrings("\n", netInfo.getTypeName(), netInfo.getSubtypeName(), netInfo.getExtraInfo());  // Wifi or mobile
                netAvail = netInfo.isAvailable();
                netConn = netInfo.isConnected();
                netState = netInfo.getDetailedState();
            }

            netCapabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        }
    }

    // =============================================================================================
    @SuppressWarnings("FieldCanBeLocal")
    public static class NetBroadcastReceiver extends BroadcastReceiver {
        private final WifiManager mWifiMgr;

        public NetBroadcastReceiver(WifiManager wifiMgr) {
            mWifiMgr = wifiMgr;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            /* background location permission required
               background exection not allowed ?

            listWifi = mWifiMgr.getScanResults();
            ALog.d.tagMsg(this, "Wifi size=" + listWifi.size());

             */
        }
    }
}
