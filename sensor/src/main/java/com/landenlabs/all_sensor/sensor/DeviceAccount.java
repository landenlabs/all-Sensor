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

package com.landenlabs.all_sensor.sensor;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.BuildConfig;
import com.landenlabs.all_sensor.utils.ArrayListEx;

/**
 * External device account credentials.
 */
public class DeviceAccount {
    public final String user;
    public final String pwd;

    public DeviceAccount(String user, String pwd) {
        this.user = user;
        this.pwd = pwd;
    }

    private static final String SEP_ACCOUNTS = ";";
    private static final String SEP_USER_PWD = ",";
    private static final String PREF_ACCOUNTS = "accounts";

    // ---------------------------------------------------------------------------------------------

    @NonNull
    @Override
    public String toString() {
        return user + "  " + pwd;
    }

    public static ArrayListEx<DeviceAccount> getAccounts(Context context) {
        ArrayListEx<DeviceAccount> accountList = new ArrayListEx<>();
        SharedPreferences pref = context.getSharedPreferences("WxManager", Context.MODE_PRIVATE);
        String[] account = pref.getString(PREF_ACCOUNTS, "").split(SEP_ACCOUNTS);
        for (String user_pwd : account) {
            String[] parts = user_pwd.split(SEP_USER_PWD);
            if (parts.length == 2) {
                accountList.add(new DeviceAccount(parts[0], parts[1]));
            }
        }

        // DEBUG - init list
        if (accountList.isEmpty()) {
            accountList.add(new DeviceAccount(BuildConfig.GoveeUser1, BuildConfig.GoveePwd1));
        }

        return accountList;
    }

    public static void saveAccounts(Context context, ArrayListEx<DeviceAccount> accounts) {
        SharedPreferences pref = context.getSharedPreferences("WxManager", Context.MODE_PRIVATE);

        StringBuilder sb = new StringBuilder();
        for (DeviceAccount account : accounts) {
            if (sb.length() != 0)
                sb.append(SEP_ACCOUNTS);
            sb.append(account.user).append(SEP_USER_PWD).append(account.pwd);
        }
        pref.edit().putString(PREF_ACCOUNTS, sb.toString()).apply();
    }
}