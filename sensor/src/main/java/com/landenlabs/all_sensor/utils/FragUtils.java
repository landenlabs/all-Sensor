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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.landenlabs.all_sensor.MainActivity;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.ui.view.BaseFragment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragUtils {

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> T getServiceSafe(@NonNull Context context, @NonNull String service) {
        //noinspection unchecked
        return (T) Objects.requireNonNull(context.getSystemService(service));
    }

    public static Fragment getVisibleFragment(MainActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    public static String getCurrentFragmentName(MainActivity activity) {
        Fragment fragment = getVisibleFragment(activity);
        if (fragment instanceof NavHostFragment) {
            return ((NavHostFragment) fragment).getNavController().getCurrentDestination().getLabel().toString();
        }
        return (fragment instanceof BaseFragment) ? ((BaseFragment) fragment).getName() : "Unknown";
    }

    public static void addOrExecuteShortcut(@NonNull Activity activity, @NonNull ViewGroup bottomNavGroup, String intentAction) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            return;
        }

        Map<String, MenuItem> menus;
        Menu menu;

        if (bottomNavGroup instanceof BottomNavigationView) {
            BottomNavigationView bottomNavigationView = (BottomNavigationView) bottomNavGroup;
            menu = bottomNavigationView.getMenu();
        } else {
            PopupMenu popupMenu = new PopupMenu(activity, bottomNavGroup.getRootView());
            activity.getMenuInflater().inflate(R.menu.main_drawer, popupMenu.getMenu());
            menu = popupMenu.getMenu();
        }

        int menuSize = menu.size();
        menus = new HashMap<>(menuSize);
        for (int menuIdx = 0; menuIdx < menuSize; menuIdx++) {
            MenuItem menuItem = menu.getItem(menuIdx);
            menus.put(menuItem.getTitle().toString(), menuItem);
        }

        ShortcutManager shortcutManager = getServiceSafe(activity, Context.SHORTCUT_SERVICE);
        NavController navBotController = Navigation.findNavController(activity, R.id.nav_host_fragment);
        Iterator<NavDestination> navIT = navBotController.getGraph().iterator();
        List<ShortcutInfo> shortcutList = new ArrayListEx<>();
        int maxShortcuts = shortcutManager.getMaxShortcutCountPerActivity();
        while (navIT.hasNext() && shortcutList.size() + 1 < maxShortcuts) {
            NavDestination navDestination = navIT.next();
            if (navDestination != null && navDestination.getLabel() != null) {
                MenuItem menuItem = menus.get(navDestination.getLabel());
                if (menuItem != null) {
                    Intent newTaskIntent = new Intent(activity, MainActivity.class);
                    newTaskIntent.setAction(navDestination.getLabel().toString());
                    newTaskIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    String iconName = "ic_menu_" + menuItem.getTitle().toString().toLowerCase();
                    int iconRes =
                            activity.getResources().getIdentifier(iconName, "drawable", activity.getPackageName());
                    if (iconRes <= 0) {
                        iconRes = R.drawable.ic_menu_start;
                    }
                    ShortcutInfo postShortcut
                            = new ShortcutInfo.Builder(activity, navDestination.getLabel().toString())
                            .setShortLabel(navDestination.getLabel())
                            .setLongLabel(navDestination.getLabel())
                            .setIcon(Icon.createWithResource(activity, iconRes))
                            .setIntent(newTaskIntent)
                            .build();
                    shortcutList.add(postShortcut);

                    if (navDestination.getLabel().equals(intentAction)) {
                        // Execute shortcut
                        navBotController.navigate(navDestination.getId());
                    }
                }
            }
        }

        shortcutManager.addDynamicShortcuts(shortcutList);
    }

    public static void setText(TextView tv, @StringRes int textRes, @StringRes int linkRes, String ... args) {
        String text = tv.getContext().getString(textRes, args);
        String link = tv.getContext().getString(linkRes);
        SpannableString span = new SpannableString(text);
        int pos = text.indexOf(link);
        if (pos < 0)
            throw new IllegalArgumentException("Text does not contain Link " + text + " " + link);
        span.setSpan(new ForegroundColorSpan(Color.BLUE), pos, link.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new UnderlineSpan(), pos, link.length(), 0);
        tv.setText(span);
    }

    public static void setText(TextView tv, @StringRes int textRes, Pattern regExp) {
        String text = tv.getContext().getString(textRes);
        SpannableString span = new SpannableString(text);

        Matcher matcher = regExp.matcher(text);
        if (matcher.find()) {
            int linkStart = matcher.start();
            if (linkStart > 0) {
                span.setSpan(new ForegroundColorSpan(Color.BLUE), 0, linkStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new UnderlineSpan(), 0, linkStart, 0);
            }
        }
        tv.setText(span);
    }
}
