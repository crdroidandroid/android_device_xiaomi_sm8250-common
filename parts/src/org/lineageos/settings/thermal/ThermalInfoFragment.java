/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lineageos.settings.R;

public class ThermalInfoFragment extends Fragment {
    private ThermalProfiles.Profile[] mProfiles;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.thermal_info_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int region = ThermalUtils.getSelectedRegion();
        mProfiles = ThermalProfiles.getProfiles(region);
        String regionName = getString(region == ThermalProfiles.REGION_INDIA
                ? R.string.thermal_region_india : R.string.thermal_region_global);
        TextView headerTitle = view.findViewById(R.id.thermal_info_header_title);
        TextView header = view.findViewById(R.id.thermal_info_header);
        headerTitle.setText(getString(R.string.thermal_info_header_title, regionName));
        header.setText(getString(R.string.thermal_info_header, mProfiles.length));

        RecyclerView list = view.findViewById(R.id.thermal_info_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setItemAnimator(null);
        list.setAdapter(new ProfileAdapter());
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.thermal_info_title);
    }

    private class ProfileAdapter extends RecyclerView.Adapter<ProfileViewHolder> {
        @Override public int getItemCount() { return mProfiles.length; }

        @NonNull
        @Override
        public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ProfileViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.thermal_info_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
            ThermalProfiles.Profile profile = mProfiles[position];
            holder.icon.setImageResource(profile.iconRes);
            holder.title.setText(profile.titleRes);
            holder.summary.setText(profile.summaryRes);
            holder.meta.setText(getString(R.string.thermal_info_meta,
                    profile.sconfig, profile.policy.compactConfigName()));
            holder.itemView.setOnClickListener(v -> {
                ThermalProfileDetailFragment fragment =
                        ThermalProfileDetailFragment.newInstance(profile.storageState);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                                fragment, "thermal_profile_detail")
                        .addToBackStack(null)
                        .commit();
            });
        }
    }

    private static class ProfileViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView summary;
        final TextView meta;

        ProfileViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.profile_info_icon);
            title = view.findViewById(R.id.profile_info_title);
            summary = view.findViewById(R.id.profile_info_summary);
            meta = view.findViewById(R.id.profile_info_meta);
        }
    }
}
