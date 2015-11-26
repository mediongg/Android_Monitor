package com.example.medionchou.tobacco.SubFragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.medionchou.tobacco.Constants.Command;
import com.example.medionchou.tobacco.Constants.Config;
import com.example.medionchou.tobacco.DataContainer.Schedule;
import com.example.medionchou.tobacco.LocalService;
import com.example.medionchou.tobacco.LocalServiceConnection;
import com.example.medionchou.tobacco.R;
import com.example.medionchou.tobacco.ServiceListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Medion on 2015/11/4.
 */
public class ScheduleFragment extends Fragment {

    private LocalService mService;
    private ScheduleAsync schedulAsync;
    private TableLayout parentLayout;



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ServiceListener mCallBack;
        LocalServiceConnection mConnection;
        mCallBack = (ServiceListener) activity;
        mConnection = mCallBack.getLocalServiceConnection();
        mService = mConnection.getService();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_schedule_layout, container, false);
        parentLayout = (TableLayout) rootView.findViewById(R.id.parent_layout);

        parentLayout.setStretchAllColumns(true);
        schedulAsync = new ScheduleAsync();
        schedulAsync.execute((Void) null);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!schedulAsync.isCancelled())
            schedulAsync.cancel(true);
    }

    private class ScheduleAsync extends AsyncTask<Void, String, Void> {
        ProgressDialog progressDialog;
        List<Schedule> scheduleList = new ArrayList<>();

        int lineId[] = {R.id.line1, R.id.line2, R.id.line3, R.id.line4, R.id.line5, R.id.line6, R.id.line7, R.id.line8, R.id.line9};
        int cm_staff[] = {R.id.cm_staff1, R.id.cm_staff2, R.id.cm_staff3, R.id.cm_staff4, R.id.cm_staff5, R.id.cm_staff6, R.id.cm_staff7, R.id.cm_staff8, R.id.cm_staff9};
        int pm_staff[] = {R.id.pm_staff1, R.id.pm_staff2, R.id.pm_staff3, R.id.pm_staff4, R.id.pm_staff5, R.id.pm_staff6, R.id.pm_staff7, R.id.pm_staff8, R.id.pm_staff9};
        int office_id[] = {R.id.filter, R.id.boxing, R.id.repair, R.id.office};
        int office_layout[] = {R.id.fp_layout, R.id.boxing_layout, R.id.repair_layout, R.id.office_layout};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(getString(R.string.progress_dialog_waiting));
            progressDialog.setMessage(getString(R.string.getting_query_result));
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String reply = "";

            try{

                sendCommand(Command.SCHEDULE);
                publishProgress("Update");

                while (!isCancelled()) {
                    reply = mService.getUpdateMsg();

                    if (reply.length() > 0) {
                        mService.resetUpdateMsg();
                        String[] updateMsg = reply.split("<END>");
                        boolean isUpdate = false;

                        for (String tmp : updateMsg) {
                            if (tmp.contains("SCHEDULE")) {
                                isUpdate = true;
                            }
                        }

                        if (isUpdate) {
                            scheduleList.clear();
                            sendCommand(Command.SCHEDULE);
                            publishProgress("Update");
                        }
                    }
                    Thread.sleep(2000);
                }

            } catch (InterruptedException e) {
                Log.e("MyLog", e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (progressDialog.isShowing())
                progressDialog.cancel();

            if (values[0].equals("Update")) {
                setLayout();
            }
        }

        private void setLayout() {

            for (int i = 0; i < scheduleList.size(); i++) {
                Schedule schedule = scheduleList.get(i);

                if (i < 9) {
                    TextView line = (TextView) parentLayout.findViewById(lineId[i]);
                    TextView cm = (TextView) parentLayout.findViewById(cm_staff[i]);
                    TextView pm = (TextView) parentLayout.findViewById(pm_staff[i]);

                    line.setText(schedule.getOffice());
                    cm.setText(schedule.getCMStaff());
                    pm.setText(schedule.getPMStaff());

                    line.setTextSize(Config.TEXT_SIZE);
                    cm.setTextSize(Config.TEXT_SIZE);
                    pm.setTextSize(Config.TEXT_SIZE);

                    cm.setBackgroundColor(schedule.getColor(schedule.getCMOn()));
                    pm.setBackgroundColor(schedule.getColor(schedule.getPMOn()));

                } else {
                    LinearLayout layout = (LinearLayout)parentLayout.findViewById(office_layout[i-9]);
                    TextView staff = (TextView) parentLayout.findViewById(office_id[i-9]);

                    staff.setText(schedule.getStaff());

                    staff.setTextSize(24);

                    staff.setMaxEms(5);
                    staff.setMaxLines(3);

                    layout.setBackgroundColor(schedule.getColor(schedule.getOfficeOn()));

                }

            }
        }


        private void sendCommand(String cmd) {
            String msg = "";
            try {
                while (msg.length() == 0) {
                    mService.setCmd(cmd);
                    Thread.sleep(1000);
                    msg = mService.getQueryReply();
                }
                switch (cmd) {
                    case Command.SCHEDULE:
                        parseSchedule(msg);
                        break;
                }
                mService.resetQueryReply();
            } catch (InterruptedException e) {
                Log.e("MyLog", e.toString() + "SendCommand thread interrupted");
            }
        }


        private void parseSchedule(String rawData) {
            String[] officeInfo = rawData.split("<N>|<END>");

            for (int i = 0; i < officeInfo.length; i++) {
                Log.v("MyLog", officeInfo[i]);
                String[] detail = officeInfo[i].split("\\t", -1);

                if (i < 9) {

                    Schedule schedule = new Schedule(detail[1].equals("1"), detail[2].equals("1"), detail[3], detail[4], detail[5]);

                    scheduleList.add(schedule);
                } else {
                    Schedule schedule = new Schedule(detail[1].equals("1"), detail[2], detail[3]);

                    scheduleList.add(schedule);
                }
            }

            TableLayout test = (TableLayout) parentLayout.findViewById(R.id.lower_layout);
            test.setStretchAllColumns(true);
        }


    }

}
