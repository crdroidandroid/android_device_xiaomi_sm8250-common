/*
 * Copyright (C) 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.lineageos.settings.R;

public class ThermalActivity extends CollapsingToolbarBaseActivity {
    private static final String TAG_THERMAL = "thermal";
    private static final int MENU_INFO = 1;
    private static final int MENU_SEARCH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(
                    com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                    new ThermalSettingsFragment(), TAG_THERMAL).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Keep information immediately to the left of search.
        MenuItem info = menu.add(Menu.NONE, MENU_INFO, 1, R.string.thermal_info_title);
        info.setIcon(R.drawable.ic_info);
        info.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem searchItem =
                menu.add(Menu.NONE, MENU_SEARCH, 2, R.string.thermal_search_apps);
        searchItem.setIcon(R.drawable.ic_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS
                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        SearchView searchView = new SearchView(this);
        searchView.setIconifiedByDefault(true);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint(getString(R.string.thermal_search_apps));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateSearchQuery(newText);
                return true;
            }
        });
        searchItem.setActionView(searchView);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.setQuery("", false);
                updateSearchQuery("");
                return true;
            }
        });
        return true;
    }

    private void updateSearchQuery(String query) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_THERMAL);
        if (fragment instanceof ThermalSettingsFragment) {
            ((ThermalSettingsFragment) fragment).setSearchQuery(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_INFO) {
            startActivity(new Intent(this, ThermalInfoActivity.class));
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
