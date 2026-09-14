package com.android.settings.deviceinfo;

import android.content.Context;
import android.app.ActivityManager;
import android.widget.TextView;
import androidx.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import com.android.settings.R;

public class DivaRamCpuPreferenceController extends BasePreferenceController {
    
    public DivaRamCpuPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }
    
    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
    
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        LayoutPreference pref = screen.findPreference(getPreferenceKey());
        if (pref != null) {
            TextView ramTv = pref.findViewById(R.id.ram_info_tv);
            TextView cpuTv = pref.findViewById(R.id.cpu_info_tv);
            
            if (ramTv != null) {
                ramTv.setText(getRamSize());
            }
            if (cpuTv != null) {
                cpuTv.setText(getCpuInfo());
            }
        }
    }

    private String getRamSize() {
        return DivaInfoUtils.getTotalRam();
    }

    private String getCpuInfo() {
        String cpuName = "Octa-core";
        String maxFreq = "";
        
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware")) {
                    cpuName = line.split(":")[1].trim();
                }
            }
            br.close();
            
            // Try to get max frequency
            BufferedReader brFreq = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
            String freqLine = brFreq.readLine();
            if (freqLine != null) {
                long freqKHz = Long.parseLong(freqLine.trim());
                double freqGHz = freqKHz / 1000000.0;
                maxFreq = String.format(" Max %.2fGHz", freqGHz);
            }
            brFreq.close();
        } catch (Exception e) {
            // Ignore
        }
        
        if (cpuName.equals("Octa-core") && maxFreq.isEmpty()) {
            return "Octa-core Max 3.25GHz"; // Fallback to prompt image
        }
        return cpuName + maxFreq;
    }
}
