/***********************************************************************************************************************
 *
 * This file is part of the eeg-database-for-android project

 * ==========================================
 *
 * Copyright (C) 2013 by University of West Bohemia (http://www.zcu.cz/en/)
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
 *
 * Petr Ježek, Petr Miko
 *
 **********************************************************************************************************************/
package cz.zcu.kiv.eeg.mobile.base.ui.reservation;

import android.app.*;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import cz.zcu.kiv.eeg.mobile.base.R;
import cz.zcu.kiv.eeg.mobile.base.archetypes.CommonActivity;
import cz.zcu.kiv.eeg.mobile.base.data.Values;
import cz.zcu.kiv.eeg.mobile.base.data.adapter.ReservationAdapter;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.Reservation;
import cz.zcu.kiv.eeg.mobile.base.data.container.xml.TimeContainer;
import cz.zcu.kiv.eeg.mobile.base.utils.ConnectionUtils;
import cz.zcu.kiv.eeg.mobile.base.ws.asynctask.FetchReservationsToDate;

import java.util.ArrayList;

/**
 * Reservation fragment for displaying reservation created on eeg base.
 *
 * @author Petr Miko
 */
public class ReservationFragment extends ListFragment implements OnClickListener {

    public final static String TAG = ReservationFragment.class.getSimpleName();
    private final static int HEADER_ROW = 1;
    private static TimeContainer timeContainer;
    private static ReservationAdapter dataAdapter = null;
    private final OnDateSetListener dateSetListener = new OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            timeContainer.setYear(year);
            // +1 is correction due to "bug" in java Date implementation (months starts from 0, not 1)
            timeContainer.setMonth(monthOfYear + 1);
            timeContainer.setDay(dayOfMonth);
            updateData();

            //update date
            dateLabel.setText(timeContainer.toDateString());
        }
    };
    private boolean isDualView;
    private int cursorPosition;
    private View header = null;
    private TextView dateLabel;

    /**
     * Sets, that fragment has own menu items and initializes time to be set.
     *
     * @param savedInstanceState previous instance bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setTitle(R.string.app_reser);
        actionBar.setIcon(R.drawable.ic_action_time);

        if (savedInstanceState != null) {
            cursorPosition = savedInstanceState.getInt("cursorPos", 0);
            timeContainer = savedInstanceState.getParcelable("time");
        } else if (timeContainer == null) {
            Time time = new Time();
            time.setToNow();
            timeContainer = new TimeContainer();
            timeContainer.setDay(time.monthDay);
            // +1 is correction due to "bug" in java Date implementation (months starts from 0, not 1)
            timeContainer.setMonth(time.month + 1);
            timeContainer.setYear(time.year);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reser_agenda, container, false);
        header = inflater.inflate(R.layout.reser_agenda_list_row_header, null);
        View detailsFrame = view.findViewById(R.id.details);
        isDualView = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        initView(view);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setListAdapter(null);
        setListAdapter(getAdapter());

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.addHeaderView(header);
        if (isDualView) {
            listView.setSelector(R.drawable.list_selector);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            showDetails(cursorPosition);
            setSelection(cursorPosition);
        }

        listView.setTextFilterEnabled(true);
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Sets date label text field and sets onClick listener on choose date button.
     *
     * @param view view to be displayed
     */
    private void initView(View view) {
        //obtaining view elements
        dateLabel = (TextView) view.findViewById(R.id.dateLabel);
        Button chooseDateButton = (Button) view.findViewById(R.id.chooseDateButton);


        // setting onclick listener
        chooseDateButton.setOnClickListener(this);

        //setting date
        dateLabel.setText(timeContainer.toDateString());

        //setting refresh capability to empty list view
        View emptyView = view.findViewById(android.R.id.empty);
        emptyView.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);
        //due to refreshing view on update, activity must be set on creation - prevention from losing activity reference on activity recreation
        dataAdapter.setContext((CommonActivity) getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Saving date...");
        super.onSaveInstanceState(outState);
        outState.putParcelable("time", timeContainer);
        outState.putInt("cursorPos", cursorPosition);
    }

    /**
     * If online fetches reservations up to set date.
     * Un-selects selected item in process.
     */
    public void updateData() {
        CommonActivity activity = (CommonActivity) getActivity();
        if (ConnectionUtils.isOnline(activity)) {
            //un-selects row
            getListView().clearChoices();

            new FetchReservationsToDate(activity, getAdapter()).execute(timeContainer);
        } else
            activity.showAlert(activity.getString(R.string.error_offline));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.reser_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addReservation:
                Log.d(TAG, "Add new booking time chosen");
                Intent intent = new Intent(getActivity(), AddRecordActivity.class);
                Bundle b = new Bundle();
                b.putParcelable("time", timeContainer);
                intent.putExtras(b);
                startActivityForResult(intent, Values.ADD_RESERVATION_FLAG);
                break;
            case R.id.refresh:
                updateData();
                Log.d(TAG, "Refresh data button pressed");
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * On choose date button click event show date picker dialog.
     *
     * @param v date picker button
     */
    public void chooseDateClick(View v) {
        Log.d(TAG, "Add new booking time chosen");

        // -1 is due to our previous "fix" of Date format bug
        DatePickerDialog datePicker = new DatePickerDialog(getActivity(), dateSetListener, timeContainer.getYear(), timeContainer.getMonth() - 1, timeContainer.getDay());
        datePicker.getDatePicker().getCalendarView().setFirstDayOfWeek(Values.firstDayOfWeek);
        datePicker.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (Values.ADD_RESERVATION_FLAG): {
                if (resultCode == Activity.RESULT_OK) {
                    Reservation record = (Reservation) data.getExtras().get(Values.ADD_RESERVATION_KEY);
                    ((ReservationAdapter) getListAdapter()).add(record);
                }
                break;
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        showDetails(pos);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chooseDateButton:
                chooseDateClick(v);
                break;
            case android.R.id.empty:
                updateData();
                break;
        }
    }

    /**
     * Method to show the details of a selected item.
     * Details are displayed either in-place in the current UI fragment, or new ReservationDetailsActivity is created.
     *
     * @param index index of selected item in list
     */
    void showDetails(int index) {
        cursorPosition = index;
        boolean empty = dataAdapter == null || dataAdapter.isEmpty();

        ReservationAdapter dataAdapter = getAdapter();
        if (isDualView) {
            getListView().setItemChecked(index, true);

            ReservationDetailsFragment details = new ReservationDetailsFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (getFragmentManager().findFragmentByTag(ReservationDetailsFragment.TAG) == null) {
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }

            Bundle args = new Bundle();
            args.putInt("index", index);
            args.putParcelable("data", empty || index <= 0 ? null : dataAdapter.getItem(index - HEADER_ROW));
            details.setArguments(args);

            ft.replace(R.id.details, details, ReservationDetailsFragment.TAG);
            ft.commit();

        } else if (!empty) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), ReservationDetailsActivity.class);
            intent.putExtra("index", index);
            intent.putExtra("data", dataAdapter.getItem(index - HEADER_ROW));
            startActivity(intent);
        }
    }

    /**
     * Getter of reservation adapter.
     * Created on first access.
     *
     * @return reservation adapter
     */
    private ReservationAdapter getAdapter() {
        if (dataAdapter == null)
            dataAdapter = new ReservationAdapter((CommonActivity) getActivity(), getId(), R.layout.reser_agenda_list_row, new ArrayList<Reservation>());

        return dataAdapter;
    }

}
