package com.muzima.adapters.setupconfiguration;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.cohort.CohortsAdapter;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.Fonts;
import com.muzima.view.CheckedLinearLayout;

import java.util.ArrayList;
import java.util.List;

public class SetupConfigurationAdapter extends ListAdapter<SetupConfiguration> {
    private static final String TAG = SetupConfigurationAdapter.class.getSimpleName();
    private SetupConfigurationController setupConfigurationController;
    private final MuzimaSyncService muzimaSyncService;
    private String selectedConfigurationUuid;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private List<SetupConfiguration> allSetupConfigurations;

    public SetupConfigurationAdapter(Context context, int textViewResourceId, SetupConfigurationController setupConfigurationController){
        super(context, textViewResourceId);
        this.setupConfigurationController = setupConfigurationController;
        muzimaSyncService = ((MuzimaApplication) (getContext().getApplicationContext())).getMuzimaSyncService();
    }

    public String getSelectedConfigurationUuid(){
        return selectedConfigurationUuid;
    }

    @Override
    public void reloadData() {
        new DownloadSetupConfigurationsBackgroundQueryTask().execute();
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void onListItemClick(int position) {
        SetupConfiguration setupConfiguration = getItem(position);
        selectedConfigurationUuid = setupConfiguration.getUuid();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_setup_configs_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setTextToName(getItem(position).getName());
        holder.setTextToDescription(getItem(position).getDescription());
        return convertView;
    }
    public class ViewHolder {
        private CheckedTextView name;
        private CheckedTextView description;

        public ViewHolder(View convertView) {
            this.name = (CheckedTextView) convertView
                    .findViewById(R.id.config_name);
            this.description = (CheckedTextView) convertView
                    .findViewById(R.id.config_description);
        }

        public void setTextToName(String text) {
            name.setText(text);
            name.setTypeface(Fonts.roboto_medium(getContext()));
        }

        public void setTextToDescription(String description) {
            this.description.setText(description);
            this.description.setTypeface(Fonts.roboto_medium(getContext()));
        }
    }

    public void filterItems(String filterText) {
        SetupConfigurationAdapter.this.clear();
        List <SetupConfiguration> filteredSetupConfigurations = new ArrayList<>();
        for (SetupConfiguration setupConfiguration : allSetupConfigurations) {
            if (setupConfiguration.getName().toLowerCase().contains(filterText.toLowerCase())
                    || setupConfiguration.getDescription().toLowerCase().contains(filterText.toLowerCase())) {
                filteredSetupConfigurations.add(setupConfiguration);
            }
        }
        addAll(filteredSetupConfigurations);
        notifyDataSetChanged();
    }

    public class DownloadSetupConfigurationsBackgroundQueryTask extends AsyncTask<Void, Void, List<SetupConfiguration>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<SetupConfiguration> doInBackground(Void... voids) {
            List<SetupConfiguration> setupConfigurations = null;
            try {
                muzimaSyncService.downloadSetupConfigurations();
                setupConfigurations = setupConfigurationController.getAllSetupConfigurations();
                Log.i(TAG, "#SetupConfigurations: " + setupConfigurations.size());
            } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
                Log.w(TAG, "Exception occurred while fetching the downloaded Setup Configurations", e);
            }
            return setupConfigurations;
        }

        @Override
        protected void onPostExecute(List<SetupConfiguration> setupConfigurations){
            if(setupConfigurations == null){
                Toast.makeText(getContext(), R.string.error_setup_configuration_download, Toast.LENGTH_SHORT).show();
                return;
            }
            allSetupConfigurations = setupConfigurations;
            SetupConfigurationAdapter.this.clear();
            addAll(setupConfigurations);
            notifyDataSetChanged();

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }
    }
}